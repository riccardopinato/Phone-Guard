package com.riccardopinato.phoneguard.protection

import com.riccardopinato.phoneguard.hardware.HardwareSnapshot
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionReadinessTest {

    @Test
    fun chargingIsReadyOnlyWhilePowerIsConnected() {
        val hardware =
            HardwareSnapshot(
                hasAccelerometer = true,
                hasGyroscope = true,
                hasProximity = true,
                hasLight = true,
                hasFlash = true,
                hasVibrator = true
            )

        val disconnected =
            ProtectionReadinessEngine
                .evaluate(
                    hardware =
                        hardware,
                    powerConnected =
                        false,
                    journeyGuardMode =
                        GuardMode.POCKET
                )
                .first {
                    it.mode ==
                        ProtectionMode.CHARGING
                }

        val connected =
            ProtectionReadinessEngine
                .evaluate(
                    hardware =
                        hardware,
                    powerConnected =
                        true,
                    journeyGuardMode =
                        GuardMode.POCKET
                )
                .first {
                    it.mode ==
                        ProtectionMode.CHARGING
                }

        assertFalse(
            disconnected.ready
        )

        assertTrue(
            connected.ready
        )
    }

    @Test
    fun pocketReadinessStillRequiresProximity() {
        val hardware =
            HardwareSnapshot(
                hasAccelerometer = true,
                hasGyroscope = true,
                hasProximity = false,
                hasLight = true,
                hasFlash = true,
                hasVibrator = true
            )

        val result =
            ProtectionReadinessEngine
                .evaluate(
                    hardware =
                        hardware,
                    powerConnected =
                        true,
                    journeyGuardMode =
                        GuardMode.POCKET
                )
                .first {
                    it.mode ==
                        ProtectionMode.POCKET
                }

        assertFalse(
            result.ready
        )
    }
}
