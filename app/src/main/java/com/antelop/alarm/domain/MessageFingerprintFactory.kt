package com.antelop.alarm.domain

import com.antelop.alarm.model.IncomingSms
import java.security.MessageDigest

class MessageFingerprintFactory(
    private val keywordMatcher: KeywordMatcher,
) {
    fun create(message: IncomingSms): String {
        val payload = buildString {
            append(message.timestampMillis)
            append('|')
            append(message.address.trim())
            append('|')
            append(keywordMatcher.normalize(message.body))
        }
        val bytes = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray())
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }
}
