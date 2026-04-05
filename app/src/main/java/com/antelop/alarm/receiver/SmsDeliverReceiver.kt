package com.antelop.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.antelop.alarm.di.appContainer
import com.antelop.alarm.model.IncomingSms
import com.antelop.alarm.service.AlertService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isEmpty()) return@launch
                val body = messages.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
                val message = IncomingSms(
                    address = messages.first().originatingAddress.orEmpty(),
                    body = body,
                    timestampMillis = messages.first().timestampMillis,
                    subscriptionId = null,
                )
                val container = context.appContainer
                container.smsRepository.insertIncomingMessage(message)
                val prefs = container.preferencesRepository.preferences.first()
                val fingerprint = container.messageFingerprintFactory.create(message)
                val isDuplicate = prefs.appState.lastMatchedMessageFingerprint == fingerprint
                val shouldAlert = !isDuplicate &&
                    container.keywordMatcher.matches(message.body, prefs.keywords)
                if (shouldAlert) {
                    container.preferencesRepository.setLastMatchedFingerprint(fingerprint)
                    val serviceIntent = AlertService.createStartIntent(
                        context = context,
                        sender = message.address,
                        body = message.body,
                        fingerprint = fingerprint,
                    )
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
