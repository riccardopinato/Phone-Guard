package com.riccardopinato.phoneguard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riccardopinato.phoneguard.data.SettingsRepository
import com.riccardopinato.phoneguard.model.AlarmDuration
import com.riccardopinato.phoneguard.model.PreAlarmStyle
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.model.ProtectionPreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryInstrumentedTest {

    @Test
    fun flashFrequencyIsClampedToAccessibilityCeiling() =
        runBlocking {
            val context =
                ApplicationProvider
                    .getApplicationContext<Context>()

            val repository =
                SettingsRepository(
                    context
                )

            repository
                .setFlashFrequencyHz(
                    10f
                )

            assertEquals(
                3f,
                repository
                    .settings
                    .first()
                    .flashFrequencyHz,
                0.001f
            )
        }


    @Test
    fun modeSpecificProfilePersistsIndependently() =
        runBlocking {
            val context =
                ApplicationProvider
                    .getApplicationContext<Context>()

            val repository =
                SettingsRepository(
                    context
                )

            repository
                .setAlarmVolumePercent(
                    ProtectionMode.CHARGING,
                    42
                )

            repository
                .setAlarmVolumePercent(
                    ProtectionMode.TABLE,
                    88
                )

            val settings =
                repository
                    .settings
                    .first()

            assertEquals(
                42,
                settings
                    .alarmProfile(
                        ProtectionMode.CHARGING
                    )
                    .alarmVolumePercent
            )

            assertEquals(
                88,
                settings
                    .alarmProfile(
                        ProtectionMode.TABLE
                    )
                    .alarmVolumePercent
            )
        }

    @Test
    fun advancedAccessoryAlarmProfilePersists() =
        runBlocking {
            val context =
                ApplicationProvider
                    .getApplicationContext<Context>()

            val repository =
                SettingsRepository(
                    context
                )

            repository
                .setCustomSoundUri(
                    ProtectionMode.ACCESSORY,
                    "content://phoneguard/test-audio"
                )

            repository
                .setPreAlarmStyle(
                    ProtectionMode.ACCESSORY,
                    PreAlarmStyle.BEEP
                )

            repository
                .setAlarmDuration(
                    ProtectionMode.ACCESSORY,
                    AlarmDuration.MINUTE_1
                )

            val profile =
                repository
                    .settings
                    .first()
                    .alarmProfile(
                        ProtectionMode.ACCESSORY
                    )

            assertEquals(
                "content://phoneguard/test-audio",
                profile.customSoundUri
            )
            assertEquals(
                PreAlarmStyle.BEEP,
                profile.preAlarmStyle
            )
            assertEquals(
                AlarmDuration.MINUTE_1,
                profile.alarmDuration
            )
        }

    @Test
    fun publicChargePresetSelectsChargingAndGrace() =
        runBlocking {
            val context =
                ApplicationProvider
                    .getApplicationContext<Context>()

            val repository =
                SettingsRepository(
                    context
                )

            repository.applyPreset(
                ProtectionPreset.PUBLIC_CHARGE
            )

            val settings =
                repository
                    .settings
                    .first()

            assertEquals(
                ProtectionMode.CHARGING,
                settings.selectedMode
            )
            assertEquals(
                2,
                settings.gracePeriodSeconds
            )
        }


    @Test
    fun concurrentModeProfileUpdatesPreserveIndependentFields() =
        runBlocking {
            val context =
                ApplicationProvider
                    .getApplicationContext<Context>()

            val repository =
                SettingsRepository(
                    context
                )

            repository
                .setSoundEnabled(
                    ProtectionMode.CHARGING,
                    true
                )
            repository
                .setVibrationEnabled(
                    ProtectionMode.CHARGING,
                    true
                )

            coroutineScope {
                launch {
                    repository
                        .setSoundEnabled(
                            ProtectionMode.CHARGING,
                            false
                        )
                }

                launch {
                    repository
                        .setVibrationEnabled(
                            ProtectionMode.CHARGING,
                            false
                        )
                }
            }

            val profile =
                repository
                    .settings
                    .first()
                    .alarmProfile(
                        ProtectionMode.CHARGING
                    )

            assertFalse(
                profile.soundEnabled
            )
            assertFalse(
                profile.vibrationEnabled
            )
        }
    @Test
    fun adaptiveAutoGuardSettingsPersistAndStayOptIn() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SettingsRepository(context)
        val before = repository.settings.first()
        try {
            repository.setAdaptiveAutoGuardEnabled(false)
            repository.setAdaptiveAutoGuardIdleMinutes(5)
            repository.setAdaptiveAutoGuardPocket(true)
            repository.setAdaptiveAutoGuardTable(false)
            repository.setAdaptiveAutoGuardMobility(false)
            val disabled = repository.settings.first()
            assertFalse(disabled.adaptiveAutoGuardEnabled)
            assertEquals(5, disabled.adaptiveAutoGuardIdleMinutes)
            assertFalse(disabled.adaptiveAutoGuardTable)
            assertFalse(disabled.adaptiveAutoGuardMobility)

            repository.setAdaptiveAutoGuardEnabled(true)
            val enabled = repository.settings.first()
            org.junit.Assert.assertTrue(enabled.adaptiveAutoGuardEnabled)
        } finally {
            repository.setAdaptiveAutoGuardEnabled(before.adaptiveAutoGuardEnabled)
            repository.setAdaptiveAutoGuardIdleMinutes(before.adaptiveAutoGuardIdleMinutes)
            repository.setAdaptiveAutoGuardPocket(before.adaptiveAutoGuardPocket)
            repository.setAdaptiveAutoGuardTable(before.adaptiveAutoGuardTable)
            repository.setAdaptiveAutoGuardMobility(before.adaptiveAutoGuardMobility)
        }
    }

}
