package com.riccardopinato.phoneguard.pocket.model

data class GuardSettings(
    val mode: GuardMode = GuardMode.POCKET,
    val armDelaySeconds: Int = 5,
    val sensitivity: PocketSensitivity = PocketSensitivity.NORMAL,
    val journeyDuration: JourneyDuration = JourneyDuration.UNTIL_STOPPED,
    val pocketStableMs: Long = 1_200L,
    val removalWindowMs: Long = 1_600L,
    val ownerGraceMs: Long = 2_500L,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val flashEnabled: Boolean = false,
    val biometricEnabled: Boolean = true,
    val enhancedReliability: Boolean = false
)
