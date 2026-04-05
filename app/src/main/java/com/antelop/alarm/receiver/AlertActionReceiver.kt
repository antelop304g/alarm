package com.antelop.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.antelop.alarm.service.AlertService

class AlertActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AlertService.ACTION_STOP) {
            context.startService(AlertService.createStopIntent(context))
        }
    }
}
