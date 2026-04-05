package com.antelop.alarm.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.antelop.alarm.R
import com.antelop.alarm.notification.NotificationChannels

class MmsWapPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(context, NotificationChannels.SETUP)
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setContentTitle("已收到彩信")
            .setContentText("当前版本暂不处理彩信内容，请尽快检查短信应用。")
            .setAutoCancel(true)
            .build()
        manager.notify(2002, notification)
    }
}
