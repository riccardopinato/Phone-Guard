package com.riccardopinato.phoneguard.autoguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoGuardDecisionEngineTest {
    private fun sample(
        lux: Float? = 0f,
        near: Boolean? = true,
        motion: Float = 0.09f,
        lightSeen: Boolean = true,
        proximitySeen: Boolean = true,
        accelerometerSeen: Boolean = true,
        rotation: Float = 0f
    ) = AutoGuardObservation(
        lightObserved = lightSeen,
        lux = lux,
        proximityObserved = proximitySeen,
        proximityNear = near,
        accelerometerObserved = accelerometerSeen,
        motionScore = motion,
        gyroscopeObserved = true,
        rotationScore = rotation
    )

    @Test fun darkNearMovingSuggestsPocketOnlyAsEvidence() {
        val assessment = AutoGuardDecisionEngine.assess(sample(), true, true)
        assertEquals(AutoGuardCandidate.POCKET, assessment.candidate)
        assertEquals("covered_and_moving", assessment.reason)
        assertTrue(assessment.evidenceScore < 100)
    }

    @Test fun coveredButStationaryNeverClaimsPocket() {
        assertEquals(AutoGuardCandidate.NONE,
            AutoGuardDecisionEngine.assess(sample(motion = 0f), true, true).candidate)
    }

    @Test fun darkNearWithoutObservedLightCannotArmPocket() {
        assertEquals(AutoGuardCandidate.NONE,
            AutoGuardDecisionEngine.assess(sample(lightSeen = false), true, true).candidate)
    }

    @Test fun missingProximityNeverClaimsPocket() {
        assertEquals(AutoGuardCandidate.NONE,
            AutoGuardDecisionEngine.assess(
                sample(proximitySeen = false, near = null), true, false
            ).candidate)
    }

    @Test fun phoneOnStableUncoveredSurfaceSuggestsTable() {
        assertEquals(AutoGuardCandidate.TABLE,
            AutoGuardDecisionEngine.assess(
                sample(lux = 120f, near = false, motion = 0.002f), true, true
            ).candidate)
    }

    @Test fun motionOrRotationPreventsTable() {
        assertEquals(AutoGuardCandidate.NONE,
            AutoGuardDecisionEngine.assess(
                sample(lux = 120f, near = false, motion = 0.2f), true, true
            ).candidate)
        assertEquals(AutoGuardCandidate.NONE,
            AutoGuardDecisionEngine.assess(
                sample(lux = 120f, near = false, motion = 0f, rotation = 1f),
                true, true
            ).candidate)
    }

    @Test fun optionsRespectExplicitOptIn() {
        assertEquals(AutoGuardCandidate.NONE,
            AutoGuardDecisionEngine.assess(sample(), false, true).candidate)
        assertEquals(AutoGuardCandidate.NONE,
            AutoGuardDecisionEngine.assess(
                sample(lux = 120f, near = false, motion = 0f), true, false
            ).candidate)
    }

    @Test fun inadequateAndInvalidAccelerometerCannotArm() {
        assertEquals(AutoGuardCandidate.NONE,
            AutoGuardDecisionEngine.assess(sample(accelerometerSeen = false), true, true).candidate)
        assertEquals(AutoGuardCandidate.NONE,
            AutoGuardDecisionEngine.assess(sample(motion = Float.NaN), true, true).candidate)
    }

    @Test fun stabilizerRequiresTwoSeparatedReadings() {
        val stable = AutoGuardStability()
        val pocket = AutoGuardAssessment(AutoGuardCandidate.POCKET, "covered_and_moving", 90)
        assertEquals(AutoGuardCandidate.NONE, stable.observe(pocket, 1000L))
        assertEquals(AutoGuardCandidate.NONE, stable.observe(pocket, 4000L))
        assertEquals(AutoGuardCandidate.POCKET, stable.observe(pocket, 13000L))
    }

    @Test fun conflictedEvidenceResetsStability() {
        val stable = AutoGuardStability()
        val pocket = AutoGuardAssessment(AutoGuardCandidate.POCKET, "covered_and_moving", 90)
        val ambiguous = AutoGuardAssessment(AutoGuardCandidate.NONE, "ambiguous", 0)
        stable.observe(pocket, 1_000L)
        stable.observe(ambiguous, 9_000L)
        assertEquals(AutoGuardCandidate.NONE, stable.observe(pocket, 20_000L))
        assertEquals(AutoGuardCandidate.POCKET, stable.observe(pocket, 32_000L))
    }

    @Test fun monotonicTimingCannotBeSkippedByClockRollback() {
        assertFalse(AutoGuardTiming.idleElapsed(10L, 100L, 1))
        assertFalse(AutoGuardTiming.idleElapsed(60_099L, 100L, 1))
        assertTrue(AutoGuardTiming.idleElapsed(60_100L, 100L, 1))
        assertFalse(AutoGuardTiming.idleElapsed(1_000_000L, 100L, 2))
    }

    @Test fun inputOutsideAllowedIdleOptionsIsRejected() {
        assertFalse(AutoGuardTiming.idleElapsed(1_000_000L, 0L, 0))
        assertFalse(AutoGuardTiming.idleElapsed(1_000_000L, 0L, 99))
    }
}
