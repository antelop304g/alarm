package com.antelop.alarm.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.antelop.alarm.R
import com.antelop.alarm.di.appContainer
import com.antelop.alarm.notification.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = context.appContainer
                container.setupController.refreshAppState()
                val prefs = container.preferencesRepository.preferences.first()
                if (!prefs.appState.isDefaultSmsApp || !prefs.appState.setupCompleted) {
                    val notification = NotificationCompat.Builder(context, NotificationChannels.SETUP)
                        .setSmallIcon(R.drawable.ic_notification_alert)
                        .setContentTitle(context.getString(R.string.setup_notification_title))
                        .setContentText(context.getString(R.string.setup_notification_body))
                        .setAutoCancel(true)
                        .build()
                    val manager = context.getSystemService(NotificationManager::class.java)
                    manager.notify(1001, notification)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
