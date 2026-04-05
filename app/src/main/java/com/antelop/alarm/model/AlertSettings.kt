package com.antelop.alarm.model

import kotlinx.serialization.Serializable

@Serializable
data class AlertSettings(
    val ringtoneUri: String? = null,
    val vibrationEnabled: Boolean = true,
    val alarmStyleEnabled: Boolean = true,
    val autoStopMinutes: Int = 10,
)
