package com.riccardopinato.phoneguard.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.riccardopinato.phoneguard.model.AppSettings
import com.riccardopinato.phoneguard.model.Sensitivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(
    name = "phone_guard_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val countdown = intPreferencesKey("countdown_seconds")
        val sensitivity = stringPreferencesKey("sensitivity")
        val sound = booleanPreferencesKey("sound_enabled")
        val vibration = booleanPreferencesKey("vibration_enabled")
        val flash = booleanPreferencesKey("flash_enabled")
        val flashFrequency = floatPreferencesKey("flash_frequency_hz")
        val alarmVolume = intPreferencesKey("alarm_volume_percent")
        val warningScreen = booleanPreferencesKey("warning_screen_enabled")
        val biometric = booleanPreferencesKey("biometric_enabled")
        val onboardingCompleted =
            booleanPreferencesKey("onboarding_completed")
    }

    val settings: Flow<AppSettings> =
        context.settingsDataStore.data.map { prefs ->
            AppSettings(
                countdownSeconds = prefs[Keys.countdown] ?: 5,
                sensitivity = runCatching {
                    Sensitivity.valueOf(
                        prefs[Keys.sensitivity]
                            ?: Sensitivity.MEDIUM.name
                    )
                }.getOrDefault(Sensitivity.MEDIUM),
                soundEnabled = prefs[Keys.sound] ?: true,
                vibrationEnabled = prefs[Keys.vibration] ?: true,
                flashEnabled = prefs[Keys.flash] ?: true,
                flashFrequencyHz =
                    (prefs[Keys.flashFrequency] ?: 2.5f)
                        .coerceIn(0.5f, 10f),
                alarmVolumePercent =
                    (prefs[Keys.alarmVolume] ?: 100)
                        .coerceIn(0, 100),
                warningScreenEnabled =
                    prefs[Keys.warningScreen] ?: true,
                biometricEnabled = prefs[Keys.biometric] ?: false,
                onboardingCompleted =
                    prefs[Keys.onboardingCompleted] ?: false
            )
        }

    suspend fun setCountdown(seconds: Int) {
        require(seconds in setOf(3, 5, 10, 15))
        context.settingsDataStore.edit {
            it[Keys.countdown] = seconds
        }
    }

    suspend fun setSensitivity(value: Sensitivity) {
        context.settingsDataStore.edit {
            it[Keys.sensitivity] = value.name
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
                value.coerceIn(0.5f, 10f)
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

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.onboardingCompleted] = completed
        }
    }
}
