package com.riccardopinato.phoneguard.quickaccess

import com.riccardopinato.phoneguard.data.SecurityHealth
import com.riccardopinato.phoneguard.service.AlarmPhase

object QuickArmPolicy {

    fun canStartFresh(
        runtimePhase: AlarmPhase,
        hasPersistedSession: Boolean,
        securityHealth: SecurityHealth
    ): Boolean =
        runtimePhase ==
            AlarmPhase.DISARMED &&
            !hasPersistedSession &&
            securityHealth ==
            SecurityHealth.READY
}
