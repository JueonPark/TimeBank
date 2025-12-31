package com.example.timebank

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilTest {

    @Test
    fun formatTime_zero() {
        assertEquals("00:00:00", TimeUtil.formatTime(0))
    }

    @Test
    fun formatTime_oneSecond() {
        assertEquals("00:00:01", TimeUtil.formatTime(1000))
    }

    @Test
    fun formatTime_oneMinute() {
        assertEquals("00:01:00", TimeUtil.formatTime(60 * 1000))
    }

    @Test
    fun formatTime_oneHour() {
        assertEquals("01:00:00", TimeUtil.formatTime(60 * 60 * 1000))
    }

    @Test
    fun formatTime_complexTime() {
        // 1 hour, 1 minute, 1 second
        val millis = 3600000L + 60000L + 1000L
        assertEquals("01:01:01", TimeUtil.formatTime(millis))
    }

    @Test
    fun formatTime_largeTime() {
        // 25 hours, 0 minutes, 0 seconds
        // Note: The current implementation might show 25:00:00 or might rollover depending on logic.
        // Let's check TimeUtil logic:
        // val hours = TimeUnit.MILLISECONDS.toHours(millis) -> this returns total hours.
        // So 25 hours will be "25:00:00"
        val millis = 25L * 3600000L
        assertEquals("25:00:00", TimeUtil.formatTime(millis))
    }
}
