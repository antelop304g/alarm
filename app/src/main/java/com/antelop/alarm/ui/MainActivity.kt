package com.antelop.alarm.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.antelop.alarm.di.appContainer
import com.antelop.alarm.model.ExternalComposeRequest
import com.antelop.alarm.ui.theme.AlarmTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val externalComposeRequest = mutableStateOf(ExternalComposeRequest())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalComposeRequest.value = parseComposeRequest(intent)
        lifecycleScope.launch {
            appContainer.setupController.refreshAppState()
        }
        setContent {
            AlarmTheme {
                AlarmApp(
                    initialComposeRequest = externalComposeRequest.value,
                    onConsumeComposeRequest = {
                        externalComposeRequest.value = ExternalComposeRequest()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalComposeRequest.value = parseComposeRequest(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            appContainer.setupController.refreshAppState()
        }
    }

    private fun parseComposeRequest(intent: Intent?): ExternalComposeRequest {
        if (intent?.action != Intent.ACTION_SENDTO) return ExternalComposeRequest()
        val uri = intent.data ?: return ExternalComposeRequest()
        val address = uri.schemeSpecificPart.substringBefore('?')
        val body = uri.getQueryParameter("body")
            ?: intent.getStringExtra("sms_body")
            ?: intent.getStringExtra(Intent.EXTRA_TEXT)
            .orEmpty()
        return ExternalComposeRequest(address = address, body = body)
    }
}
