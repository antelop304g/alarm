package com.antelop.alarm

import android.app.Application
import com.antelop.alarm.di.AppContainer
import com.antelop.alarm.notification.NotificationChannels

class AlarmApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.ensureCreated(this)
    }
}
