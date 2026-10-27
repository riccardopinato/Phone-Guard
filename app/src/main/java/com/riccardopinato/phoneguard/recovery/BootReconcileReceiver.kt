package com.riccardopinato.phoneguard.recovery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.riccardopinato.phoneguard.alarm.AlarmVolumeLeaseManager
import com.riccardopinato.phoneguard.data.SessionStateRepository
import com.riccardopinato.phoneguard.data.SettingsRepository
import com.riccardopinato.phoneguard.service.ProtectionForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReconcileReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent?
    ) {
        if (
            intent?.action !=
            Intent.ACTION_BOOT_COMPLETED
        ) {
            return
        }

        val pending =
            goAsync()

        CoroutineScope(
            SupervisorJob() +
                Dispatchers.IO
        ).launch {
            try {
                val sessions =
                    SessionStateRepository(
                        context
                    )

                val active =
                    sessions.read()

                if (active == null) {
                    AlarmVolumeLeaseManager(
                        context
                    ).restoreIfOwned()
                }

                val settings =
                    SettingsRepository(
                        context
                    )
                        .settings
                        .first()

                if (settings.autoArmCharging || settings.adaptiveAutoGuardEnabled) {
                    // One foreground owner reconciles both approved standby modes.
                    // No stale pre-reboot auto-arm timer is carried over.
                    if (settings.autoArmCharging) {
                        ProtectionForegroundService.setChargingAutoArm(context, true)
                    } else {
                        ProtectionForegroundService.setAdaptiveAutoGuard(context, true)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
