package com.antelop.alarm.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.antelop.alarm.R
import com.antelop.alarm.di.appContainer
import com.antelop.alarm.notification.NotificationChannels
import com.antelop.alarm.receiver.AlertActionReceiver
import com.antelop.alarm.ui.AlertFullscreenActivity
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlertService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAlert()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                val sender = intent.getStringExtra(EXTRA_SENDER).orEmpty()
                val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
                val fingerprint = intent.getStringExtra(EXTRA_FINGERPRINT).orEmpty()
                startAlert(sender, body, fingerprint)
            }
        }
        return START_STICKY
    }

    private fun startAlert(sender: String, body: String, fingerprint: String) {
        serviceScope.launch {
            val prefs = appContainer.preferencesRepository.preferences.first()
            val notification = buildNotification(sender, body)
            ServiceCompat.startForeground(
                this@AlertService,
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
            startAudio(prefs.alertSettings.ringtoneUri)
            if (prefs.alertSettings.vibrationEnabled) {
                startVibration()
            }
            kotlinx.coroutines.delay(appContainer.alertPolicy.autoStopDurationMillis(prefs.alertSettings))
            val currentPrefs = appContainer.preferencesRepository.preferences.first()
            if (currentPrefs.appState.lastMatchedMessageFingerprint == fingerprint) {
                stopAlert()
                stopSelf()
            }
        }
    }

    private fun startAudio(savedUri: String?) {
        val candidateUris = listOfNotNull(
            savedUri?.let(Uri::parse),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            Uri.parse("android.resource://$packageName/${R.raw.fallback_alarm}"),
        )
        val player = MediaPlayer()
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        val selected = candidateUris.firstOrNull { uri ->
            runCatching {
                player.reset()
                player.setDataSource(this, uri)
                player.prepare()
                true
            }.getOrDefault(false)
        }
        if (selected != null) {
            player.isLooping = true
            player.start()
            mediaPlayer = player
        } else {
            player.release()
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator?.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 800, 600),
                0,
            ),
        )
    }

    private fun stopAlert() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun buildNotification(sender: String, body: String): android.app.Notification {
        val stopIntent = PendingIntent.getBroadcast(
            this,
            1,
            Intent(this, AlertActionReceiver::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            2,
            AlertFullscreenActivity.createIntent(this, sender, body),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, NotificationChannels.ALERTS)
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setContentTitle(getString(R.string.alert_notification_title))
            .setContentText("$sender：${body.take(40)}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenIntent)
            .addAction(0, getString(R.string.alert_stop_action), stopIntent)
        if (appContainer.setupController.canUseFullScreenIntent()) {
            builder.setFullScreenIntent(fullScreenIntent, true)
        }
        return builder.build()
    }

    override fun onDestroy() {
        stopAlert()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.antelop.alarm.action.START_ALERT"
        const val ACTION_STOP = "com.antelop.alarm.action.STOP_ALERT"
        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_FINGERPRINT = "extra_fingerprint"
        private const val NOTIFICATION_ID = 2001

        fun createStartIntent(
            context: Context,
            sender: String,
            body: String,
            fingerprint: String,
        ): Intent {
            return Intent(context, AlertService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_FINGERPRINT, fingerprint)
            }
        }

        fun createStopIntent(context: Context): Intent {
            return Intent(context, AlertService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}
