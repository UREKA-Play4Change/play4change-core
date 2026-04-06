package com.ureka.play4change.application.enrollment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class DayIndexCalculatorTest {

    // ── Same day / basic counts ───────────────────────────────────────────────

    @Test
    fun `dayIndex is 0 when enrolled today and timezone is null (defaults to UTC)`() {
        // null timezone → code falls back to ZoneOffset.UTC via getOrNull() ?: ZoneOffset.UTC
        val enrolledAt = OffsetDateTime.now(ZoneOffset.UTC)
        assertEquals(0, DayIndexCalculator.compute(enrolledAt, null))
    }

    @Test
    fun `dayIndex is 0 when enrolled today with explicit UTC`() {
        val enrolledAt = OffsetDateTime.now(ZoneOffset.UTC)
        assertEquals(0, DayIndexCalculator.compute(enrolledAt, "UTC"))
    }

    @Test
    fun `dayIndex is 1 when enrolled exactly one calendar day ago`() {
        val enrolledAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)
        assertEquals(1, DayIndexCalculator.compute(enrolledAt, "UTC"))
    }

    @Test
    fun `dayIndex is 7 when enrolled seven calendar days ago`() {
        val enrolledAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7)
        assertEquals(7, DayIndexCalculator.compute(enrolledAt, "UTC"))
    }

    // ── Timezone handling ─────────────────────────────────────────────────────

    @Test
    fun `invalid timezone string falls back to UTC`() {
        // ZoneId.of("Not/Real") throws; runCatching { }.getOrNull() returns null → UTC used
        val enrolledAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(3)
        val withInvalid = DayIndexCalculator.compute(enrolledAt, "Not/A/Real/Zone")
        val withUtc    = DayIndexCalculator.compute(enrolledAt, "UTC")
        assertEquals(withUtc, withInvalid)
    }

    @Test
    fun `timezone affects enrolled date - UTC+2 shifts a 23h00 UTC enrollment to next calendar day`() {
        // Enrolled 2020-01-01T23:00Z:
        //   in UTC        → enrolledDate = 2020-01-01
        //   in Africa/Cairo (UTC+2) → enrolledDate = 2020-01-02 (01:00 local)
        // The UTC calculation has 1 more day of elapsed time than the Cairo calculation
        // because it sees the enrollment 1 calendar day earlier.
        // (The assertion uses >= 0 to remain non-flaky across the 2-hour window where
        //  ZonedDateTime.now(UTC) and now(Cairo) may land on different calendar dates.)
        val enrolledAt = OffsetDateTime.of(2020, 1, 1, 23, 0, 0, 0, ZoneOffset.UTC)
        val utcIndex   = DayIndexCalculator.compute(enrolledAt, "UTC")
        val cairoIndex = DayIndexCalculator.compute(enrolledAt, "Africa/Cairo")
        // UTC enrolled date (Jan 1) is earlier than Cairo enrolled date (Jan 2)
        // → UTC sees more elapsed days → utcIndex >= cairoIndex
        assertTrue(utcIndex >= cairoIndex,
            "Expected UTC day index ($utcIndex) >= Cairo day index ($cairoIndex) " +
            "because enrolled date in UTC is one day earlier than in UTC+2")
        // The difference is exactly 1 in ~91.7% of runs (0 during the 2-hour Cairo rollover window)
        assertTrue(utcIndex - cairoIndex <= 1,
            "Difference should be at most 1 (enrolled date shifts by exactly 1 calendar day)")
    }

    // ── Boundary / clock-skew guard ───────────────────────────────────────────

    @Test
    fun `dayIndex is never below 0 for a future enrollment timestamp`() {
        // coerceAtLeast(0) prevents negative indices from clock skew or data issues
        val futureEnrolledAt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2)
        assertEquals(0, DayIndexCalculator.compute(futureEnrolledAt, "UTC"))
    }
}
