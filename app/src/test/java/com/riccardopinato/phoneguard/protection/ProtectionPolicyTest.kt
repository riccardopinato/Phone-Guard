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
                journeyGuardMode =
                    GuardMode.POCKET,
                capabilities =
                    caps(
                        accelerometer = true
                    )
            )
        )
    }

    @Test
    fun pocketRequiresProximityAndAccelerometer() {
        assertFalse(
            ProtectionPolicy.isSupported(
                mode = ProtectionMode.POCKET,
                journeyGuardMode =
                    GuardMode.POCKET,
                capabilities =
                    caps(
                        accelerometer = true,
                        proximity = false
                    )
            )
        )

        assertTrue(
            ProtectionPolicy.isSupported(
                mode = ProtectionMode.POCKET,
                journeyGuardMode =
                    GuardMode.POCKET,
                capabilities =
                    caps(
                        accelerometer = true,
                        proximity = true
                    )
            )
        )
    }

    @Test
    fun bagRejectsAccelerometerOnly() {
        assertFalse(
            ProtectionPolicy.isSupported(
                mode = ProtectionMode.BAG,
                journeyGuardMode =
                    GuardMode.POCKET,
                capabilities =
                    caps(
                        accelerometer = true
                    )
            )
        )
    }

    @Test
    fun bagSupportsAccelerometerPlusLight() {
        assertTrue(
            ProtectionPolicy.isSupported(
                mode = ProtectionMode.BAG,
                journeyGuardMode =
                    GuardMode.POCKET,
                capabilities =
                    caps(
                        accelerometer = true,
                        light = true
                    )
            )
        )
    }

    @Test
    fun bagSupportsAccelerometerPlusGyroscope() {
        assertTrue(
            ProtectionPolicy.isSupported(
                mode = ProtectionMode.BAG,
                journeyGuardMode =
                    GuardMode.POCKET,
                capabilities =
                    caps(
                        accelerometer = true,
                        gyroscope = true
                    )
            )
        )
    }

    @Test
    fun chargingDoesNotRequireMotionSensors() {
        assertTrue(
            ProtectionPolicy.isSupported(
                mode =
                    ProtectionMode.CHARGING,
                journeyGuardMode =
                    GuardMode.POCKET,
                capabilities =
                    caps()
            )
        )
    }

    @Test
    fun journeyUsesSelectedStorageProfileCapabilities() {
        assertFalse(
            ProtectionPolicy.isSupported(
                mode = ProtectionMode.JOURNEY,
                journeyGuardMode =
                    GuardMode.POCKET,
                capabilities =
                    caps(
                        accelerometer = true,
                        proximity = false,
                        light = true
                    )
            )
        )

        assertTrue(
            ProtectionPolicy.isSupported(
                mode = ProtectionMode.JOURNEY,
                journeyGuardMode =
                    GuardMode.BAG,
                capabilities =
                    caps(
                        accelerometer = true,
                        proximity = false,
                        light = true
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
            ProtectionPolicy
                .remainingJourneyMs(
                    endAt = end,
                    now = 900_000L
                )
        )

        assertTrue(
            ProtectionPolicy
                .isJourneyExpired(
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

    @Test
    fun monotonicJourneyDeadlineIgnoresWallClockChanges() {
        assertEquals(
            5_000L,
            ProtectionPolicy
                .remainingJourneyMs(
                    endAtElapsedRealtime =
                        20_000L,
                    nowElapsedRealtime =
                        15_000L,
                    endAtWallClock =
                        1_000_000L,
                    nowWallClock =
                        9_000_000L
                )
        )
    }

    private fun caps(
        accelerometer: Boolean = false,
        proximity: Boolean = false,
        light: Boolean = false,
        gyroscope: Boolean = false
    ) =
        ProtectionCapabilities(
            hasAccelerometer =
                accelerometer,
            hasProximity =
                proximity,
            hasLight =
                light,
            hasGyroscope =
                gyroscope
        )
}
