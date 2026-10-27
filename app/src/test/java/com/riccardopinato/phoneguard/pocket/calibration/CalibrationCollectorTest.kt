package com.riccardopinato.phoneguard.pocket.calibration

import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.SensorSnapshot
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationCollectorTest {
    @Test
    fun derivedExitLuxIsHigherThanEnterLux() {
        val collector = CalibrationCollector(GuardMode.POCKET)

        repeat(20) { i ->
            collector.add(
                SensorSnapshot(
                    lux = 1.5f + (i % 3) * 0.3f,
                    proximityNear = true,
                    motionScore = 0.02f,
                    rotationScore = 0.04f,
                    timestampMs = i * 100L
                )
            )
        }

        val profile = collector.build(100L)

        assertTrue(profile.exitLux > profile.enterLux)
        assertTrue(profile.sampleCount >= 20)
    }
}
