package com.riccardopinato.phoneguard.diagnostics

import android.content.Context
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.model.Sensitivity
import com.riccardopinato.phoneguard.pocket.model.PocketSensitivity
import com.riccardopinato.phoneguard.pocket.sensor.SamplingMode
import com.riccardopinato.phoneguard.pocket.sensor.SensorController
import kotlinx.coroutines.delay

data class SensitivityRecommendation(
    val tableSensitivity: Sensitivity,
    val pocketSensitivity: PocketSensitivity,
    val motionNoise: Float,
    val rotationNoise: Float,
    val sampleCount: Int,
    val explanation: String
)

object SensitivityRecommendationEngine {
    fun recommend(
        motionNoise: Float,
        rotationNoise: Float,
        sampleCount: Int
    ): SensitivityRecommendation {
        val combined =
            maxOf(
                motionNoise,
                rotationNoise * 0.55f
            )

        val table =
            when {
                combined < 0.045f ->
                    Sensitivity.HIGH

                combined < 0.13f ->
                    Sensitivity.MEDIUM

                else ->
                    Sensitivity.LOW
            }

        val pocket =
            when {
                combined < 0.055f ->
                    PocketSensitivity.HIGH

                combined < 0.16f ->
                    PocketSensitivity.NORMAL

                else ->
                    PocketSensitivity.LOW
            }

        val explanation =
            when (table) {
                Sensitivity.HIGH ->
                    "Very stable environment: higher sensitivity should remain practical."

                Sensitivity.MEDIUM ->
                    "Moderate background movement: normal sensitivity is the balanced choice."

                Sensitivity.LOW ->
                    "Frequent background movement detected: lower sensitivity should reduce false alarms."
            }

        return SensitivityRecommendation(
            tableSensitivity = table,
            pocketSensitivity = pocket,
            motionNoise = motionNoise,
            rotationNoise = rotationNoise,
            sampleCount = sampleCount,
            explanation = explanation
        )
    }
}

class SensitivityAdvisor(
    context: Context
) {
    private val appContext =
        context.applicationContext

    suspend fun analyze():
        SensitivityRecommendation {
        val motion =
            mutableListOf<Float>()

        val rotation =
            mutableListOf<Float>()

        val controller =
            SensorController(
                appContext
            ) {
                snapshot ->
                if (
                    snapshot.accelerometerObserved
                ) {
                    motion +=
                        snapshot.motionScore
                }

                if (
                    snapshot.gyroscopeObserved
                ) {
                    rotation +=
                        snapshot.rotationScore
                }
            }

        try {
            controller.start(
                SamplingMode.NORMAL
            )

            delay(
                SAMPLE_WINDOW_MS
            )
        } finally {
            controller.stop()
        }

        val motionNoise =
            percentile(
                motion,
                0.90f
            ) ?: 0f

        val rotationNoise =
            percentile(
                rotation,
                0.90f
            ) ?: 0f

        return SensitivityRecommendationEngine
            .recommend(
                motionNoise =
                    motionNoise,
                rotationNoise =
                    rotationNoise,
                sampleCount =
                    motion.size
            )
    }

    private fun percentile(
        values: List<Float>,
        fraction: Float
    ): Float? {
        if (
            values.isEmpty()
        ) {
            return null
        }

        val sorted =
            values.sorted()

        val index =
            (
                sorted.lastIndex *
                    fraction
                )
                .toInt()
                .coerceIn(
                    0,
                    sorted.lastIndex
                )

        return sorted[
            index
        ]
    }

    private companion object {
        const val SAMPLE_WINDOW_MS =
            1_800L
    }
}
