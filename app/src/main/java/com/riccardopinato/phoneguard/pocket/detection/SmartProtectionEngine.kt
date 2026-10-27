package com.riccardopinato.phoneguard.pocket.detection

import com.riccardopinato.phoneguard.pocket.model.*
import kotlin.math.roundToInt

class SmartProtectionEngine(
    private var settings: GuardSettings,
    private var profile: CalibrationProfile,
    initialState: GuardState = GuardState.WAITING_FOR_POCKET
) {
    private var state = initialState
    private var pocketCandidateSince: Long? = null
    private var removalCandidateSince: Long? = null
    private var lastLux: Float? = null
    private var protectedLux: Float? = null

    fun currentState(): GuardState = state

    fun updateSettings(value: GuardSettings) { settings = value }
    fun updateProfile(value: CalibrationProfile) { profile = value }

    fun forceState(value: GuardState) {
        state = value
        pocketCandidateSince = null
        removalCandidateSince = null
    }

    fun process(snapshot: SensorSnapshot): DetectionDecision {
        val previousLux = lastLux
        lastLux = snapshot.lux

        return when (state) {
            GuardState.DISARMED,
            GuardState.ARMING,
            GuardState.CALIBRATING,
            GuardState.OWNER_CHECK,
            GuardState.ALARM ->
                unchanged(0, "State managed outside detection engine")

            GuardState.WAITING_FOR_POCKET -> handleWaiting(snapshot)
            GuardState.POCKET_CONFIRMED -> handleProtected(snapshot, previousLux)
            GuardState.REMOVAL_SUSPECTED -> handleRemoval(snapshot, previousLux)
        }
    }

    fun ownerVerified(): DetectionDecision {
        protectedLux = null
        return transition(
            GuardState.WAITING_FOR_POCKET,
            0,
            "Owner verified; waiting for automatic re-arm"
        )
    }

    fun ownerCheckExpired(): DetectionDecision =
        transition(GuardState.ALARM, 100, "Owner check expired")

    private fun handleWaiting(snapshot: SensorSnapshot): DetectionDecision {
        if (!isStored(snapshot)) {
            pocketCandidateSince = null
            return unchanged(0, "Waiting for stable stored condition")
        }

        val startedAt = pocketCandidateSince
            ?: snapshot.timestampMs.also { pocketCandidateSince = it }

        val stableFor = snapshot.timestampMs - startedAt
        if (stableFor < settings.pocketStableMs) {
            return unchanged(
                0,
                "Storage candidate ${stableFor}/${settings.pocketStableMs}ms"
            )
        }

        pocketCandidateSince = null
        protectedLux = snapshot.lux
        return transition(
            GuardState.POCKET_CONFIRMED,
            0,
            "Stable storage condition confirmed"
        )
    }

    private fun handleProtected(
        snapshot: SensorSnapshot,
        previousLux: Float?
    ): DetectionDecision {
        val score = calculateExtractionScore(snapshot, previousLux)

        if (score < settings.sensitivity.suspectScore) {
            return unchanged(score, "Extraction confidence below suspect threshold")
        }

        removalCandidateSince = snapshot.timestampMs
        return transition(
            GuardState.REMOVAL_SUSPECTED,
            score,
            "Possible extraction detected"
        )
    }

    private fun handleRemoval(
        snapshot: SensorSnapshot,
        previousLux: Float?
    ): DetectionDecision {
        if (isStored(snapshot)) {
            removalCandidateSince = null
            return transition(
                GuardState.POCKET_CONFIRMED,
                0,
                "Device returned to protected position"
            )
        }

        val score = calculateExtractionScore(snapshot, previousLux)
        val startedAt = removalCandidateSince
            ?: snapshot.timestampMs.also { removalCandidateSince = it }
        val elapsed = snapshot.timestampMs - startedAt

        if (
            (
                score >= settings.sensitivity.ownerCheckScore ||
                    isDecisivePocketExtraction(snapshot)
            ) &&
            elapsed <= settings.removalWindowMs
        ) {
            removalCandidateSince = null
            return transition(
                GuardState.OWNER_CHECK,
                score,
                "Extraction confirmed; starting owner check"
            )
        }

        if (elapsed > settings.removalWindowMs) {
            removalCandidateSince = null
            return transition(
                if (isStored(snapshot)) GuardState.POCKET_CONFIRMED
                else GuardState.WAITING_FOR_POCKET,
                score,
                "Removal confirmation window expired"
            )
        }

        return unchanged(score, "Accumulating extraction evidence")
    }

    private fun isDecisivePocketExtraction(
        snapshot: SensorSnapshot
    ): Boolean =
        settings.mode == GuardMode.POCKET &&
            snapshot.proximityNear == false &&
            snapshot.lux?.let { it >= profile.exitLux } == true

    private fun isStored(snapshot: SensorSnapshot): Boolean {
        val darkEnough = snapshot.lux?.let { it <= profile.enterLux } ?: true

        return when (settings.mode) {
            GuardMode.POCKET ->
                snapshot.proximityNear == true && darkEnough

            GuardMode.BAG -> {
                val lowMotion =
                    snapshot.motionScore < effectiveMotionThreshold() * 0.85f
                darkEnough && lowMotion
            }
        }
    }

    private fun calculateExtractionScore(
        snapshot: SensorSnapshot,
        previousLux: Float?
    ): Int {
        var score = 0f

        when (settings.mode) {
            GuardMode.POCKET -> if (snapshot.proximityNear == false) score += 30f
            GuardMode.BAG -> if (snapshot.proximityNear == false) score += 8f
        }

        snapshot.lux?.let { currentLux ->
            if (currentLux >= profile.exitLux) score += 22f

            val baseLux = protectedLux ?: profile.enterLux
            if ((currentLux - baseLux).coerceAtLeast(0f) >= 8f) score += 8f

            if (previousLux != null && currentLux - previousLux >= 6f) {
                score += 7f
            }
        }

        val motionThreshold = effectiveMotionThreshold()
        val motionRatio =
            if (motionThreshold <= 0f) 0f else snapshot.motionScore / motionThreshold
        score += (motionRatio.coerceIn(0f, 1.5f) / 1.5f) * 23f

        val rotationThreshold = profile.rotationThreshold
        val rotationRatio =
            if (rotationThreshold <= 0f) 0f else snapshot.rotationScore / rotationThreshold
        score += (rotationRatio.coerceIn(0f, 1.5f) / 1.5f) * 10f

        if (
            settings.mode == GuardMode.BAG &&
            snapshot.motionScore >= motionThreshold &&
            snapshot.rotationScore >= rotationThreshold
        ) {
            score += 10f
        }

        return score.coerceIn(0f, 100f).roundToInt()
    }

    private fun effectiveMotionThreshold(): Float =
        (profile.motionThreshold * settings.sensitivity.motionMultiplier)
            .coerceIn(0.08f, 0.65f)

    private fun transition(
        newState: GuardState,
        score: Int,
        reason: String
    ): DetectionDecision {
        val changed = newState != state
        state = newState
        return DetectionDecision(state, changed, score, reason)
    }

    private fun unchanged(score: Int, reason: String) =
        DetectionDecision(state, false, score, reason)
}
