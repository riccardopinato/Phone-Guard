package com.riccardopinato.phoneguard.charging

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class ChargingStateMonitor(
    context: Context
) {
    private val appContext =
        context.applicationContext

    fun isPowerConnected(): Boolean {
        val battery =
            appContext.registerReceiver(
                null,
                IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED
                )
            ) ?: return false

        val plugged =
            battery.getIntExtra(
                BatteryManager.EXTRA_PLUGGED,
                0
            )

        val status =
            battery.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN
            )

        return plugged != 0 ||
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }
}
