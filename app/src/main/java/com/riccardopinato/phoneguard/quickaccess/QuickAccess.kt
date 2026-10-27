package com.riccardopinato.phoneguard.quickaccess

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.riccardopinato.phoneguard.MainActivity
import com.riccardopinato.phoneguard.PhoneGuardApp
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.data.SecurityHealth
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.service.AlarmPhase
import com.riccardopinato.phoneguard.service.AlarmStateStore
import com.riccardopinato.phoneguard.service.ProtectionForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object QuickArmCoordinator {

    suspend fun arm(
        context: Context,
        modeOverride: ProtectionMode? = null
    ): Boolean {
        if (
            AlarmStateStore.state
                .value.phase !=
            AlarmPhase.DISARMED
        ) {
            return false
        }

        val app =
            context.applicationContext
                as? PhoneGuardApp
                ?: return false

        val health =
            app.container
                .securityRepository
                .securityHealth
                .first()

        if (
            health !=
            SecurityHealth.READY
        ) {
            return false
        }

        val settings =
            app.container
                .settingsRepository
                .settings
                .first()

        return ProtectionForegroundService
            .tryArm(
                context = context,
                mode =
                    modeOverride
                        ?: settings.selectedMode
            )
    }

    fun openApp(
        context: Context
    ) {
        val intent =
            Intent(
                context,
                MainActivity::class.java
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
            }

        runCatching {
            context.startActivity(intent)
        }
    }
}

class QuickArmReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val pending =
            goAsync()

        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Main.immediate
        ).launch {
            try {
                val mode =
                    intent
                        .getStringExtra(
                            EXTRA_MODE
                        )
                        ?.let {
                            runCatching {
                                ProtectionMode
                                    .valueOf(it)
                            }.getOrNull()
                        }

                val armed =
                    QuickArmCoordinator
                        .arm(
                            context,
                            mode
                        )

                if (!armed) {
                    QuickArmCoordinator
                        .openApp(context)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_QUICK_ARM =
            "com.riccardopinato.phoneguard.action.QUICK_ARM"

        const val EXTRA_MODE =
            "quick_arm_mode"
    }
}

class PowerConnectionReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (
            intent.action !=
            Intent.ACTION_POWER_CONNECTED
        ) {
            return
        }

        val pending =
            goAsync()

        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Main.immediate
        ).launch {
            try {
                val app =
                    context.applicationContext
                        as? PhoneGuardApp
                        ?: return@launch

                val settings =
                    app.container
                        .settingsRepository
                        .settings
                        .first()

                if (
                    !settings
                        .chargeAutoArmEnabled ||
                    AlarmStateStore.state
                        .value.phase !=
                    AlarmPhase.DISARMED
                ) {
                    return@launch
                }

                val armed =
                    QuickArmCoordinator
                        .arm(
                            context,
                            ProtectionMode.CHARGING
                        )

                if (!armed) {
                    showTapToArm(
                        context
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun showTapToArm(
        context: Context
    ) {
        createChannel(context)

        val actionIntent =
            Intent(
                context,
                QuickArmReceiver::class.java
            ).apply {
                action =
                    QuickArmReceiver
                        .ACTION_QUICK_ARM

                putExtra(
                    QuickArmReceiver
                        .EXTRA_MODE,
                    ProtectionMode
                        .CHARGING
                        .name
                )
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                actionIntent,
                PendingIntent
                    .FLAG_UPDATE_CURRENT or
                    PendingIntent
                        .FLAG_IMMUTABLE
            )

        val notification =
            NotificationCompat
                .Builder(
                    context,
                    CHANNEL_ID
                )
                .setSmallIcon(
                    R.drawable
                        .ic_stat_shield
                )
                .setContentTitle(
                    context.getString(
                        R.string
                            .auto_arm_notification_title
                    )
                )
                .setContentText(
                    context.getString(
                        R.string
                            .auto_arm_notification_body
                    )
                )
                .setContentIntent(
                    pendingIntent
                )
                .setAutoCancel(true)
                .setPriority(
                    NotificationCompat
                        .PRIORITY_DEFAULT
                )
                .build()

        runCatching {
            NotificationManagerCompat
                .from(context)
                .notify(
                    NOTIFICATION_ID,
                    notification
                )
        }
    }

    private fun createChannel(
        context: Context
    ) {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {
            return
        }

        val manager =
            context.getSystemService(
                NotificationManager::class.java
            )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(
                    R.string
                        .auto_arm_channel_name
                ),
                NotificationManager
                    .IMPORTANCE_DEFAULT
            )
        )
    }

    private companion object {
        const val CHANNEL_ID =
            "phone_guard_quick_arm"

        const val NOTIFICATION_ID =
            4102

        const val REQUEST_CODE =
            4102
    }
}
