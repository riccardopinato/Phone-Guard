package com.riccardopinato.phoneguard.sensor

import com.riccardopinato.phoneguard.model.Sensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionDetectionPolicyTest {

    @Test
    fun higherSensitivity_usesLowerThresholds() {
        val high =
            MotionDetectionPolicy.accelerationThreshold(
                Sensitivity.HIGH,
                noiseFloor = 0f
            )

        val medium =
            MotionDetectionPolicy.accelerationThreshold(
                Sensitivity.MEDIUM,
                noiseFloor = 0f
            )

        val low =
            MotionDetectionPolicy.accelerationThreshold(
                Sensitivity.LOW,
                noiseFloor = 0f
            )

        assertTrue(high < medium)
        assertTrue(medium < low)
        assertTrue(
            MotionDetectionPolicy.gyroscopeThreshold(
                Sensitivity.HIGH
            ) <
                MotionDetectionPolicy.gyroscopeThreshold(
                    Sensitivity.MEDIUM
                )
        )
    }

    @Test
    fun noiseFloor_canRaiseAccelerationThreshold() {
        assertEquals(
            2.70f,
            MotionDetectionPolicy.accelerationThreshold(
                Sensitivity.HIGH,
                noiseFloor = 0.50f
            ),
            0.0001f
        )
    }

    @Test
    fun highSensitivity_requiresFewerHits() {
        assertEquals(
            2,
            MotionDetectionPolicy.requiredHits(
                Sensitivity.HIGH
            )
        )

        assertEquals(
            3,
            MotionDetectionPolicy.requiredHits(
                Sensitivity.MEDIUM
            )
        )
    }
}
