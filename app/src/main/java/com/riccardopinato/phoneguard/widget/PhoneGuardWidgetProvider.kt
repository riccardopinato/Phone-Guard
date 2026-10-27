package com.riccardopinato.phoneguard.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.riccardopinato.phoneguard.MainActivity
import com.riccardopinato.phoneguard.PhoneGuardApp
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.activation.ActivationOutcome
import com.riccardopinato.phoneguard.activation.ActivationSource
import com.riccardopinato.phoneguard.activation.ProtectionActivationCoordinator
import com.riccardopinato.phoneguard.service.AlarmPhase
import com.riccardopinato.phoneguard.service.AlarmStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PhoneGuardWidgetProvider :
    AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach {
            id ->
            render(
                context,
                appWidgetManager,
                id,
                null
            )
        }
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        super.onReceive(
            context,
            intent
        )

        if (
            intent.action !=
            ACTION_ARM_LAST
        ) {
            return
        }

        val pending =
            goAsync()

        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Main
        ).launch {
            try {
                val app =
                    context.applicationContext
                        as PhoneGuardApp

                val settings =
                    app.container
                        .settingsRepository
                        .settings
                        .first()

                val result =
                    ProtectionActivationCoordinator
                        .requestArm(
                            context =
                                context,
                            mode =
                                settings.selectedMode,
                            source =
                                ActivationSource.WIDGET
                        )

                val message =
                    when (result) {
                        ActivationOutcome.Started ->
                            context.getString(
                                R.string
                                    .widget_status_started
                            )

                        is ActivationOutcome.Blocked ->
                            context.getString(
                                R.string
                                    .widget_status_open_app
                            )
                    }

                updateAll(
                    context,
                    message
                )
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_ARM_LAST =
            "com.riccardopinato.phoneguard.widget.ARM_LAST"

        fun updateAll(
            context: Context,
            overrideMessage: String? = null
        ) {
            val manager =
                AppWidgetManager
                    .getInstance(
                        context
                    )

            val component =
                ComponentName(
                    context,
                    PhoneGuardWidgetProvider::class.java
                )

            manager
                .getAppWidgetIds(
                    component
                )
                .forEach {
                    id ->
                    render(
                        context,
                        manager,
                        id,
                        overrideMessage
                    )
                }
        }

        private fun render(
            context: Context,
            manager: AppWidgetManager,
            id: Int,
            overrideMessage: String?
        ) {
            val app =
                context.applicationContext
                    as PhoneGuardApp

            val active =
                AlarmStateStore.state
                    .value.phase !=
                    AlarmPhase.DISARMED ||
                    app.container
                        .sessionStateRepository
                        .read() != null

            val views =
                RemoteViews(
                    context.packageName,
                    R.layout.widget_phone_guard
                )

            views.setTextViewText(
                R.id.widget_status,
                overrideMessage
                    ?: context.getString(
                        if (active) {
                            R.string
                                .widget_status_active
                        } else {
                            R.string
                                .widget_status_ready
                        }
                    )
            )

            val openIntent =
                Intent(
                    context,
                    MainActivity::class.java
                ).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

            val openPending =
                PendingIntent.getActivity(
                    context,
                    721,
                    openIntent,
                    PendingIntent
                        .FLAG_UPDATE_CURRENT or
                        PendingIntent
                            .FLAG_IMMUTABLE
                )

            val armIntent =
                Intent(
                    context,
                    PhoneGuardWidgetProvider::class.java
                ).setAction(
                    ACTION_ARM_LAST
                )

            val armPending =
                PendingIntent.getBroadcast(
                    context,
                    720,
                    armIntent,
                    PendingIntent
                        .FLAG_UPDATE_CURRENT or
                        PendingIntent
                            .FLAG_IMMUTABLE
                )

            views.setTextViewText(
                R.id.widget_arm,
                context.getString(
                    if (active) {
                        R.string.widget_open
                    } else {
                        R.string.widget_arm
                    }
                )
            )

            views.setOnClickPendingIntent(
                R.id.widget_arm,
                if (active) {
                    openPending
                } else {
                    armPending
                }
            )

            views.setOnClickPendingIntent(
                R.id.widget_open,
                openPending
            )

            manager.updateAppWidget(
                id,
                views
            )
        }
    }
}
