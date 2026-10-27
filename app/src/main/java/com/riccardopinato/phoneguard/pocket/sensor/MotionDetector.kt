package com.riccardopinato.phoneguard.pocket.sensor

import kotlin.math.abs
import kotlin.math.sqrt

class MotionDetector(
    private val windowMs: Long = 700L
) {
    private data class Sample(val time: Long, val value: Float)
    private val samples = ArrayDeque<Sample>()

    fun update(
        x: Float,
        y: Float,
        z: Float,
        timestampMs: Long
    ): Float {
        val magnitude = sqrt(x * x + y * y + z * z)
        val deviation =
            (abs(magnitude - GRAVITY_EARTH) / GRAVITY_EARTH)
                .coerceIn(0f, 3f)

        samples.addLast(Sample(timestampMs, deviation))
        trim(timestampMs)
        return current(timestampMs)
    }

    fun current(now: Long): Float {
        trim(now)
        return samples.maxOfOrNull { it.value } ?: 0f
    }

    fun reset() = samples.clear()

    private fun trim(now: Long) {
        while (
            samples.isNotEmpty() &&
            now - samples.first().time > windowMs
        ) {
            samples.removeFirst()
        }
    }

    private companion object {
        const val GRAVITY_EARTH = 9.80665f
    }
}
