// TARGET: app/src/main/java/com/riccardopinato/tabletopalarm/hardware/BatteryDiagnostics.kt
package com.riccardopinato.phoneguard.hardware

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

class BatteryDiagnostics(
    private val context: Context
) {

    fun isIgnoringBatteryOptimizations():
        Boolean {

        val powerManager =
            context.getSystemService(
                Context.POWER_SERVICE
            ) as PowerManager

        return powerManager
            .isIgnoringBatteryOptimizations(
                context.packageName
            )
    }

    fun openBatteryOptimizationSettings():
        Boolean {

        val intents =
            listOf(
                Intent(
                    Settings
                        .ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                ),
                Intent(
                    Settings
                        .ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse(
                        "package:${context.packageName}"
                    )
                )
            )

        for (intent in intents) {
            val resolved =
                intent.resolveActivity(
                    context.packageManager
                ) != null

            if (resolved) {
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                context.startActivity(
                    intent
                )

                return true
            }
        }

        return false
    }
}
