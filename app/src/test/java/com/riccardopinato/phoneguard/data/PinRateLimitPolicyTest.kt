package com.riccardopinato.phoneguard.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PinRateLimitPolicyTest {

    @Test
    fun firstFourFailuresDoNotLock() {
        for (attempt in 1..4) {
            assertEquals(
                0L,
                PinRateLimitPolicy
                    .lockoutDurationMs(
                        attempt
                    )
            )
        }
    }

    @Test
    fun lockoutEscalatesAndCaps() {
        assertEquals(
            15_000L,
            PinRateLimitPolicy
                .lockoutDurationMs(5)
        )

        assertEquals(
            30_000L,
            PinRateLimitPolicy
                .lockoutDurationMs(6)
        )

        assertEquals(
            60_000L,
            PinRateLimitPolicy
                .lockoutDurationMs(7)
        )

        assertEquals(
            300_000L,
            PinRateLimitPolicy
                .lockoutDurationMs(8)
        )

        assertEquals(
            300_000L,
            PinRateLimitPolicy
                .lockoutDurationMs(20)
        )
    }
}
