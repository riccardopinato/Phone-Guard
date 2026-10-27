package com.riccardopinato.phoneguard.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.riccardopinato.phoneguard.model.AlarmOutputProfile
import com.riccardopinato.phoneguard.model.AppSettings
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.model.ProtectionPreset
import com.riccardopinato.phoneguard.model.ProtectionPresetEngine
import com.riccardopinato.phoneguard.model.Sensitivity
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.JourneyDuration
import com.riccardopinato.phoneguard.pocket.model.PocketSensitivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.settingsDataStore by preferencesDataStore(
    name = "phone_guard_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val selectedMode = stringPreferencesKey("selected_mode")
        val countdown = intPreferencesKey("countdown_seconds")
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
        val onboardingCompleted =
            booleanPreferencesKey("onboarding_completed")
        val gracePeriod =
            intPreferencesKey("grace_period_seconds")
        val autoArmCharging =
            booleanPreferencesKey("auto_arm_charging")

        fun alarmProfile(
            mode: ProtectionMode
        ) =
            stringPreferencesKey(
                "alarm_profile_${mode.name.lowercase()}"
            )
    }

    val settings: Flow<AppSettings> =
        context.settingsDataStore.data.map { prefs ->
            val legacyProfile =
                AlarmOutputProfile(
                    soundEnabled =
                        prefs[Keys.sound] ?: true,
                    vibrationEnabled =
                        prefs[Keys.vibration] ?: true,
                    flashEnabled =
                        prefs[Keys.flash] ?: true,
                    flashFrequencyHz =
                        (prefs[Keys.flashFrequency] ?: 2.5f)
                            .coerceIn(0.5f, 3f),
                    alarmVolumePercent =
                        (prefs[Keys.alarmVolume] ?: 100)
                            .coerceIn(0, 100),
                    warningScreenEnabled =
                        prefs[Keys.warningScreen] ?: true
                )

            val profiles =
                ProtectionMode.entries
                    .associateWith { mode ->
                        decodeAlarmProfile(
                            prefs[
                                Keys.alarmProfile(
                                    mode
                                )
                            ]
                        ) ?: legacyProfile
                    }

            AppSettings(
                selectedMode = enumOrDefault(
                    prefs[Keys.selectedMode],
                    ProtectionMode.TABLE
                ),
                countdownSeconds =
                    prefs[Keys.countdown] ?: 5,
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
                soundEnabled =
                    legacyProfile.soundEnabled,
                vibrationEnabled =
                    legacyProfile.vibrationEnabled,
                flashEnabled =
                    legacyProfile.flashEnabled,
                flashFrequencyHz =
                    legacyProfile.flashFrequencyHz,
                alarmVolumePercent =
                    legacyProfile.alarmVolumePercent,
                warningScreenEnabled =
                    legacyProfile.warningScreenEnabled,
                biometricEnabled =
                    prefs[Keys.biometric] ?: false,
                onboardingCompleted =
                    prefs[Keys.onboardingCompleted] ?: false,
                gracePeriodSeconds =
                    (prefs[Keys.gracePeriod] ?: 5)
                        .takeIf {
                            it in
                                ALLOWED_GRACE_SECONDS
                        }
                        ?: 5,
                autoArmCharging =
                    prefs[Keys.autoArmCharging] ?: false,
                alarmProfiles =
                    profiles
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

    suspend fun setGracePeriodSeconds(
        seconds: Int
    ) {
        require(
            seconds in
                ALLOWED_GRACE_SECONDS
        )

        context.settingsDataStore.edit {
            it[Keys.gracePeriod] =
                seconds
        }
    }

    suspend fun setChargingAutoArm(
        enabled: Boolean
    ) {
        context.settingsDataStore.edit {
            it[Keys.autoArmCharging] =
                enabled
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

    suspend fun setSoundEnabled(
        mode: ProtectionMode,
        enabled: Boolean
    ) {
        updateAlarmProfile(mode) {
            copy(soundEnabled = enabled)
        }
    }

    suspend fun setVibrationEnabled(
        mode: ProtectionMode,
        enabled: Boolean
    ) {
        updateAlarmProfile(mode) {
            copy(vibrationEnabled = enabled)
        }
    }

    suspend fun setFlashEnabled(
        mode: ProtectionMode,
        enabled: Boolean
    ) {
        updateAlarmProfile(mode) {
            copy(flashEnabled = enabled)
        }
    }

    suspend fun setFlashFrequencyHz(
        mode: ProtectionMode,
        value: Float
    ) {
        updateAlarmProfile(mode) {
            copy(
                flashFrequencyHz =
                    value.coerceIn(
                        0.5f,
                        3f
                    )
            )
        }
    }

    suspend fun setAlarmVolumePercent(
        mode: ProtectionMode,
        value: Int
    ) {
        updateAlarmProfile(mode) {
            copy(
                alarmVolumePercent =
                    value.coerceIn(
                        0,
                        100
                    )
            )
        }
    }

    suspend fun setWarningScreenEnabled(
        mode: ProtectionMode,
        enabled: Boolean
    ) {
        updateAlarmProfile(mode) {
            copy(
                warningScreenEnabled =
                    enabled
            )
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

    suspend fun applyPreset(
        preset: ProtectionPreset
    ) {
        val config =
            ProtectionPresetEngine
                .configuration(
                    preset
                )

        context.settingsDataStore.edit {
            prefs ->
            prefs[Keys.selectedMode] =
                config.mode.name
            prefs[Keys.countdown] =
                config.countdownSeconds
            prefs[Keys.gracePeriod] =
                config.gracePeriodSeconds
            prefs[Keys.sensitivity] =
                config.tableSensitivity.name
            prefs[Keys.pocketSensitivity] =
                config.pocketSensitivity.name
            prefs[Keys.journeyDuration] =
                config.journeyDuration.name
            prefs[Keys.journeyGuardMode] =
                config.journeyGuardMode.name
            prefs[
                Keys.alarmProfile(
                    config.mode
                )
            ] =
                encodeAlarmProfile(
                    config.alarmProfile
                )
        }
    }

    private suspend fun updateAlarmProfile(
        mode: ProtectionMode,
        transform:
            AlarmOutputProfile.() ->
                AlarmOutputProfile
    ) {
        context.settingsDataStore.edit {
            prefs ->

            val legacyProfile =
                AlarmOutputProfile(
                    soundEnabled =
                        prefs[Keys.sound] ?: true,
                    vibrationEnabled =
                        prefs[Keys.vibration] ?: true,
                    flashEnabled =
                        prefs[Keys.flash] ?: true,
                    flashFrequencyHz =
                        (prefs[Keys.flashFrequency] ?: 2.5f)
                            .coerceIn(0.5f, 3f),
                    alarmVolumePercent =
                        (prefs[Keys.alarmVolume] ?: 100)
                            .coerceIn(0, 100),
                    warningScreenEnabled =
                        prefs[Keys.warningScreen] ?: true
                )

            val current =
                (
                    decodeAlarmProfile(
                        prefs[
                            Keys.alarmProfile(
                                mode
                            )
                        ]
                    )
                        ?: legacyProfile
                    )
                    .transform()

            prefs[
                Keys.alarmProfile(
                    mode
                )
            ] =
                encodeAlarmProfile(
                    current
                )
        }
    }

    private fun encodeAlarmProfile(
        profile: AlarmOutputProfile
    ): String =
        JSONObject()
            .put("version", 1)
            .put(
                "sound",
                profile.soundEnabled
            )
            .put(
                "vibration",
                profile.vibrationEnabled
            )
            .put(
                "flash",
                profile.flashEnabled
            )
            .put(
                "flashHz",
                profile.flashFrequencyHz
            )
            .put(
                "volume",
                profile.alarmVolumePercent
            )
            .put(
                "warning",
                profile.warningScreenEnabled
            )
            .toString()

    private fun decodeAlarmProfile(
        raw: String?
    ): AlarmOutputProfile? {
        if (raw.isNullOrBlank()) {
            return null
        }

        return runCatching {
            val json =
                JSONObject(raw)

            AlarmOutputProfile(
                soundEnabled =
                    json.optBoolean(
                        "sound",
                        true
                    ),
                vibrationEnabled =
                    json.optBoolean(
                        "vibration",
                        true
                    ),
                flashEnabled =
                    json.optBoolean(
                        "flash",
                        true
                    ),
                flashFrequencyHz =
                    json.optDouble(
                        "flashHz",
                        2.5
                    )
                        .toFloat()
                        .coerceIn(
                            0.5f,
                            3f
                        ),
                alarmVolumePercent =
                    json.optInt(
                        "volume",
                        100
                    )
                        .coerceIn(
                            0,
                            100
                        ),
                warningScreenEnabled =
                    json.optBoolean(
                        "warning",
                        true
                    )
            )
        }.getOrNull()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(
        raw: String?,
        fallback: T
    ): T =
        runCatching {
            enumValueOf<T>(raw ?: fallback.name)
        }.getOrDefault(fallback)

    private companion object {
        val ALLOWED_GRACE_SECONDS =
            setOf(
                0,
                2,
                5,
                8
            )
    }
}
