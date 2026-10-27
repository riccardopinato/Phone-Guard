package com.riccardopinato.phoneguard.pocket.model

enum class PocketSensitivity(
    val suspectScore: Int,
    val ownerCheckScore: Int,
    val motionMultiplier: Float
) {
    LOW(55, 80, 1.30f),
    NORMAL(45, 70, 1.00f),
    HIGH(35, 60, 0.80f)
}
