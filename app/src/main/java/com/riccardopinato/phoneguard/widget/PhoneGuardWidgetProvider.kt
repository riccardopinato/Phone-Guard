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
import com.riccardopinato.phoneguard.quick.QuickSurfaceStatus
import com.riccardopinato.phoneguard.quick.QuickSurfaceStatusPolicy
import com.riccardopinato.phoneguard.service.AlarmStateStore

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
                id
            )
        }
    }

    companion object {
        fun updateAll(
            context: Context
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
                        id
                    )
                }
        }

        private fun render(
            context: Context,
            manager: AppWidgetManager,
            id: Int
        ) {
            val app =
                context.applicationContext
                    as PhoneGuardApp

            val status =
                QuickSurfaceStatusPolicy
                    .status(
                        phase =
                            AlarmStateStore
                                .state
                                .value
                                .phase,
                        hasPersistedSession =
                            app.container
                                .sessionStateRepository
                                .read() != null
                    )

            val views =
                RemoteViews(
                    context.packageName,
                    R.layout.widget_phone_guard
                )

            views.setTextViewText(
                R.id.widget_status,
                context.getString(
                    when (status) {
                        QuickSurfaceStatus.IDLE ->
                            R.string
                                .widget_status_ready

                        QuickSurfaceStatus.PREPARING ->
                            R.string
                                .widget_status_preparing

                        QuickSurfaceStatus.PROTECTED ->
                            R.string
                                .widget_status_active

                        QuickSurfaceStatus.ATTENTION ->
                            R.string
                                .widget_status_attention

                        QuickSurfaceStatus.RECOVERY_PENDING ->
                            R.string
                                .widget_status_recovery

                        QuickSurfaceStatus.ERROR ->
                            R.string
                                .widget_status_error
                    }
                )
            )

            val openIntent =
                Intent(
                    context,
                    MainActivity::class.java
                )
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
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
                    MainActivity::class.java
                )
                    .setAction(
                        MainActivity
                            .ACTION_ARM_WIDGET
                    )
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )

            val armPending =
                PendingIntent.getActivity(
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
                    if (
                        status ==
                        QuickSurfaceStatus.IDLE
                    ) {
                        R.string.widget_arm
                    } else {
                        R.string.widget_open
                    }
                )
            )

            views.setOnClickPendingIntent(
                R.id.widget_arm,
                if (
                    status ==
                    QuickSurfaceStatus.IDLE
                ) {
                    armPending
                } else {
                    openPending
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
