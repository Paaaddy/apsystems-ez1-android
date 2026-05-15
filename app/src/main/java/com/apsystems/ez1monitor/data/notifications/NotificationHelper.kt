package com.apsystems.ez1monitor.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.apsystems.ez1monitor.MainActivity
import com.apsystems.ez1monitor.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "ez1_alerts"
        private const val ID_ALARM = 1
        private const val ID_POWER_LOST = 2
        private const val ID_CONNECTION_LOST = 3

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Inverter Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for EZ1 inverter alarms and connectivity issues"
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun canPost(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun tapIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun buildBase(title: String, body: String, priority: Int): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(priority)
            .setContentIntent(tapIntent())
            .setAutoCancel(true)

    fun postAlarmNotification(alarmDescription: String) {
        if (!canPost()) return
        NotificationManagerCompat.from(context).notify(
            ID_ALARM,
            buildBase(
                "EZ1 Inverter Alarm",
                alarmDescription,
                NotificationCompat.PRIORITY_HIGH
            ).build()
        )
    }

    fun postPowerLostNotification() {
        if (!canPost()) return
        NotificationManagerCompat.from(context).notify(
            ID_POWER_LOST,
            buildBase(
                "EZ1 Power Output Lost",
                "Inverter is on but producing 0 W — check for faults",
                NotificationCompat.PRIORITY_DEFAULT
            ).build()
        )
    }

    fun postConnectionLostNotification() {
        if (!canPost()) return
        NotificationManagerCompat.from(context).notify(
            ID_CONNECTION_LOST,
            buildBase(
                "EZ1 Connection Lost",
                "Cannot reach inverter — check your WiFi connection",
                NotificationCompat.PRIORITY_DEFAULT
            ).build()
        )
    }

    fun cancelAlarm() = NotificationManagerCompat.from(context).cancel(ID_ALARM)
    fun cancelPowerLost() = NotificationManagerCompat.from(context).cancel(ID_POWER_LOST)
    fun cancelConnectionLost() = NotificationManagerCompat.from(context).cancel(ID_CONNECTION_LOST)
}
