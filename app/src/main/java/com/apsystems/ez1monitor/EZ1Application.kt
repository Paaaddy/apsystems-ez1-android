package com.apsystems.ez1monitor

import android.app.Application
import com.apsystems.ez1monitor.data.logger.FileLoggingTree
import com.apsystems.ez1monitor.data.prefs.AppPreferences
import com.apsystems.ez1monitor.data.repository.EZ1Repository
import timber.log.Timber

class EZ1Application : Application() {

    lateinit var prefs: AppPreferences
        private set

    val repository = EZ1Repository()

    private lateinit var fileLoggingTree: FileLoggingTree

    override fun onCreate() {
        super.onCreate()

        prefs = AppPreferences(this)

        fileLoggingTree = FileLoggingTree { getExternalFilesDir("logs") }
        fileLoggingTree.cleanupOldLogs()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.plant(fileLoggingTree)
    }
}
