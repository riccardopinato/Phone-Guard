// TARGET: app/src/test/java/com/riccardopinato/tabletopalarm/SecurityRepositoryTest.kt
package com.riccardopinato.phoneguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityRepositoryTest {

    private val pinRegex =
        Regex("\\d{4,6}")

    @Test
    fun pinFormat_acceptsFourToSixDigits() {
        assertTrue(
            "1234".matches(
                pinRegex
            )
        )

        assertTrue(
            "123456".matches(
                pinRegex
            )
        )
    }

    @Test
    fun pinFormat_rejectsInvalidPins() {
        assertFalse(
            "123".matches(
                pinRegex
            )
        )

        assertFalse(
            "1234567".matches(
                pinRegex
            )
        )

        assertFalse(
            "12a4".matches(
                pinRegex
            )
        )
    }
}
