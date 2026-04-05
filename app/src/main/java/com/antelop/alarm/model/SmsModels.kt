package com.antelop.alarm.model

data class IncomingSms(
    val address: String,
    val body: String,
    val timestampMillis: Long,
    val subscriptionId: Int? = null,
)

data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestampMillis: Long,
    val type: Int,
    val read: Boolean,
) {
    val isOutgoing: Boolean
        get() = type == 2 || type == 4 || type == 5 || type == 6
}

data class ConversationSummary(
    val threadId: Long,
    val address: String,
    val snippet: String,
    val timestampMillis: Long,
    val messageCount: Int,
    val unreadCount: Int,
)

data class ExternalComposeRequest(
    val address: String = "",
    val body: String = "",
)
