package com.footballxtream.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class XmltvEpgTest {

    @Test
    fun prioritize_putsCountryFeedsFirstThenAllSources() {
        val urls = listOf(
            "https://x/epg_ripper_FR1.xml.gz",
            "https://x/epg_ripper_ALL_SOURCES1.xml.gz",
            "https://x/epg_ripper_IT1.xml.gz",
            "https://x/epg_ripper_DE1.xml.gz",
        )
        // tvg-ids carry the country in the suffix; only Italy matches a feed here.
        val result = XmltvEpg.prioritize(urls, setOf("Rete8Sport.it", "SkySport.es"))

        assertTrue("IT feed first", result[0].contains("IT1"))
        assertTrue("all-sources next", result[1].contains("ALL_SOURCES"))
        // The unrelated feeds keep their original relative order after the prioritized ones.
        assertEquals(listOf("FR1", "DE1"), result.drop(2).map { it.substringAfter("ripper_").substringBefore(".") })
    }

    @Test
    fun prioritize_allSourcesBeatsUnrelatedWhenNoCountryFeed() {
        val urls = listOf("https://x/epg_ripper_DE1.xml.gz", "https://x/epg_ripper_ALL_SOURCES1.xml.gz")
        val result = XmltvEpg.prioritize(urls, setOf("Esport3.es"))

        assertTrue(result[0].contains("ALL_SOURCES"))
        assertTrue(result[1].contains("DE1"))
    }

    @Test
    fun prioritize_withoutCountryHints_keepsOrderUnchanged() {
        val urls = listOf("https://x/guide_b.xml", "https://x/guide_a.xml")
        // No 2-letter country suffix in the ids → nothing to prioritize by.
        assertEquals(urls, XmltvEpg.prioritize(urls, setOf("ChannelOne", "ChannelTwo")))
    }

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
