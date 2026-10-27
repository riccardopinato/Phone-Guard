package com.riccardopinato.phoneguard.pocket.detection

import com.riccardopinato.phoneguard.pocket.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartProtectionEngineTest {
    private val profile =
        CalibrationProfile(
            mode = GuardMode.POCKET,
            createdAt = 1L,
            enterLux = 5f,
            exitLux = 12f,
            motionNoise = 0.03f,
            rotationNoise = 0.05f,
            sampleCount = 50,
            hasLightData = true,
            hasGyroscopeData = true
        )

    private val settings =
        GuardSettings(
            mode = GuardMode.POCKET,
            sensitivity = PocketSensitivity.NORMAL,
            pocketStableMs = 1_200L,
            removalWindowMs = 1_600L
        )

    @Test
    fun stablePocketIsRequired() {
        val engine = SmartProtectionEngine(settings, profile)

        engine.process(s(0L, 2f, true, 0.01f, 0.01f))

        val result =
            engine.process(s(1_250L, 2f, true, 0.01f, 0.01f))

        assertEquals(GuardState.POCKET_CONFIRMED, result.state)
    }

    @Test
    fun strongExtractionEntersOwnerCheck() {
        val engine = SmartProtectionEngine(settings, profile)
        protect(engine)

        val suspect =
            engine.process(s(1_500L, 30f, false, 0.35f, 0.90f))

        assertEquals(GuardState.REMOVAL_SUSPECTED, suspect.state)

        val confirm =
            engine.process(s(1_650L, 45f, false, 0.45f, 1.20f))

        assertEquals(GuardState.OWNER_CHECK, confirm.state)
        assertTrue(
            confirm.confidenceScore >=
                PocketSensitivity.NORMAL.ownerCheckScore
        )
    }


    @Test
    fun pocketExtractionWithProximityAndLightStartsOwnerCheckWithoutMotion() {
        val engine = SmartProtectionEngine(settings, profile)
        protect(engine)

        val suspect =
            engine.process(s(1_500L, 30f, false, 0.01f, 0.01f))

        assertEquals(GuardState.REMOVAL_SUSPECTED, suspect.state)
        assertTrue(
            suspect.confidenceScore <
                PocketSensitivity.NORMAL.ownerCheckScore
        )

        val confirm =
            engine.process(s(1_560L, 30f, false, 0.01f, 0.01f))

        assertEquals(GuardState.OWNER_CHECK, confirm.state)
    }

    @Test
    fun proximityOnlyDoesNotConfirmExtractionWithoutOtherEvidence() {
        val engine = SmartProtectionEngine(settings, profile)
        protect(engine)

        val suspect =
            engine.process(s(1_500L, 2f, false, 0.01f, 0.01f))

        assertEquals(GuardState.POCKET_CONFIRMED, suspect.state)
    }

    @Test
    fun ownerVerificationReturnsToAutoRearm() {
        val engine = SmartProtectionEngine(settings, profile)
        protect(engine)

        engine.process(s(1_500L, 30f, false, 0.40f, 1.00f))
        engine.process(s(1_650L, 40f, false, 0.45f, 1.20f))

        val verified = engine.ownerVerified()

        assertEquals(GuardState.WAITING_FOR_POCKET, verified.state)
    }

    @Test
    fun ownerTimeoutProducesAlarm() {
        val engine = SmartProtectionEngine(settings, profile)
        engine.forceState(GuardState.OWNER_CHECK)

        val result = engine.ownerCheckExpired()

        assertEquals(GuardState.ALARM, result.state)
    }

    @Test
    fun lightOnlyDoesNotImmediatelyTriggerNormalPocketAlarm() {
        val engine = SmartProtectionEngine(settings, profile)
        protect(engine)

        val result =
            engine.process(s(1_500L, 40f, true, 0.01f, 0.01f))

        assertEquals(GuardState.POCKET_CONFIRMED, result.state)
    }

    private fun protect(engine: SmartProtectionEngine) {
        engine.process(s(0L, 2f, true, 0.01f, 0.01f))
        engine.process(s(1_250L, 2f, true, 0.01f, 0.01f))
    }

    private fun s(
        time: Long,
        lux: Float?,
        near: Boolean?,
        motion: Float,
        rotation: Float
    ) = SensorSnapshot(
        lux = lux,
        proximityNear = near,
        motionScore = motion,
        rotationScore = rotation,
        timestampMs = time
    )
}
