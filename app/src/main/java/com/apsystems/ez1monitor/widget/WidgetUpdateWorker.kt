package com.apsystems.ez1monitor.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.apsystems.ez1monitor.EZ1Application
import com.apsystems.ez1monitor.R
import com.apsystems.ez1monitor.data.repository.EZ1Result
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as EZ1Application
        val ip = app.prefs.ipAddress.first()
        val port = app.prefs.port.first()

        if (ip.isBlank()) {
            updateWidget(applicationContext, "— W", "No inverter configured")
            return Result.success()
        }

        return when (val result = app.repository.getOutputData(ip, port)) {
            is EZ1Result.Success -> {
                val powerText = "%.0f W".format(result.value.pTotal)
                updateWidget(applicationContext, powerText, "EZ1 Monitor")
                Result.success()
            }
            is EZ1Result.Failure -> {
                Timber.w("Widget update failed: %s", result.message)
                updateWidget(applicationContext, "— W", "No connection")
                Result.retry()
            }
        }
    }

    companion object {
        private const val WORK_NAME = "ez1_widget_update"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun updateWidget(context: Context, powerText: String, subtitle: String) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, EZ1WidgetProvider::class.java)
            )
            if (ids.isEmpty()) return

            val views = RemoteViews(context.packageName, R.layout.widget_ez1)
            views.setTextViewText(R.id.widget_power, powerText)
            views.setTextViewText(R.id.widget_updated, subtitle)
            manager.updateAppWidget(ids, views)
        }
    }
}
