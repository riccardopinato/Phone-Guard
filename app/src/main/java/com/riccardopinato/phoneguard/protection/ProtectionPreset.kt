package com.riccardopinato.phoneguard.protection

import com.riccardopinato.phoneguard.model.AlarmSoundProfile
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.model.Sensitivity
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.JourneyDuration
import com.riccardopinato.phoneguard.pocket.model.PocketSensitivity

enum class ProtectionPreset {
    CAFE,
    COMMUTE,
    BACKPACK,
    PUBLIC_CHARGING,
    TRIP
}

data class ProtectionPresetSpec(
    val mode: ProtectionMode,
    val countdownSeconds: Int,
    val preAlarmSeconds: Int,
    val tableSensitivity: Sensitivity = Sensitivity.MEDIUM,
    val pocketSensitivity: PocketSensitivity = PocketSensitivity.NORMAL,
    val journeyDuration: JourneyDuration = JourneyDuration.HOUR_1,
    val journeyGuardMode: GuardMode = GuardMode.POCKET,
    val soundProfile: AlarmSoundProfile = AlarmSoundProfile.SIREN
)

object ProtectionPresetCatalog {

    fun spec(
        preset: ProtectionPreset
    ): ProtectionPresetSpec =
        when (preset) {
            ProtectionPreset.CAFE ->
                ProtectionPresetSpec(
                    mode = ProtectionMode.TABLE,
                    countdownSeconds = 5,
                    preAlarmSeconds = 2,
                    tableSensitivity = Sensitivity.MEDIUM,
                    soundProfile = AlarmSoundProfile.SIREN
                )

            ProtectionPreset.COMMUTE ->
                ProtectionPresetSpec(
                    mode = ProtectionMode.POCKET,
                    countdownSeconds = 5,
                    preAlarmSeconds = 5,
                    pocketSensitivity = PocketSensitivity.HIGH,
                    soundProfile = AlarmSoundProfile.WARNING
                )

            ProtectionPreset.BACKPACK ->
                ProtectionPresetSpec(
                    mode = ProtectionMode.BAG,
                    countdownSeconds = 5,
                    preAlarmSeconds = 5,
                    pocketSensitivity = PocketSensitivity.NORMAL,
                    soundProfile = AlarmSoundProfile.SIREN
                )

            ProtectionPreset.PUBLIC_CHARGING ->
                ProtectionPresetSpec(
                    mode = ProtectionMode.CHARGING,
                    countdownSeconds = 3,
                    preAlarmSeconds = 2,
                    soundProfile = AlarmSoundProfile.SIREN
                )

            ProtectionPreset.TRIP ->
                ProtectionPresetSpec(
                    mode = ProtectionMode.JOURNEY,
                    countdownSeconds = 5,
                    preAlarmSeconds = 5,
                    pocketSensitivity = PocketSensitivity.NORMAL,
                    journeyDuration = JourneyDuration.HOURS_2,
                    journeyGuardMode = GuardMode.POCKET,
                    soundProfile = AlarmSoundProfile.WARNING
                )
        }
}
