package com.riccardopinato.phoneguard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riccardopinato.phoneguard.data.SettingsRepository
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
}
