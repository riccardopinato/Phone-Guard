package com.riccardopinato.phoneguard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riccardopinato.phoneguard.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
}
