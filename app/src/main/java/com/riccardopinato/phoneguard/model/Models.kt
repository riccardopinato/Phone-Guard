package com.riccardopinato.phoneguard.model

enum class Sensitivity {
    LOW,
    MEDIUM,
    HIGH;

    val label: String
        get() = when (this) {
            LOW -> "Bassa"
            MEDIUM -> "Media"
            HIGH -> "Alta"
        }
}

data class AppSettings(
    val countdownSeconds: Int = 5,
    val sensitivity: Sensitivity = Sensitivity.MEDIUM,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val flashEnabled: Boolean = true,
    val flashFrequencyHz: Float = 2.5f,
    val alarmVolumePercent: Int = 100,
    val warningScreenEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false
)

enum class AlarmEventType {
    ARMING_STARTED,
    ARMED,
    TRIGGERED,
    DISARMED,
    CANCELED,
    ERROR
}

data class AlarmEvent(
    val id: Long,
    val timestamp: Long,
    val type: AlarmEventType,
    val detail: String
)
