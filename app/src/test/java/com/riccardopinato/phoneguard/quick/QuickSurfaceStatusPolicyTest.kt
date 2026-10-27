package com.riccardopinato.phoneguard.quick

import com.riccardopinato.phoneguard.service.AlarmPhase
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickSurfaceStatusPolicyTest {

    @Test
    fun armingAndCalibrationArePreparingNotProtected() {
        assertEquals(
            QuickSurfaceStatus.PREPARING,
            QuickSurfaceStatusPolicy.status(
                AlarmPhase.ARMING,
                hasPersistedSession = true
            )
        )

        assertEquals(
            QuickSurfaceStatus.PREPARING,
            QuickSurfaceStatusPolicy.status(
                AlarmPhase.CALIBRATING,
                hasPersistedSession = true
            )
        )
    }

    @Test
    fun onlyArmedIsNormalProtectedState() {
        assertEquals(
            QuickSurfaceStatus.PROTECTED,
            QuickSurfaceStatusPolicy.status(
                AlarmPhase.ARMED,
                hasPersistedSession = true
            )
        )
    }

    @Test
    fun disarmedWithPersistedSessionReportsRecovery() {
        assertEquals(
            QuickSurfaceStatus.RECOVERY_PENDING,
            QuickSurfaceStatusPolicy.status(
                AlarmPhase.DISARMED,
                hasPersistedSession = true
            )
        )

        assertEquals(
            QuickSurfaceStatus.IDLE,
            QuickSurfaceStatusPolicy.status(
                AlarmPhase.DISARMED,
                hasPersistedSession = false
            )
        )
    }

    @Test
    fun ownerCheckAndTriggeredDemandAttention() {
        assertEquals(
            QuickSurfaceStatus.ATTENTION,
            QuickSurfaceStatusPolicy.status(
                AlarmPhase.OWNER_CHECK,
                hasPersistedSession = true
            )
        )

        assertEquals(
            QuickSurfaceStatus.ATTENTION,
            QuickSurfaceStatusPolicy.status(
                AlarmPhase.TRIGGERED,
                hasPersistedSession = true
            )
        )
    }
}
