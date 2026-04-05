package com.antelop.alarm.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.antelop.alarm.R

object NotificationChannels {
    const val ALERTS = "alerts"
    const val SETUP = "setup"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val alertChannel = NotificationChannel(
            ALERTS,
            context.getString(R.string.alert_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.alert_notification_channel_description)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            enableVibration(true)
        }
        val setupChannel = NotificationChannel(
            SETUP,
            context.getString(R.string.setup_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.setup_notification_channel_description)
        }
        manager.createNotificationChannels(listOf(alertChannel, setupChannel))
    }
}
