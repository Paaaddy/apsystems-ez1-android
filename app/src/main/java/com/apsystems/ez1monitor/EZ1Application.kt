package com.apsystems.ez1monitor

import android.app.Application
import com.apsystems.ez1monitor.data.logger.FileLoggingTree
import com.apsystems.ez1monitor.data.notifications.NotificationHelper
import com.apsystems.ez1monitor.data.prefs.AppPreferences
import com.apsystems.ez1monitor.data.repository.EZ1Repository
import timber.log.Timber
import java.io.File

class EZ1Application : Application() {

    lateinit var prefs: AppPreferences
        private set

    val repository = EZ1Repository()

    val notificationHelper by lazy { NotificationHelper(this) }

    private lateinit var fileLoggingTree: FileLoggingTree

    override fun onCreate() {
        super.onCreate()

        NotificationHelper.createChannel(this)

        prefs = AppPreferences(this)

        fileLoggingTree = FileLoggingTree { File(filesDir, "logs") }
        fileLoggingTree.cleanupOldLogs()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.plant(fileLoggingTree)
    }
}
