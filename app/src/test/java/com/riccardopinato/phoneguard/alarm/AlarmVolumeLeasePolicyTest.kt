package com.riccardopinato.phoneguard.alarm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmVolumeLeasePolicyTest {

    private val lease =
        AlarmVolumeLease(
            originalLevel = 3,
            targetLevel = 8,
            targetPercent = 80,
            acquiredAt = 1L
        )

    @Test
    fun restoresOnlyWhilePhoneGuardStillOwnsTargetLevel() {
        assertTrue(
            AlarmVolumeLeasePolicy
                .shouldRestore(
                    currentLevel = 8,
                    lease = lease
                )
        )
    }

    @Test
    fun doesNotOverwriteManualUserVolumeChange() {
        assertFalse(
            AlarmVolumeLeasePolicy
                .shouldRestore(
                    currentLevel = 5,
                    lease = lease
                )
        )
    }
}
