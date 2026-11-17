package com.pay2own

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.pay2own.reminders.NotificationHelper
import com.pay2own.reminders.firebaseReady
import com.pay2own.reminders.initCrashReporting
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class Pay2OwnApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        initCrashReporting(this)
        firebaseReady(this)
    }
}
