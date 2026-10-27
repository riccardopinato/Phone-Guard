package com.riccardopinato.phoneguard.pocket.calibration

import com.riccardopinato.phoneguard.pocket.model.CalibrationProfile
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.SensorSnapshot

class CalibrationCollector(
    private val mode: GuardMode,
    private val hasLightSensor: Boolean,
    private val hasGyroscopeSensor: Boolean
) {
    private val luxValues = mutableListOf<Float>()
    private val motionValues = mutableListOf<Float>()
    private val rotationValues = mutableListOf<Float>()

    fun add(snapshot: SensorSnapshot) {
        if (hasLightSensor) {
            snapshot.lux
                ?.takeIf { it.isFinite() && it >= 0f }
                ?.let(luxValues::add)
        }

        motionValues +=
            snapshot.motionScore
                .coerceAtLeast(0f)

        if (hasGyroscopeSensor) {
            rotationValues +=
                snapshot.rotationScore
                    .coerceAtLeast(0f)
        }
    }

    fun build(now: Long): CalibrationProfile {
        val medianLux =
            percentile(luxValues, 0.50f) ?: 3f

        val highStoredLux =
            percentile(luxValues, 0.85f)
                ?: medianLux

        val enterLux =
            (highStoredLux + 1.5f)
                .coerceIn(1f, 20f)

        val exitLux =
            maxOf(
                enterLux + 5f,
                medianLux * 2.5f
            ).coerceIn(
                enterLux + 2f,
                60f
            )

        val motionNoise =
            (percentile(motionValues, 0.90f)
                ?: 0.03f)
                .coerceIn(0f, 0.35f)

        val rotationNoise =
            (percentile(rotationValues, 0.90f)
                ?: 0.05f)
                .coerceIn(0f, 0.80f)

        return CalibrationProfile(
            mode = mode,
            createdAt = now,
            enterLux = enterLux,
            exitLux = exitLux,
            motionNoise = motionNoise,
            rotationNoise = rotationNoise,
            sampleCount = maxOf(
                luxValues.size,
                motionValues.size,
                rotationValues.size
            ),
            hasLightData =
                hasLightSensor &&
                    luxValues.isNotEmpty(),
            hasGyroscopeData =
                hasGyroscopeSensor
        )
    }

    private fun percentile(
        values: List<Float>,
        fraction: Float
    ): Float? {
        if (values.isEmpty()) return null

        val sorted = values.sorted()

        val index =
            (sorted.lastIndex * fraction)
                .toInt()
                .coerceIn(
                    0,
                    sorted.lastIndex
                )

        return sorted[index]
    }
}
