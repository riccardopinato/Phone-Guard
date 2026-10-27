package com.riccardopinato.phoneguard.quickaccess

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.riccardopinato.phoneguard.R

class PhoneGuardWidgetProvider :
    AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager:
            AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach {
            appWidgetId ->

            val intent =
                Intent(
                    context,
                    QuickArmReceiver::class.java
                ).apply {
                    action =
                        QuickArmReceiver
                            .ACTION_QUICK_ARM
                }

            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent
                        .FLAG_UPDATE_CURRENT or
                        PendingIntent
                            .FLAG_IMMUTABLE
                )

            val views =
                RemoteViews(
                    context.packageName,
                    R.layout
                        .widget_phone_guard
                ).apply {
                    setOnClickPendingIntent(
                        R.id
                            .widget_phone_guard_root,
                        pendingIntent
                    )
                }

            appWidgetManager
                .updateAppWidget(
                    appWidgetId,
                    views
                )
        }
    }
}
