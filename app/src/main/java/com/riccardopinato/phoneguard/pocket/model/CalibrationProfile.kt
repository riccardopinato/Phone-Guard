package com.riccardopinato.phoneguard.pocket.model

data class CalibrationProfile(
    val mode: GuardMode,
    val createdAt: Long,
    val enterLux: Float,
    val exitLux: Float,
    val motionNoise: Float,
    val rotationNoise: Float,
    val sampleCount: Int,
    val hasLightData: Boolean,
    val hasGyroscopeData: Boolean
) {
    val motionThreshold: Float
        get() = (motionNoise + 0.12f).coerceIn(0.10f, 0.50f)

    val rotationThreshold: Float
        get() = (rotationNoise + 0.25f).coerceIn(0.20f, 1.20f)
}
