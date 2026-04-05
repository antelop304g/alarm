package com.antelop.alarm.model

import kotlinx.serialization.Serializable

@Serializable
data class AppState(
    val isDefaultSmsApp: Boolean = false,
    val setupCompleted: Boolean = false,
    val lastMatchedMessageFingerprint: String? = null,
)
