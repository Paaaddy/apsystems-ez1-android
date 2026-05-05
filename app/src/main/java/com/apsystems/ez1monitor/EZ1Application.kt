package com.apsystems.ez1monitor

import android.app.Application
import com.apsystems.ez1monitor.data.prefs.AppPreferences
import com.apsystems.ez1monitor.data.repository.EZ1Repository
import timber.log.Timber

class EZ1Application : Application() {

    lateinit var prefs: AppPreferences
        private set

    lateinit var repository: EZ1Repository
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        prefs = AppPreferences(this)
        repository = EZ1Repository()
    }
}
