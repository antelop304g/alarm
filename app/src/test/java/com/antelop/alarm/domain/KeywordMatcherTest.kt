package com.antelop.alarm.domain

import com.antelop.alarm.model.KeywordEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordMatcherTest {
    private val matcher = KeywordMatcher()

    @Test
    fun matchesKeywordAfterNormalization() {
        val matched = matcher.matches(
            messageBody = "您的车辆 闽ＡＦ 1234 请立即挪车",
            keywords = listOf(KeywordEntry(id = "1", value = "闽AF1234")),
        )

        assertTrue(matched)
    }

    @Test
    fun ignoresDisabledKeyword() {
        val matched = matcher.matches(
            messageBody = "您的车辆 闽AF1234 请立即挪车",
            keywords = listOf(KeywordEntry(id = "1", value = "闽AF1234", enabled = false)),
        )

        assertFalse(matched)
    }

    @Test
    fun returnsFalseWhenBodyDoesNotContainKeyword() {
        val matched = matcher.matches(
            messageBody = "欢迎使用停车服务",
            keywords = listOf(KeywordEntry(id = "1", value = "闽AF1234")),
        )

        assertFalse(matched)
    }
}
