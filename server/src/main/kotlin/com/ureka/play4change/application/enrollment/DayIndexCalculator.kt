package com.ureka.play4change.application.enrollment

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object DayIndexCalculator {

    fun compute(enrolledAt: OffsetDateTime, timezoneHeader: String?): Int {
        val zone = timezoneHeader
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneOffset.UTC
        val enrolledDate = enrolledAt.atZoneSameInstant(zone).toLocalDate()
        val todayDate = ZonedDateTime.now(zone).toLocalDate()
        return ChronoUnit.DAYS.between(enrolledDate, todayDate).toInt().coerceAtLeast(0)
    }
}
