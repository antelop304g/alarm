package com.antelop.alarm.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antelop.alarm.service.AlertService
import com.antelop.alarm.ui.theme.AlarmTheme

class AlertFullscreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sender = intent.getStringExtra(EXTRA_SENDER).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        setContent {
            AlarmTheme {
                AlertFullscreenContent(
                    sender = sender,
                    body = body,
                    onStop = {
                        startService(AlertService.createStopIntent(this))
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_SENDER = "extra_sender"
        private const val EXTRA_BODY = "extra_body"

        fun createIntent(context: Context, sender: String, body: String): Intent {
            return Intent(context, AlertFullscreenActivity::class.java).apply {
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_BODY, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }
}

@Composable
private fun AlertFullscreenContent(
    sender: String,
    body: String,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFB))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "发现疑似抄牌短信",
            color = Color(0xFFDC2626),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = sender.ifBlank { "未知号码" },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onStop, modifier = Modifier.size(width = 220.dp, height = 56.dp)) {
            Text("停止提醒")
        }
    }
}
