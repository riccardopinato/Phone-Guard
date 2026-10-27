package com.riccardopinato.phoneguard.diagnostics

import com.riccardopinato.phoneguard.model.Sensitivity
import com.riccardopinato.phoneguard.pocket.model.PocketSensitivity
import org.junit.Assert.assertEquals
import org.junit.Test

class SensitivityRecommendationEngineTest {

    @Test
    fun stableEnvironmentRecommendsHighSensitivity() {
        val result =
            SensitivityRecommendationEngine
                .recommend(
                    motionNoise = 0.02f,
                    rotationNoise = 0.02f,
                    sampleCount = 25
                )

        assertEquals(
            Sensitivity.HIGH,
            result.tableSensitivity
        )
        assertEquals(
            PocketSensitivity.HIGH,
            result.pocketSensitivity
        )
    }

    @Test
    fun moderateEnvironmentRecommendsBalancedSensitivity() {
        val result =
            SensitivityRecommendationEngine
                .recommend(
                    motionNoise = 0.08f,
                    rotationNoise = 0.10f,
                    sampleCount = 25
                )

        assertEquals(
            Sensitivity.MEDIUM,
            result.tableSensitivity
        )
        assertEquals(
            PocketSensitivity.NORMAL,
            result.pocketSensitivity
        )
    }

    @Test
    fun noisyEnvironmentRecommendsLowSensitivity() {
        val result =
            SensitivityRecommendationEngine
                .recommend(
                    motionNoise = 0.22f,
                    rotationNoise = 0.40f,
                    sampleCount = 25
                )

        assertEquals(
            Sensitivity.LOW,
            result.tableSensitivity
        )
        assertEquals(
            PocketSensitivity.LOW,
            result.pocketSensitivity
        )
    }
}
