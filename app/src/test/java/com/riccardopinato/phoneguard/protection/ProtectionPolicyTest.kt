package com.riccardopinato.phoneguard.protection

import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.JourneyDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionPolicyTest {

    @Test
    fun tableRequiresAccelerometerOnly() {
        assertTrue(
            ProtectionPolicy.isSupported(
                mode = ProtectionMode.TABLE,
                journeyGuardMode = GuardMode.POCKET,
                capabilities =
                    ProtectionCapabilities(
                        hasAccelerometer = true,
                        hasProximity = false
                    )
            )
        )
    }

    @Test
    fun pocketRequiresProximityAndAccelerometer() {
        assertFalse(
            ProtectionPolicy.isSupported(
                mode = ProtectionMode.POCKET,
                journeyGuardMode = GuardMode.POCKET,
                capabilities =
                    ProtectionCapabilities(
                        hasAccelerometer = true,
                        hasProximity = false
                    )
            )
        )

        assertTrue(
            ProtectionPolicy.isSupported(
                mode = ProtectionMode.POCKET,
                journeyGuardMode = GuardMode.POCKET,
                capabilities =
                    ProtectionCapabilities(
                        hasAccelerometer = true,
                        hasProximity = true
                    )
            )
        )
    }

    @Test
    fun bagCanRunWithoutProximity() {
        assertTrue(
            ProtectionPolicy.isSupported(
                mode = ProtectionMode.BAG,
                journeyGuardMode = GuardMode.POCKET,
                capabilities =
                    ProtectionCapabilities(
                        hasAccelerometer = true,
                        hasProximity = false
                    )
            )
        )
    }

    @Test
    fun journeyUsesSelectedStorageProfileCapabilities() {
        assertFalse(
            ProtectionPolicy.isSupported(
                mode = ProtectionMode.JOURNEY,
                journeyGuardMode = GuardMode.POCKET,
                capabilities =
                    ProtectionCapabilities(
                        hasAccelerometer = true,
                        hasProximity = false
                    )
            )
        )

        assertTrue(
            ProtectionPolicy.isSupported(
                mode = ProtectionMode.JOURNEY,
                journeyGuardMode = GuardMode.BAG,
                capabilities =
                    ProtectionCapabilities(
                        hasAccelerometer = true,
                        hasProximity = false
                    )
            )
        )
    }

    @Test
    fun journeyEndAndRemainingAreDeterministic() {
        val end =
            ProtectionPolicy.journeyEndAt(
                now = 1_000L,
                duration =
                    JourneyDuration.MINUTES_30
            )

        assertEquals(
            1_801_000L,
            end
        )

        assertEquals(
            901_000L,
            ProtectionPolicy.remainingJourneyMs(
                endAt = end,
                now = 900_000L
            )
        )

        assertTrue(
            ProtectionPolicy.isJourneyExpired(
                endAt = end,
                now = 1_801_000L
            )
        )
    }

    @Test
    fun untilStoppedHasNoEndTimestamp() {
        assertNull(
            ProtectionPolicy.journeyEndAt(
                now = 1_000L,
                duration =
                    JourneyDuration.UNTIL_STOPPED
            )
        )
    }
}
