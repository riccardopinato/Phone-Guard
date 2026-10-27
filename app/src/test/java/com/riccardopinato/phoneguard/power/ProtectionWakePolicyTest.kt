package com.riccardopinato.phoneguard.power

import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.SensorCapabilities
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionWakePolicyTest {

    @Test
    fun tableNeedsLeaseWhenAccelerometerIsNotWakeUp() {
        assertTrue(
            ProtectionWakePolicy.requiresCpuLease(
                ProtectionMode.TABLE,
                null,
                caps(
                    accel = true,
                    accelWake = false
                )
            )
        )

        assertFalse(
            ProtectionWakePolicy.requiresCpuLease(
                ProtectionMode.TABLE,
                null,
                caps(
                    accel = true,
                    accelWake = true
                )
            )
        )
    }

    @Test
    fun pocketCanReleaseLeaseOnlyWhenPrimarySensorsWakeCpu() {
        assertFalse(
            ProtectionWakePolicy.requiresCpuLease(
                ProtectionMode.POCKET,
                GuardMode.POCKET,
                caps(
                    accel = true,
                    proximity = true,
                    accelWake = true,
                    proximityWake = true
                )
            )
        )

        assertTrue(
            ProtectionWakePolicy.requiresCpuLease(
                ProtectionMode.POCKET,
                GuardMode.POCKET,
                caps(
                    accel = true,
                    proximity = true,
                    accelWake = true,
                    proximityWake = false
                )
            )
        )
    }

    @Test
    fun bagCanReleaseLeaseWithWakeUpAccelerometerAndAuxiliary() {
        assertFalse(
            ProtectionWakePolicy.requiresCpuLease(
                ProtectionMode.BAG,
                GuardMode.BAG,
                caps(
                    accel = true,
                    gyro = true,
                    accelWake = true,
                    gyroWake = true
                )
            )
        )

        assertTrue(
            ProtectionWakePolicy.requiresCpuLease(
                ProtectionMode.BAG,
                GuardMode.BAG,
                caps(
                    accel = true,
                    light = true,
                    accelWake = true,
                    lightWake = false
                )
            )
        )
    }

    @Test
    fun chargingDoesNotHoldCpuLease() {
        assertFalse(
            ProtectionWakePolicy.requiresCpuLease(
                ProtectionMode.CHARGING,
                null,
                caps()
            )
        )
    }

    @Test
    fun journeyUsesItsSelectedGuardProfile() {
        val capabilities =
            caps(
                accel = true,
                proximity = true,
                gyro = true,
                accelWake = true,
                proximityWake = true,
                gyroWake = false
            )

        assertFalse(
            ProtectionWakePolicy.requiresCpuLease(
                ProtectionMode.JOURNEY,
                GuardMode.POCKET,
                capabilities
            )
        )

        assertTrue(
            ProtectionWakePolicy.requiresCpuLease(
                ProtectionMode.JOURNEY,
                GuardMode.BAG,
                capabilities
            )
        )
    }

    private fun caps(
        accel: Boolean = false,
        proximity: Boolean = false,
        light: Boolean = false,
        gyro: Boolean = false,
        accelWake: Boolean = false,
        proximityWake: Boolean = false,
        lightWake: Boolean = false,
        gyroWake: Boolean = false
    ) =
        SensorCapabilities(
            hasProximity = proximity,
            hasLight = light,
            hasAccelerometer = accel,
            hasGyroscope = gyro,
            proximityWakeUp = proximityWake,
            lightWakeUp = lightWake,
            accelerometerWakeUp = accelWake,
            gyroscopeWakeUp = gyroWake,
            hasFlash = false
        )
}
