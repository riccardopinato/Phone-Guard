package com.riccardopinato.phoneguard.model

import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.JourneyDuration
import com.riccardopinato.phoneguard.pocket.model.PocketSensitivity

enum class ProtectionPreset {
    CAFE,
    TRANSIT,
    BACKPACK,
    PUBLIC_CHARGE,
    TRAVEL
}

data class ProtectionPresetConfiguration(
    val mode: ProtectionMode,
    val countdownSeconds: Int,
    val gracePeriodSeconds: Int,
    val tableSensitivity: Sensitivity,
    val pocketSensitivity: PocketSensitivity,
    val journeyDuration: JourneyDuration,
    val journeyGuardMode: GuardMode,
    val alarmProfile: AlarmOutputProfile
)

object ProtectionPresetEngine {
    fun configuration(
        preset: ProtectionPreset
    ): ProtectionPresetConfiguration =
        when (preset) {
            ProtectionPreset.CAFE ->
                ProtectionPresetConfiguration(
                    mode = ProtectionMode.TABLE,
                    countdownSeconds = 5,
                    gracePeriodSeconds = 2,
                    tableSensitivity = Sensitivity.MEDIUM,
                    pocketSensitivity = PocketSensitivity.NORMAL,
                    journeyDuration = JourneyDuration.HOUR_1,
                    journeyGuardMode = GuardMode.POCKET,
                    alarmProfile = AlarmOutputProfile()
                )

            ProtectionPreset.TRANSIT ->
                ProtectionPresetConfiguration(
                    mode = ProtectionMode.POCKET,
                    countdownSeconds = 3,
                    gracePeriodSeconds = 5,
                    tableSensitivity = Sensitivity.MEDIUM,
                    pocketSensitivity = PocketSensitivity.HIGH,
                    journeyDuration = JourneyDuration.HOUR_1,
                    journeyGuardMode = GuardMode.POCKET,
                    alarmProfile = AlarmOutputProfile()
                )

            ProtectionPreset.BACKPACK ->
                ProtectionPresetConfiguration(
                    mode = ProtectionMode.BAG,
                    countdownSeconds = 5,
                    gracePeriodSeconds = 5,
                    tableSensitivity = Sensitivity.MEDIUM,
                    pocketSensitivity = PocketSensitivity.NORMAL,
                    journeyDuration = JourneyDuration.HOUR_1,
                    journeyGuardMode = GuardMode.BAG,
                    alarmProfile = AlarmOutputProfile()
                )

            ProtectionPreset.PUBLIC_CHARGE ->
                ProtectionPresetConfiguration(
                    mode = ProtectionMode.CHARGING,
                    countdownSeconds = 3,
                    gracePeriodSeconds = 2,
                    tableSensitivity = Sensitivity.MEDIUM,
                    pocketSensitivity = PocketSensitivity.NORMAL,
                    journeyDuration = JourneyDuration.HOUR_1,
                    journeyGuardMode = GuardMode.POCKET,
                    alarmProfile = AlarmOutputProfile()
                )

            ProtectionPreset.TRAVEL ->
                ProtectionPresetConfiguration(
                    mode = ProtectionMode.JOURNEY,
                    countdownSeconds = 5,
                    gracePeriodSeconds = 5,
                    tableSensitivity = Sensitivity.MEDIUM,
                    pocketSensitivity = PocketSensitivity.NORMAL,
                    journeyDuration = JourneyDuration.HOURS_2,
                    journeyGuardMode = GuardMode.POCKET,
                    alarmProfile = AlarmOutputProfile()
                )
        }
}
