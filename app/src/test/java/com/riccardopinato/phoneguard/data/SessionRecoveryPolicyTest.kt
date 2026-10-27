package com.riccardopinato.phoneguard.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRecoveryPolicyTest {

    @Test
    fun sameBoot_acceptsSmallClockDrift() {
        assertTrue(
            SessionRecoveryPolicy.sameBoot(
                savedBootMarker = 1_000_000L,
                currentBootMarker = 1_060_000L
            )
        )
    }

    @Test
    fun sameBoot_rejectsDifferentBoot() {
        assertFalse(
            SessionRecoveryPolicy.sameBoot(
                savedBootMarker = 1_000_000L,
                currentBootMarker = 1_300_001L
            )
        )
    }

    @Test
    fun sameBoot_rejectsMissingMarker() {
        assertFalse(
            SessionRecoveryPolicy.sameBoot(
                savedBootMarker = 0L,
                currentBootMarker = 1_000_000L
            )
        )
    }
}
