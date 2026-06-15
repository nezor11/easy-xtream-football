package com.footballxtream.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class XmltvEpgTest {

    @Test
    fun parseTime_withOffset() {
        // 2026-05-25 20:00:00 +0200 == 18:00:00 UTC.
        val expected = utcMillis(2026, 5, 25, 18, 0, 0)
        assertEquals(expected, XmltvEpg.parseTime("20260525200000 +0200"))
    }

    @Test
    fun parseTime_bareIsTreatedAsUtc() {
        val expected = utcMillis(2026, 5, 25, 20, 0, 0)
        assertEquals(expected, XmltvEpg.parseTime("20260525200000"))
    }

    @Test
    fun parseTime_offsetWithoutSpace() {
        val expected = utcMillis(2026, 5, 25, 18, 0, 0)
        assertEquals(expected, XmltvEpg.parseTime("20260525200000+0200"))
    }

    @Test
    fun parseTime_invalidReturnsZero() {
        assertEquals(0L, XmltvEpg.parseTime(null))
        assertEquals(0L, XmltvEpg.parseTime(""))
        assertEquals(0L, XmltvEpg.parseTime("not-a-date"))
        assertEquals(0L, XmltvEpg.parseTime("2026")) // too short
    }

    private fun utcMillis(y: Int, mo: Int, d: Int, h: Int, mi: Int, s: Int): Long {
        val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.set(y, mo - 1, d, h, mi, s)
        return cal.timeInMillis
    }
}
