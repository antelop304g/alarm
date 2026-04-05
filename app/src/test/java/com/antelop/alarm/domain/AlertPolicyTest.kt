package com.antelop.alarm.domain

import com.antelop.alarm.model.AlertSettings
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class AlertPolicyTest {
    private val policy = AlertPolicy()

    @Test
    fun autoStopDefaultsToTenMinutes() {
        assertEquals(
            TimeUnit.MINUTES.toMillis(10),
            policy.autoStopDurationMillis(AlertSettings()),
        )
    }
}
