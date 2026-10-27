package com.riccardopinato.phoneguard.autoguard

/**
 * Offline context classification. Confidence is a relative evidence score, NOT a probability.
 * A recommendation does not arm protection: activation policy and live calibration remain required.
 */
enum class AutoGuardCandidate { NONE, TABLE, POCKET }

data class AutoGuardObservation(
    val lightObserved: Boolean,
    val lux: Float?,
    val proximityObserved: Boolean,
    val proximityNear: Boolean?,
    val accelerometerObserved: Boolean,
    val motionScore: Float,
    val gyroscopeObserved: Boolean,
    val rotationScore: Float
)

data class AutoGuardAssessment(
    val candidate: AutoGuardCandidate,
    val reason: String,
    val evidenceScore: Int
)

object AutoGuardDecisionEngine {
    fun assess(
        observation: AutoGuardObservation?,
        allowPocket: Boolean,
        allowTable: Boolean
    ): AutoGuardAssessment {
        if (observation == null || !observation.accelerometerObserved ||
            !observation.motionScore.isFinite()
        ) {
            return AutoGuardAssessment(AutoGuardCandidate.NONE, "missing_motion", 0)
        }

        // A covered face-down phone on a desk is not automatically a pocket.
        // Require actual motion evidence in addition to TWO independent sensor types.
        if (allowPocket &&
            observation.lightObserved &&
            observation.lux != null && observation.lux.isFinite() &&
            observation.lux in 0f..8f &&
            observation.proximityObserved && observation.proximityNear == true &&
            observation.motionScore >= 0.055f
        ) {
            return AutoGuardAssessment(AutoGuardCandidate.POCKET, "covered_and_moving", 90)
        }

        if (allowTable && observation.proximityNear != true &&
            observation.motionScore <= 0.018f &&
            (!observation.gyroscopeObserved || observation.rotationScore <= 0.12f)
        ) {
            return AutoGuardAssessment(AutoGuardCandidate.TABLE, "stable_surface", 72)
        }

        return AutoGuardAssessment(AutoGuardCandidate.NONE, "ambiguous_context", 0)
    }
}

/** Requires repeated, time-separated evidence; a single burst cannot request arming. */
class AutoGuardStability(
    private val requiredObservations: Int = 2,
    private val minimumStableMs: Long = 12_000L
) {
    private var candidate = AutoGuardCandidate.NONE
    private var firstSeenAt = 0L
    private var seen = 0

    fun observe(result: AutoGuardAssessment, nowElapsedMs: Long): AutoGuardCandidate {
        if (result.candidate == AutoGuardCandidate.NONE) {
            reset()
            return AutoGuardCandidate.NONE
        }
        if (candidate != result.candidate || nowElapsedMs < firstSeenAt) {
            candidate = result.candidate
            firstSeenAt = nowElapsedMs
            seen = 1
        } else {
            seen++
        }
        return if (seen >= requiredObservations &&
            nowElapsedMs - firstSeenAt >= minimumStableMs
        ) candidate else AutoGuardCandidate.NONE
    }

    fun reset() {
        candidate = AutoGuardCandidate.NONE
        firstSeenAt = 0L
        seen = 0
    }
}

object AutoGuardTiming {
    fun idleElapsed(nowElapsedMs: Long, screenOffAtElapsedMs: Long, idleMinutes: Int): Boolean =
        idleMinutes in setOf(1, 3, 5, 10, 15) &&
            nowElapsedMs >= screenOffAtElapsedMs &&
            nowElapsedMs - screenOffAtElapsedMs >= idleMinutes * 60_000L
}
