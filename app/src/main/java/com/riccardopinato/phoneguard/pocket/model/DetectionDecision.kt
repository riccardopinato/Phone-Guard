package com.riccardopinato.phoneguard.pocket.model

data class DetectionDecision(
    val state: GuardState,
    val changed: Boolean,
    val confidenceScore: Int,
    val reason: String
)
