package com.antelop.alarm.domain

import com.antelop.alarm.model.KeywordEntry
import java.text.Normalizer
import java.util.Locale

class KeywordMatcher {
    fun normalize(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace("\\s+".toRegex(), "")
    }

    fun matches(messageBody: String, keywords: List<KeywordEntry>): Boolean {
        val normalizedMessage = normalize(messageBody)
        return keywords
            .asSequence()
            .filter { it.enabled }
            .map { normalize(it.value) }
            .filter { it.isNotBlank() }
            .any { normalizedMessage.contains(it) }
    }
}
