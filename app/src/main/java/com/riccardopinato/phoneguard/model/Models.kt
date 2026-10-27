package com.riccardopinato.phoneguard.model

import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.JourneyDuration
import com.riccardopinato.phoneguard.pocket.model.PocketSensitivity

enum class Sensitivity {
    LOW,
    MEDIUM,
    HIGH
}

enum class AlarmSoundProfile {
    SIREN,
    WARNING,
    BEEP
}

enum class ProtectionMode {
    TABLE,
    POCKET,
    BAG,
    CHARGING,
    JOURNEY;

    val label: String
        get() = when (this) {
            TABLE -> "Table"
            POCKET -> "Pocket"
            BAG -> "Bag"
            CHARGING -> "Charging"
            JOURNEY -> "Journey"
        }
}

data class AppSettings(
    val selectedMode: ProtectionMode = ProtectionMode.TABLE,
    val countdownSeconds: Int = 5,
    val preAlarmSeconds: Int = 5,
    val sensitivity: Sensitivity = Sensitivity.MEDIUM,
    val pocketSensitivity: PocketSensitivity = PocketSensitivity.NORMAL,
    val journeyDuration: JourneyDuration = JourneyDuration.HOUR_1,
    val journeyGuardMode: GuardMode = GuardMode.POCKET,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val flashEnabled: Boolean = true,
    val flashFrequencyHz: Float = 2.5f,
    val alarmVolumePercent: Int = 100,
    val warningScreenEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val chargeAutoArmEnabled: Boolean = false,
    val tableSoundProfile: AlarmSoundProfile = AlarmSoundProfile.SIREN,
    val pocketSoundProfile: AlarmSoundProfile = AlarmSoundProfile.SIREN,
    val bagSoundProfile: AlarmSoundProfile = AlarmSoundProfile.SIREN,
    val chargingSoundProfile: AlarmSoundProfile = AlarmSoundProfile.SIREN,
    val journeySoundProfile: AlarmSoundProfile = AlarmSoundProfile.SIREN,
    val onboardingCompleted: Boolean = false
) {
    fun soundProfileFor(
        mode: ProtectionMode
    ): AlarmSoundProfile =
        when (mode) {
            ProtectionMode.TABLE -> tableSoundProfile
            ProtectionMode.POCKET -> pocketSoundProfile
            ProtectionMode.BAG -> bagSoundProfile
            ProtectionMode.CHARGING -> chargingSoundProfile
            ProtectionMode.JOURNEY -> journeySoundProfile
        }
}

enum class AlarmEventType {
    ARMING_STARTED,
    ARMED,
    OWNER_VERIFIED,
    TRIGGERED,
    JOURNEY_ENDED,
    DISARMED,
    CANCELED,
    ERROR
}

data class AlarmEvent(
    val id: Long,
    val timestamp: Long,
    val type: AlarmEventType,
    val detail: String,
    val metadata: Map<String, String> = emptyMap()
)
