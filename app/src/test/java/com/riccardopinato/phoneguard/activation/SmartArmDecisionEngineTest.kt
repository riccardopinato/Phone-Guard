package com.riccardopinato.phoneguard.activation

import com.riccardopinato.phoneguard.model.ProtectionMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartArmDecisionEngineTest {

    @Test
    fun externalPowerHasHighestPriority() {
        val decision =
            SmartArmDecisionEngine.resolve(
                externalPowerConnected = true,
                accessoryConnected = true,
                observation =
                    SmartArmObservation(
                        proximityNear = true,
                        accelerometerObserved = true,
                        proximityObserved = true
                    ),
                fallbackMode =
                    ProtectionMode.POCKET
            )

        assertEquals(
            ProtectionMode.CHARGING,
            decision.mode
        )
        assertEquals(
            SmartArmReason.EXTERNAL_POWER,
            decision.reason
        )
    }

    @Test
    fun accessoryWinsBeforeSensorContext() {
        val decision =
            SmartArmDecisionEngine.resolve(
                externalPowerConnected = false,
                accessoryConnected = true,
                observation =
                    SmartArmObservation(
                        proximityNear = true,
                        accelerometerObserved = true,
                        proximityObserved = true
                    ),
                fallbackMode =
                    ProtectionMode.TABLE
            )

        assertEquals(
            ProtectionMode.ACCESSORY,
            decision.mode
        )
    }

    @Test
    fun proximityNearSelectsPocket() {
        val decision =
            SmartArmDecisionEngine.resolve(
                externalPowerConnected = false,
                accessoryConnected = false,
                observation =
                    SmartArmObservation(
                        proximityNear = true,
                        motionScore = 0.03f,
                        accelerometerObserved = true,
                        proximityObserved = true
                    ),
                fallbackMode =
                    ProtectionMode.TABLE
            )

        assertEquals(
            ProtectionMode.POCKET,
            decision.mode
        )
    }

    @Test
    fun darkEnclosedContextSelectsBag() {
        val decision =
            SmartArmDecisionEngine.resolve(
                externalPowerConnected = false,
                accessoryConnected = false,
                observation =
                    SmartArmObservation(
                        lux = 3f,
                        proximityNear = false,
                        motionScore = 0.03f,
                        rotationScore = 0.02f,
                        accelerometerObserved = true,
                        proximityObserved = true,
                        lightObserved = true,
                        gyroscopeObserved = true
                    ),
                fallbackMode =
                    ProtectionMode.TABLE
            )

        assertEquals(
            ProtectionMode.BAG,
            decision.mode
        )
    }

    @Test
    fun accelerometerEvidenceFallsBackToTable() {
        val decision =
            SmartArmDecisionEngine.resolve(
                externalPowerConnected = false,
                accessoryConnected = false,
                observation =
                    SmartArmObservation(
                        lux = 80f,
                        proximityNear = false,
                        motionScore = 0.01f,
                        accelerometerObserved = true,
                        proximityObserved = true,
                        lightObserved = true
                    ),
                fallbackMode =
                    ProtectionMode.BAG
            )

        assertEquals(
            ProtectionMode.TABLE,
            decision.mode
        )
    }

    @Test
    fun unavailableContextNeverFallsBackToTransientConnectionModes() {
        for (
            fallback in
            listOf(
                ProtectionMode.CHARGING,
                ProtectionMode.ACCESSORY
            )
        ) {
            val decision =
                SmartArmDecisionEngine.resolve(
                    externalPowerConnected = false,
                    accessoryConnected = false,
                    observation = null,
                    fallbackMode = fallback
                )

            assertEquals(
                ProtectionMode.TABLE,
                decision.mode
            )
        }
    }
}
