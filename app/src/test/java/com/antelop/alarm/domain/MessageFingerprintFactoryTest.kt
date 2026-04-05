package com.antelop.alarm.domain

import com.antelop.alarm.model.IncomingSms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MessageFingerprintFactoryTest {
    private val factory = MessageFingerprintFactory(KeywordMatcher())

    @Test
    fun sameMessageCreatesSameFingerprint() {
        val message = IncomingSms(
            address = "1069",
            body = "闽AF1234 请立即驶离",
            timestampMillis = 123456789L,
        )

        assertEquals(factory.create(message), factory.create(message))
    }

    @Test
    fun differentTimestampCreatesDifferentFingerprint() {
        val first = IncomingSms(
            address = "1069",
            body = "闽AF1234 请立即驶离",
            timestampMillis = 123456789L,
        )
        val second = first.copy(timestampMillis = 123456790L)

        assertNotEquals(factory.create(first), factory.create(second))
    }
}
