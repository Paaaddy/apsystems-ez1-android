package com.apsystems.ez1monitor.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.widget.RemoteViews
import com.apsystems.ez1monitor.MainActivity
import com.apsystems.ez1monitor.R

class EZ1WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_ez1)

            // Tap widget → open main app
            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_power, openApp)
            views.setOnClickPendingIntent(R.id.widget_updated, openApp)

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        // Schedule background updates via WorkManager
        WidgetUpdateWorker.schedule(context)
    }

    override fun onEnabled(context: Context) {
        WidgetUpdateWorker.schedule(context)
    }

    override fun onDisabled(context: Context) {
        // WorkManager periodic work persists — no cleanup needed
    }
}
