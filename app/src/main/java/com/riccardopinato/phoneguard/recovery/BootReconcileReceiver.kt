package com.riccardopinato.phoneguard.recovery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.riccardopinato.phoneguard.alarm.AlarmVolumeLeaseManager
import com.riccardopinato.phoneguard.data.SessionStateRepository

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

        val sessions =
            SessionStateRepository(context)

        val active =
            sessions.read()

        if (active == null) {
            AlarmVolumeLeaseManager(
                context
            ).restoreIfOwned()
        }
    }
}
