package com.antelop.alarm.model

import kotlinx.serialization.Serializable

@Serializable
data class AppPreferences(
    val keywords: List<KeywordEntry> = listOf(
        KeywordEntry(
            id = "default-fujian-plate",
            value = "闽AF1234",
            enabled = true,
        ),
    ),
    val alertSettings: AlertSettings = AlertSettings(),
    val appState: AppState = AppState(),
)
