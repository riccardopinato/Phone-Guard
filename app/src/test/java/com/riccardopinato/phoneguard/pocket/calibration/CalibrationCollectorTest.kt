package com.riccardopinato.phoneguard.pocket.calibration

import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.SensorSnapshot
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationCollectorTest {

    @Test
    fun derivedExitLuxIsHigherThanEnterLux() {
        val collector =
            CalibrationCollector(
                mode = GuardMode.POCKET,
                hasLightSensor = true,
                hasGyroscopeSensor = true
            )

        repeat(20) { i ->
            collector.add(
                SensorSnapshot(
                    lux =
                        1.5f +
                            (i % 3) *
                            0.3f,
                    proximityNear = true,
                    motionScore = 0.02f,
                    rotationScore = 0.04f,
                    timestampMs =
                        i * 100L
                )
            )
        }

        val profile =
            collector.build(100L)

        assertTrue(
            profile.exitLux >
                profile.enterLux
        )

        assertTrue(
            profile.sampleCount >= 20
        )

        assertTrue(
            profile.hasLightData
        )

        assertTrue(
            profile.hasGyroscopeData
        )
    }

    @Test
    fun capabilitiesAreNotInferredFromNonZeroNoise() {
        val collector =
            CalibrationCollector(
                mode = GuardMode.POCKET,
                hasLightSensor = false,
                hasGyroscopeSensor = true
            )

        repeat(10) { i ->
            collector.add(
                SensorSnapshot(
                    lux = null,
                    proximityNear = true,
                    motionScore = 0.01f,
                    rotationScore = 0f,
                    timestampMs =
                        i * 100L
                )
            )
        }

        val profile =
            collector.build(100L)

        assertTrue(
            !profile.hasLightData
        )

        assertTrue(
            profile.hasGyroscopeData
        )
    }
}
