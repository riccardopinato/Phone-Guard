package com.riccardopinato.phoneguard.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat

class ChargingGuardController(
    context: Context,
    private val onDisconnected: () -> Unit
) {
    private val appContext =
        context.applicationContext

    private var registered = false

    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                if (
                    intent?.action ==
                    Intent.ACTION_POWER_DISCONNECTED
                ) {
                    onDisconnected()
                }
            }
        }

    fun start(): Boolean {
        stop()

        if (!isPowerConnected(appContext)) {
            return false
        }

        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(
                Intent.ACTION_POWER_DISCONNECTED
            ),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        registered = true
        return true
    }

    fun stop() {
        if (!registered) return

        runCatching {
            appContext.unregisterReceiver(
                receiver
            )
        }

        registered = false
    }

    companion object {
        fun isPowerConnected(
            context: Context
        ): Boolean {
            val battery =
                context.registerReceiver(
                    null,
                    IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                    )
                ) ?: return false

            return battery.getIntExtra(
                BatteryManager.EXTRA_PLUGGED,
                0
            ) != 0
        }
    }
}
