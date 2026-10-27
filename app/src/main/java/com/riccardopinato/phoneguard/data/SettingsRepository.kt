package com.riccardopinato.phoneguard.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.riccardopinato.phoneguard.model.AlarmSoundProfile
import com.riccardopinato.phoneguard.model.AppSettings
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.model.Sensitivity
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.JourneyDuration
import com.riccardopinato.phoneguard.pocket.model.PocketSensitivity
import com.riccardopinato.phoneguard.protection.ProtectionPreset
import com.riccardopinato.phoneguard.protection.ProtectionPresetCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(
    name = "phone_guard_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val selectedMode = stringPreferencesKey("selected_mode")
        val countdown = intPreferencesKey("countdown_seconds")
        val preAlarmSeconds = intPreferencesKey("pre_alarm_seconds")
        val sensitivity = stringPreferencesKey("sensitivity")
        val pocketSensitivity = stringPreferencesKey("pocket_sensitivity")
        val journeyDuration = stringPreferencesKey("journey_duration")
        val journeyGuardMode = stringPreferencesKey("journey_guard_mode")
        val sound = booleanPreferencesKey("sound_enabled")
        val vibration = booleanPreferencesKey("vibration_enabled")
        val flash = booleanPreferencesKey("flash_enabled")
        val flashFrequency = floatPreferencesKey("flash_frequency_hz")
        val alarmVolume = intPreferencesKey("alarm_volume_percent")
        val warningScreen = booleanPreferencesKey("warning_screen_enabled")
        val biometric = booleanPreferencesKey("biometric_enabled")
        val chargeAutoArm = booleanPreferencesKey("charge_auto_arm_enabled")
        val tableSoundProfile = stringPreferencesKey("table_sound_profile")
        val pocketSoundProfile = stringPreferencesKey("pocket_sound_profile")
        val bagSoundProfile = stringPreferencesKey("bag_sound_profile")
        val chargingSoundProfile = stringPreferencesKey("charging_sound_profile")
        val journeySoundProfile = stringPreferencesKey("journey_sound_profile")
        val onboardingCompleted =
            booleanPreferencesKey("onboarding_completed")
    }

    val settings: Flow<AppSettings> =
        context.settingsDataStore.data.map { prefs ->
            AppSettings(
                selectedMode = enumOrDefault(
                    prefs[Keys.selectedMode],
                    ProtectionMode.TABLE
                ),
                countdownSeconds = prefs[Keys.countdown] ?: 5,
                preAlarmSeconds =
                    (prefs[Keys.preAlarmSeconds] ?: 5)
                        .coerceIn(0, 8),
                sensitivity = enumOrDefault(
                    prefs[Keys.sensitivity],
                    Sensitivity.MEDIUM
                ),
                pocketSensitivity = enumOrDefault(
                    prefs[Keys.pocketSensitivity],
                    PocketSensitivity.NORMAL
                ),
                journeyDuration = enumOrDefault(
                    prefs[Keys.journeyDuration],
                    JourneyDuration.HOUR_1
                ),
                journeyGuardMode = enumOrDefault(
                    prefs[Keys.journeyGuardMode],
                    GuardMode.POCKET
                ),
                soundEnabled = prefs[Keys.sound] ?: true,
                vibrationEnabled = prefs[Keys.vibration] ?: true,
                flashEnabled = prefs[Keys.flash] ?: true,
                flashFrequencyHz =
                    (prefs[Keys.flashFrequency] ?: 2.5f)
                        .coerceIn(0.5f, 3f),
                alarmVolumePercent =
                    (prefs[Keys.alarmVolume] ?: 100)
                        .coerceIn(0, 100),
                warningScreenEnabled =
                    prefs[Keys.warningScreen] ?: true,
                biometricEnabled = prefs[Keys.biometric] ?: false,
                chargeAutoArmEnabled =
                    prefs[Keys.chargeAutoArm] ?: false,
                tableSoundProfile = enumOrDefault(
                    prefs[Keys.tableSoundProfile],
                    AlarmSoundProfile.SIREN
                ),
                pocketSoundProfile = enumOrDefault(
                    prefs[Keys.pocketSoundProfile],
                    AlarmSoundProfile.SIREN
                ),
                bagSoundProfile = enumOrDefault(
                    prefs[Keys.bagSoundProfile],
                    AlarmSoundProfile.SIREN
                ),
                chargingSoundProfile = enumOrDefault(
                    prefs[Keys.chargingSoundProfile],
                    AlarmSoundProfile.SIREN
                ),
                journeySoundProfile = enumOrDefault(
                    prefs[Keys.journeySoundProfile],
                    AlarmSoundProfile.SIREN
                ),
                onboardingCompleted =
                    prefs[Keys.onboardingCompleted] ?: false
            )
        }

    suspend fun setSelectedMode(value: ProtectionMode) {
        context.settingsDataStore.edit {
            it[Keys.selectedMode] = value.name
        }
    }

    suspend fun setCountdown(seconds: Int) {
        require(seconds in setOf(3, 5, 10, 15))
        context.settingsDataStore.edit {
            it[Keys.countdown] = seconds
        }
    }

    suspend fun setPreAlarmSeconds(seconds: Int) {
        require(seconds in setOf(0, 2, 5, 8))
        context.settingsDataStore.edit {
            it[Keys.preAlarmSeconds] = seconds
        }
    }

    suspend fun setSensitivity(value: Sensitivity) {
        context.settingsDataStore.edit {
            it[Keys.sensitivity] = value.name
        }
    }

    suspend fun setPocketSensitivity(value: PocketSensitivity) {
        context.settingsDataStore.edit {
            it[Keys.pocketSensitivity] = value.name
        }
    }

    suspend fun setJourneyDuration(value: JourneyDuration) {
        context.settingsDataStore.edit {
            it[Keys.journeyDuration] = value.name
        }
    }

    suspend fun setJourneyGuardMode(value: GuardMode) {
        context.settingsDataStore.edit {
            it[Keys.journeyGuardMode] = value.name
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.sound] = enabled
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.vibration] = enabled
        }
    }

    suspend fun setFlashEnabled(enabled: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.flash] = enabled
        }
    }

    suspend fun setFlashFrequencyHz(value: Float) {
        context.settingsDataStore.edit {
            it[Keys.flashFrequency] =
                value.coerceIn(0.5f, 3f)
        }
    }

    suspend fun setAlarmVolumePercent(value: Int) {
        context.settingsDataStore.edit {
            it[Keys.alarmVolume] =
                value.coerceIn(0, 100)
        }
    }

    suspend fun setWarningScreenEnabled(enabled: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.warningScreen] = enabled
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.biometric] = enabled
        }
    }

    suspend fun setChargeAutoArmEnabled(enabled: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.chargeAutoArm] = enabled
        }
    }

    suspend fun setAlarmSoundProfile(
        mode: ProtectionMode,
        profile: AlarmSoundProfile
    ) {
        context.settingsDataStore.edit {
            prefs ->
            prefs[
                when (mode) {
                    ProtectionMode.TABLE -> Keys.tableSoundProfile
                    ProtectionMode.POCKET -> Keys.pocketSoundProfile
                    ProtectionMode.BAG -> Keys.bagSoundProfile
                    ProtectionMode.CHARGING -> Keys.chargingSoundProfile
                    ProtectionMode.JOURNEY -> Keys.journeySoundProfile
                }
            ] = profile.name
        }
    }

    suspend fun applyPreset(
        preset: ProtectionPreset
    ) {
        val spec =
            ProtectionPresetCatalog.spec(
                preset
            )

        context.settingsDataStore.edit {
            prefs ->
            prefs[Keys.selectedMode] =
                spec.mode.name
            prefs[Keys.countdown] =
                spec.countdownSeconds
            prefs[Keys.preAlarmSeconds] =
                spec.preAlarmSeconds
            prefs[Keys.sensitivity] =
                spec.tableSensitivity.name
            prefs[Keys.pocketSensitivity] =
                spec.pocketSensitivity.name
            prefs[Keys.journeyDuration] =
                spec.journeyDuration.name
            prefs[Keys.journeyGuardMode] =
                spec.journeyGuardMode.name
            prefs[
                when (spec.mode) {
                    ProtectionMode.TABLE -> Keys.tableSoundProfile
                    ProtectionMode.POCKET -> Keys.pocketSoundProfile
                    ProtectionMode.BAG -> Keys.bagSoundProfile
                    ProtectionMode.CHARGING -> Keys.chargingSoundProfile
                    ProtectionMode.JOURNEY -> Keys.journeySoundProfile
                }
            ] = spec.soundProfile.name
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.onboardingCompleted] = completed
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(
        raw: String?,
        fallback: T
    ): T =
        runCatching {
            enumValueOf<T>(raw ?: fallback.name)
        }.getOrDefault(fallback)
}
