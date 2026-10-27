package com.riccardopinato.phoneguard.pocket.model

data class SensorSnapshot(
    val lux: Float?,
    val proximityNear: Boolean?,
    val motionScore: Float,
    val rotationScore: Float,
    val timestampMs: Long,
    val accelerometerObserved: Boolean = true,
    val proximityObserved: Boolean = true,
    val lightObserved: Boolean = true,
    val gyroscopeObserved: Boolean = true
)
