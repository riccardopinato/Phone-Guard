package com.riccardopinato.phoneguard.quick

import com.riccardopinato.phoneguard.service.AlarmPhase

enum class QuickSurfaceStatus {
    IDLE,
    PREPARING,
    PROTECTED,
    ATTENTION,
    RECOVERY_PENDING,
    ERROR;

    val hasExistingSession: Boolean
        get() =
            this != IDLE
}

object QuickSurfaceStatusPolicy {
    fun status(
        phase: AlarmPhase,
        hasPersistedSession: Boolean
    ): QuickSurfaceStatus =
        when (phase) {
            AlarmPhase.DISARMED ->
                if (hasPersistedSession) {
                    QuickSurfaceStatus
                        .RECOVERY_PENDING
                } else {
                    QuickSurfaceStatus.IDLE
                }

            AlarmPhase.ARMING,
            AlarmPhase.CALIBRATING ->
                QuickSurfaceStatus.PREPARING

            AlarmPhase.ARMED ->
                QuickSurfaceStatus.PROTECTED

            AlarmPhase.OWNER_CHECK,
            AlarmPhase.TRIGGERED ->
                QuickSurfaceStatus.ATTENTION

            AlarmPhase.ERROR ->
                QuickSurfaceStatus.ERROR
        }
}
