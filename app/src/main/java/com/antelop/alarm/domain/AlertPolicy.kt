package com.antelop.alarm.domain

import com.antelop.alarm.model.AlertSettings
import java.util.concurrent.TimeUnit

class AlertPolicy {
    fun autoStopDurationMillis(settings: AlertSettings): Long {
        return TimeUnit.MINUTES.toMillis(settings.autoStopMinutes.toLong())
    }
}
