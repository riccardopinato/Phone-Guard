package com.riccardopinato.phoneguard.protection

import com.riccardopinato.phoneguard.model.AlarmSoundProfile
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.JourneyDuration
import com.riccardopinato.phoneguard.pocket.model.PocketSensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionPresetTest {

    @Test
    fun publicChargingPresetSelectsChargingGuard() {
        val spec =
            ProtectionPresetCatalog.spec(
                ProtectionPreset
                    .PUBLIC_CHARGING
            )

        assertEquals(
            ProtectionMode.CHARGING,
            spec.mode
        )

        assertEquals(
            2,
            spec.preAlarmSeconds
        )

        assertEquals(
            AlarmSoundProfile.SIREN,
            spec.soundProfile
        )
    }

    @Test
    fun commutePresetUsesHighPocketSensitivity() {
        val spec =
            ProtectionPresetCatalog.spec(
                ProtectionPreset.COMMUTE
            )

        assertEquals(
            ProtectionMode.POCKET,
            spec.mode
        )

        assertEquals(
            PocketSensitivity.HIGH,
            spec.pocketSensitivity
        )
    }

    @Test
    fun tripPresetIsTimeBoundedJourney() {
        val spec =
            ProtectionPresetCatalog.spec(
                ProtectionPreset.TRIP
            )

        assertEquals(
            ProtectionMode.JOURNEY,
            spec.mode
        )

        assertEquals(
            JourneyDuration.HOURS_2,
            spec.journeyDuration
        )
    }

    @Test
    fun everyPresetUsesSupportedGraceValue() {
        val supported =
            setOf(0, 2, 5, 8)

        ProtectionPreset.entries
            .forEach {
                assertTrue(
                    ProtectionPresetCatalog
                        .spec(it)
                        .preAlarmSeconds in
                        supported
                )
            }
    }
}
