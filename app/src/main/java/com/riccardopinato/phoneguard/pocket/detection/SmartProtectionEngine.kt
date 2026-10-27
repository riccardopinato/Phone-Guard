package com.riccardopinato.phoneguard.pocket.detection

import com.riccardopinato.phoneguard.pocket.model.CalibrationProfile
import com.riccardopinato.phoneguard.pocket.model.DetectionDecision
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.GuardSettings
import com.riccardopinato.phoneguard.pocket.model.GuardState
import com.riccardopinato.phoneguard.pocket.model.SensorSnapshot
import kotlin.math.roundToInt

class SmartProtectionEngine(
    private var settings: GuardSettings,
    private var profile: CalibrationProfile,
    initialState: GuardState =
        GuardState.WAITING_FOR_POCKET
) {
    private var state = initialState
    private var pocketCandidateSince: Long? = null
    private var removalCandidateSince: Long? = null
    private var lastLux: Float? = null
    private var protectedLux: Float? = null

    fun currentState(): GuardState =
        state

    fun updateSettings(value: GuardSettings) {
        settings = value
    }

    fun updateProfile(value: CalibrationProfile) {
        profile = value
    }

    fun forceState(value: GuardState) {
        state = value
        pocketCandidateSince = null
        removalCandidateSince = null
    }

    fun process(
        snapshot: SensorSnapshot
    ): DetectionDecision {
        val previousLux = lastLux
        lastLux = snapshot.lux

        return when (state) {
            GuardState.DISARMED,
            GuardState.ARMING,
            GuardState.CALIBRATING,
            GuardState.OWNER_CHECK,
            GuardState.ALARM ->
                unchanged(
                    0,
                    "State managed outside detection engine"
                )

            GuardState.WAITING_FOR_POCKET ->
                handleWaiting(snapshot)

            GuardState.POCKET_CONFIRMED ->
                handleProtected(
                    snapshot,
                    previousLux
                )

            GuardState.REMOVAL_SUSPECTED ->
                handleRemoval(
                    snapshot,
                    previousLux
                )
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

    fun ownerCheckExpired():
        DetectionDecision =
        transition(
            GuardState.ALARM,
            100,
            "Owner check expired"
        )

    private fun handleWaiting(
        snapshot: SensorSnapshot
    ): DetectionDecision {
        if (!isStored(snapshot)) {
            pocketCandidateSince = null

            return unchanged(
                0,
                "Waiting for stable stored condition"
            )
        }

        val startedAt =
            pocketCandidateSince
                ?: snapshot.timestampMs
                    .also {
                        pocketCandidateSince = it
                    }

        val stableFor =
            snapshot.timestampMs -
                startedAt

        if (
            stableFor <
            settings.pocketStableMs
        ) {
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
        val score =
            calculateExtractionScore(
                snapshot,
                previousLux
            )

        if (
            score <
            settings.sensitivity.suspectScore
        ) {
            return unchanged(
                score,
                "Extraction confidence below suspect threshold"
            )
        }

        removalCandidateSince =
            snapshot.timestampMs

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

        val score =
            calculateExtractionScore(
                snapshot,
                previousLux
            )

        val startedAt =
            removalCandidateSince
                ?: snapshot.timestampMs
                    .also {
                        removalCandidateSince = it
                    }

        val elapsed =
            snapshot.timestampMs -
                startedAt

        if (
            (
                score >=
                    settings
                        .sensitivity
                        .ownerCheckScore ||
                    isDecisivePocketExtraction(
                        snapshot
                    )
                ) &&
            elapsed <=
                settings.removalWindowMs
        ) {
            removalCandidateSince = null

            return transition(
                GuardState.OWNER_CHECK,
                score,
                "Extraction confirmed; starting owner check"
            )
        }

        if (
            elapsed >
            settings.removalWindowMs
        ) {
            removalCandidateSince = null

            return transition(
                if (isStored(snapshot)) {
                    GuardState.POCKET_CONFIRMED
                } else {
                    GuardState.WAITING_FOR_POCKET
                },
                score,
                "Removal confirmation window expired"
            )
        }

        return unchanged(
            score,
            "Accumulating extraction evidence"
        )
    }

    private fun isDecisivePocketExtraction(
        snapshot: SensorSnapshot
    ): Boolean =
        settings.mode ==
            GuardMode.POCKET &&
            profile.hasLightData &&
            snapshot.proximityNear == false &&
            snapshot.lux
                ?.let {
                    it >= profile.exitLux
                } == true

    private fun isStored(
        snapshot: SensorSnapshot
    ): Boolean {
        val darkEnough =
            if (profile.hasLightData) {
                snapshot.lux
                    ?.let {
                        it <= profile.enterLux
                    } ?: false
            } else {
                true
            }

        return when (settings.mode) {
            GuardMode.POCKET ->
                snapshot.proximityNear ==
                    true &&
                    darkEnough

            GuardMode.BAG -> {
                val lowMotion =
                    snapshot.motionScore <
                        effectiveMotionThreshold() *
                        0.85f

                darkEnough &&
                    lowMotion
            }
        }
    }

    private fun calculateExtractionScore(
        snapshot: SensorSnapshot,
        previousLux: Float?
    ): Int {
        var raw = 0f
        var available = 0f

        if (
            settings.mode ==
            GuardMode.POCKET &&
            snapshot.proximityNear != null
        ) {
            available += PROXIMITY_WEIGHT

            if (
                snapshot.proximityNear ==
                false
            ) {
                raw += PROXIMITY_WEIGHT
            }
        }

        if (
            profile.hasLightData &&
            snapshot.lux != null
        ) {
            available += LIGHT_TOTAL_WEIGHT

            val currentLux = snapshot.lux

            if (
                currentLux >=
                profile.exitLux
            ) {
                raw += LIGHT_EXIT_WEIGHT
            }

            val baseLux =
                protectedLux
                    ?: profile.enterLux

            if (
                (
                    currentLux -
                        baseLux
                    )
                    .coerceAtLeast(0f) >=
                8f
            ) {
                raw += LIGHT_DELTA_WEIGHT
            }

            if (
                previousLux != null &&
                currentLux -
                    previousLux >=
                6f
            ) {
                raw += LIGHT_JUMP_WEIGHT
            }
        }

        available += MOTION_WEIGHT

        val motionThreshold =
            effectiveMotionThreshold()

        val motionRatio =
            if (
                motionThreshold <= 0f
            ) {
                0f
            } else {
                snapshot.motionScore /
                    motionThreshold
            }

        raw +=
            (
                motionRatio
                    .coerceIn(0f, 1.5f) /
                    1.5f
                ) *
                MOTION_WEIGHT

        if (profile.hasGyroscopeData) {
            available += ROTATION_WEIGHT

            val rotationThreshold =
                profile.rotationThreshold

            val rotationRatio =
                if (
                    rotationThreshold <= 0f
                ) {
                    0f
                } else {
                    snapshot.rotationScore /
                        rotationThreshold
                }

            raw +=
                (
                    rotationRatio
                        .coerceIn(
                            0f,
                            1.5f
                        ) /
                        1.5f
                    ) *
                    ROTATION_WEIGHT

            if (
                settings.mode ==
                    GuardMode.BAG
            ) {
                available +=
                    BAG_COHERENCE_WEIGHT

                if (
                    snapshot.motionScore >=
                        motionThreshold &&
                    snapshot.rotationScore >=
                        rotationThreshold
                ) {
                    raw +=
                        BAG_COHERENCE_WEIGHT
                }
            }
        }

        if (available <= 0f) {
            return 0
        }

        return (
            raw /
                available *
                100f
            )
            .coerceIn(0f, 100f)
            .roundToInt()
    }

    private fun effectiveMotionThreshold():
        Float =
        (
            profile.motionThreshold *
                settings
                    .sensitivity
                    .motionMultiplier
            )
            .coerceIn(
                0.08f,
                0.65f
            )

    private fun transition(
        newState: GuardState,
        score: Int,
        reason: String
    ): DetectionDecision {
        val changed =
            newState != state

        state = newState

        return DetectionDecision(
            state,
            changed,
            score,
            reason
        )
    }

    private fun unchanged(
        score: Int,
        reason: String
    ) =
        DetectionDecision(
            state,
            false,
            score,
            reason
        )

    private companion object {
        const val PROXIMITY_WEIGHT = 30f
        const val LIGHT_EXIT_WEIGHT = 22f
        const val LIGHT_DELTA_WEIGHT = 8f
        const val LIGHT_JUMP_WEIGHT = 7f
        const val LIGHT_TOTAL_WEIGHT =
            LIGHT_EXIT_WEIGHT +
                LIGHT_DELTA_WEIGHT +
                LIGHT_JUMP_WEIGHT
        const val MOTION_WEIGHT = 23f
        const val ROTATION_WEIGHT = 10f
        const val BAG_COHERENCE_WEIGHT = 10f
    }
}
