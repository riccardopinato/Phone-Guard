package com.riccardopinato.phoneguard.pocket.detection

import com.riccardopinato.phoneguard.pocket.model.CalibrationProfile
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.GuardSettings
import com.riccardopinato.phoneguard.pocket.model.GuardState
import com.riccardopinato.phoneguard.pocket.model.PocketSensitivity
import com.riccardopinato.phoneguard.pocket.model.SensorSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartProtectionEngineTest {
    private val profile =
        profile(
            mode = GuardMode.POCKET,
            light = true,
            gyro = true
        )

    private val settings =
        GuardSettings(
            mode = GuardMode.POCKET,
            sensitivity =
                PocketSensitivity.NORMAL,
            pocketStableMs = 1_200L,
            removalWindowMs = 1_600L
        )

    @Test
    fun stablePocketIsRequired() {
        val engine =
            SmartProtectionEngine(
                settings,
                profile
            )

        engine.process(
            s(
                0L,
                2f,
                true,
                0.01f,
                0.01f
            )
        )

        val result =
            engine.process(
                s(
                    1_250L,
                    2f,
                    true,
                    0.01f,
                    0.01f
                )
            )

        assertEquals(
            GuardState.POCKET_CONFIRMED,
            result.state
        )
    }

    @Test
    fun strongExtractionEntersOwnerCheck() {
        val engine =
            SmartProtectionEngine(
                settings,
                profile
            )

        protectPocket(
            engine,
            light = true
        )

        val suspect =
            engine.process(
                s(
                    1_500L,
                    30f,
                    false,
                    0.35f,
                    0.90f
                )
            )

        assertEquals(
            GuardState.REMOVAL_SUSPECTED,
            suspect.state
        )

        val confirm =
            engine.process(
                s(
                    1_650L,
                    45f,
                    false,
                    0.45f,
                    1.20f
                )
            )

        assertEquals(
            GuardState.OWNER_CHECK,
            confirm.state
        )

        assertTrue(
            confirm.confidenceScore >=
                PocketSensitivity
                    .NORMAL
                    .ownerCheckScore
        )
    }

    @Test
    fun pocketWithoutLightOrGyroCanReachOwnerCheckWithMotionCorroboration() {
        val degradedProfile =
            profile(
                mode = GuardMode.POCKET,
                light = false,
                gyro = false
            )

        val engine =
            SmartProtectionEngine(
                settings,
                degradedProfile
            )

        protectPocket(
            engine,
            light = false
        )

        val suspect =
            engine.process(
                s(
                    1_500L,
                    null,
                    false,
                    0.55f,
                    0f
                )
            )

        assertEquals(
            GuardState.REMOVAL_SUSPECTED,
            suspect.state
        )

        val confirm =
            engine.process(
                s(
                    1_650L,
                    null,
                    false,
                    0.55f,
                    0f
                )
            )

        assertEquals(
            GuardState.OWNER_CHECK,
            confirm.state
        )
    }

    @Test
    fun pocketWithoutLightOrGyroDoesNotOwnerCheckOnProximityAlone() {
        val degradedProfile =
            profile(
                mode = GuardMode.POCKET,
                light = false,
                gyro = false
            )

        val engine =
            SmartProtectionEngine(
                settings,
                degradedProfile
            )

        protectPocket(
            engine,
            light = false
        )

        engine.process(
            s(
                1_500L,
                null,
                false,
                0.01f,
                0f
            )
        )

        val second =
            engine.process(
                s(
                    1_650L,
                    null,
                    false,
                    0.01f,
                    0f
                )
            )

        assertEquals(
            GuardState.REMOVAL_SUSPECTED,
            second.state
        )
    }

    @Test
    fun bagWithLightCanReachOwnerCheck() {
        val engine =
            SmartProtectionEngine(
                settings.copy(
                    mode = GuardMode.BAG
                ),
                profile(
                    mode = GuardMode.BAG,
                    light = true,
                    gyro = false
                )
            )

        protectBag(
            engine,
            lux = 2f
        )

        assertEquals(
            GuardState.REMOVAL_SUSPECTED,
            engine.process(
                s(
                    1_500L,
                    35f,
                    null,
                    0.50f,
                    0f
                )
            ).state
        )

        assertEquals(
            GuardState.OWNER_CHECK,
            engine.process(
                s(
                    1_650L,
                    35f,
                    null,
                    0.50f,
                    0f
                )
            ).state
        )
    }

    @Test
    fun bagWithGyroCanReachOwnerCheckWithoutLight() {
        val engine =
            SmartProtectionEngine(
                settings.copy(
                    mode = GuardMode.BAG
                ),
                profile(
                    mode = GuardMode.BAG,
                    light = false,
                    gyro = true
                )
            )

        protectBag(
            engine,
            lux = null
        )

        assertEquals(
            GuardState.REMOVAL_SUSPECTED,
            engine.process(
                s(
                    1_500L,
                    null,
                    null,
                    0.50f,
                    1.20f
                )
            ).state
        )

        assertEquals(
            GuardState.OWNER_CHECK,
            engine.process(
                s(
                    1_650L,
                    null,
                    null,
                    0.50f,
                    1.20f
                )
            ).state
        )
    }

    @Test
    fun pocketExtractionWithProximityAndLightStartsOwnerCheckWithoutMotion() {
        val engine =
            SmartProtectionEngine(
                settings,
                profile
            )

        protectPocket(
            engine,
            light = true
        )

        val suspect =
            engine.process(
                s(
                    1_500L,
                    30f,
                    false,
                    0.01f,
                    0.01f
                )
            )

        assertEquals(
            GuardState.REMOVAL_SUSPECTED,
            suspect.state
        )

        val confirm =
            engine.process(
                s(
                    1_560L,
                    30f,
                    false,
                    0.01f,
                    0.01f
                )
            )

        assertEquals(
            GuardState.OWNER_CHECK,
            confirm.state
        )
    }

    @Test
    fun proximityOnlyDoesNotConfirmExtractionWithoutOtherEvidence() {
        val engine =
            SmartProtectionEngine(
                settings,
                profile
            )

        protectPocket(
            engine,
            light = true
        )

        val suspect =
            engine.process(
                s(
                    1_500L,
                    2f,
                    false,
                    0.01f,
                    0.01f
                )
            )

        assertEquals(
            GuardState.POCKET_CONFIRMED,
            suspect.state
        )
    }

    @Test
    fun ownerVerificationReturnsToAutoRearm() {
        val engine =
            SmartProtectionEngine(
                settings,
                profile
            )

        protectPocket(
            engine,
            light = true
        )

        engine.process(
            s(
                1_500L,
                30f,
                false,
                0.40f,
                1.00f
            )
        )

        engine.process(
            s(
                1_650L,
                40f,
                false,
                0.45f,
                1.20f
            )
        )

        val verified =
            engine.ownerVerified()

        assertEquals(
            GuardState.WAITING_FOR_POCKET,
            verified.state
        )
    }

    @Test
    fun ownerTimeoutProducesAlarm() {
        val engine =
            SmartProtectionEngine(
                settings,
                profile
            )

        engine.forceState(
            GuardState.OWNER_CHECK
        )

        val result =
            engine.ownerCheckExpired()

        assertEquals(
            GuardState.ALARM,
            result.state
        )
    }

    @Test
    fun lightOnlyDoesNotImmediatelyTriggerNormalPocketAlarm() {
        val engine =
            SmartProtectionEngine(
                settings,
                profile
            )

        protectPocket(
            engine,
            light = true
        )

        val result =
            engine.process(
                s(
                    1_500L,
                    40f,
                    true,
                    0.01f,
                    0.01f
                )
            )

        assertEquals(
            GuardState.POCKET_CONFIRMED,
            result.state
        )
    }

    private fun protectPocket(
        engine: SmartProtectionEngine,
        light: Boolean
    ) {
        val lux =
            if (light) 2f else null

        engine.process(
            s(
                0L,
                lux,
                true,
                0.01f,
                0f
            )
        )

        engine.process(
            s(
                1_250L,
                lux,
                true,
                0.01f,
                0f
            )
        )
    }

    private fun protectBag(
        engine: SmartProtectionEngine,
        lux: Float?
    ) {
        engine.process(
            s(
                0L,
                lux,
                null,
                0.01f,
                0.01f
            )
        )

        engine.process(
            s(
                1_250L,
                lux,
                null,
                0.01f,
                0.01f
            )
        )
    }

    private fun profile(
        mode: GuardMode,
        light: Boolean,
        gyro: Boolean
    ) =
        CalibrationProfile(
            mode = mode,
            createdAt = 1L,
            enterLux = 5f,
            exitLux = 12f,
            motionNoise = 0.03f,
            rotationNoise = 0.05f,
            sampleCount = 50,
            hasLightData = light,
            hasGyroscopeData = gyro
        )

    private fun s(
        time: Long,
        lux: Float?,
        near: Boolean?,
        motion: Float,
        rotation: Float
    ) =
        SensorSnapshot(
            lux = lux,
            proximityNear = near,
            motionScore = motion,
            rotationScore = rotation,
            timestampMs = time
        )
}
