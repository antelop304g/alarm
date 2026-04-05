package com.antelop.alarm.model

import kotlinx.serialization.Serializable

@Serializable
data class KeywordEntry(
    val id: String,
    val value: String,
    val enabled: Boolean = true,
)
