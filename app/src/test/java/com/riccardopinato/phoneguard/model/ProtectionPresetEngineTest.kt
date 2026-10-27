package com.riccardopinato.phoneguard.model

import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.JourneyDuration
import com.riccardopinato.phoneguard.pocket.model.PocketSensitivity
import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectionPresetEngineTest {

    @Test
    fun publicChargeUsesChargingWithShortGrace() {
        val config =
            ProtectionPresetEngine
                .configuration(
                    ProtectionPreset.PUBLIC_CHARGE
                )

        assertEquals(
            ProtectionMode.CHARGING,
            config.mode
        )
        assertEquals(
            2,
            config.gracePeriodSeconds
        )
    }

    @Test
    fun transitUsesHighPocketSensitivity() {
        val config =
            ProtectionPresetEngine
                .configuration(
                    ProtectionPreset.TRANSIT
                )

        assertEquals(
            ProtectionMode.POCKET,
            config.mode
        )
        assertEquals(
            PocketSensitivity.HIGH,
            config.pocketSensitivity
        )
    }

    @Test
    fun travelUsesTwoHourPocketJourney() {
        val config =
            ProtectionPresetEngine
                .configuration(
                    ProtectionPreset.TRAVEL
                )

        assertEquals(
            ProtectionMode.JOURNEY,
            config.mode
        )
        assertEquals(
            JourneyDuration.HOURS_2,
            config.journeyDuration
        )
        assertEquals(
            GuardMode.POCKET,
            config.journeyGuardMode
        )
    }
}
