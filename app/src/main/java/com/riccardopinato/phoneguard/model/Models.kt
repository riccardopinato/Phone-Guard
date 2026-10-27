package com.riccardopinato.phoneguard.model

import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.JourneyDuration
import com.riccardopinato.phoneguard.pocket.model.PocketSensitivity

enum class Sensitivity {
    LOW,
    MEDIUM,
    HIGH
}

enum class ProtectionMode {
    TABLE,
    POCKET,
    BAG,
    CHARGING,
    ACCESSORY,
    JOURNEY;

    val label: String
        get() = when (this) {
            TABLE -> "Table"
            POCKET -> "Pocket"
            BAG -> "Bag"
            CHARGING -> "Charging"
            ACCESSORY -> "Accessory"
            JOURNEY -> "Journey"
        }
}

enum class PreAlarmStyle {
    SILENT,
    VIBRATION,
    BEEP
}

enum class AlarmDuration(
    val durationMs: Long?
) {
    SECONDS_30(30_000L),
    MINUTE_1(60_000L),
    MINUTES_3(180_000L),
    UNTIL_DISARMED(null)
}

data class AlarmOutputProfile(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val flashEnabled: Boolean = true,
    val flashFrequencyHz: Float = 2.5f,
    val alarmVolumePercent: Int = 100,
    val warningScreenEnabled: Boolean = true,
    val customSoundUri: String? = null,
    val preAlarmStyle: PreAlarmStyle = PreAlarmStyle.VIBRATION,
    val alarmDuration: AlarmDuration = AlarmDuration.UNTIL_DISARMED
)

data class AppSettings(
    val selectedMode: ProtectionMode = ProtectionMode.TABLE,
    val countdownSeconds: Int = 5,
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
    val onboardingCompleted: Boolean = false,
    val gracePeriodSeconds: Int = 5,
    val autoArmCharging: Boolean = false,
    val adaptiveAutoGuardEnabled: Boolean = false,
    val adaptiveAutoGuardIdleMinutes: Int = 3,
    val adaptiveAutoGuardPocket: Boolean = true,
    val adaptiveAutoGuardTable: Boolean = true,
    val alarmProfiles:
        Map<ProtectionMode, AlarmOutputProfile> =
        emptyMap()
) {
    fun alarmProfile(
        mode: ProtectionMode
    ): AlarmOutputProfile =
        alarmProfiles[mode]
            ?: AlarmOutputProfile(
                soundEnabled = soundEnabled,
                vibrationEnabled = vibrationEnabled,
                flashEnabled = flashEnabled,
                flashFrequencyHz = flashFrequencyHz,
                alarmVolumePercent = alarmVolumePercent,
                warningScreenEnabled = warningScreenEnabled
            )
}

enum class AlarmEventType {
    ARMING_STARTED,
    ARMED,
    OWNER_VERIFIED,
    TRIGGERED,
    PRE_ALARM_STARTED,
    ALARM_OUTPUTS_TIMED_OUT,
    JOURNEY_ENDED,
    DISARMED,
    CANCELED,
    ERROR
}

data class AlarmEventEvidence(
    val mode: ProtectionMode? = null,
    val source: String? = null,
    val reason: String? = null,
    val confidenceScore: Int? = null,
    val detectorState: String? = null,
    val sensorSummary: String? = null,
    val outputSummary: String? = null,
    val graceSeconds: Int? = null
)

data class AlarmEvent(
    val id: Long,
    val timestamp: Long,
    val type: AlarmEventType,
    val detail: String,
    val evidence: AlarmEventEvidence? = null
)
