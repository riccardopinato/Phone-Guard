package com.riccardopinato.phoneguard.quickaccess

import com.riccardopinato.phoneguard.data.SecurityHealth
import com.riccardopinato.phoneguard.service.AlarmPhase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickArmPolicyTest {

    @Test
    fun persistedSessionBlocksFreshArm() {
        assertFalse(
            QuickArmPolicy.canStartFresh(
                runtimePhase =
                    AlarmPhase.DISARMED,
                hasPersistedSession =
                    true,
                securityHealth =
                    SecurityHealth.READY
            )
        )
    }

    @Test
    fun triggeredRuntimeBlocksFreshArm() {
        assertFalse(
            QuickArmPolicy.canStartFresh(
                runtimePhase =
                    AlarmPhase.TRIGGERED,
                hasPersistedSession =
                    false,
                securityHealth =
                    SecurityHealth.READY
            )
        )
    }

    @Test
    fun invalidSecurityBlocksFreshArm() {
        assertFalse(
            QuickArmPolicy.canStartFresh(
                runtimePhase =
                    AlarmPhase.DISARMED,
                hasPersistedSession =
                    false,
                securityHealth =
                    SecurityHealth.NO_PIN
            )
        )
    }

    @Test
    fun cleanDisarmedReadyStateAllowsFreshArm() {
        assertTrue(
            QuickArmPolicy.canStartFresh(
                runtimePhase =
                    AlarmPhase.DISARMED,
                hasPersistedSession =
                    false,
                securityHealth =
                    SecurityHealth.READY
            )
        )
    }
}
