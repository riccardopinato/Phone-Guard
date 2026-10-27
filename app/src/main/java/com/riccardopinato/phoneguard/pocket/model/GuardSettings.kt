package com.riccardopinato.phoneguard.pocket.model

data class GuardSettings(
    val mode: GuardMode = GuardMode.POCKET,
    val sensitivity: PocketSensitivity = PocketSensitivity.NORMAL,
    val pocketStableMs: Long = 1_200L,
    val removalWindowMs: Long = 1_600L
)
