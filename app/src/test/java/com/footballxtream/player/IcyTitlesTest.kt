package com.footballxtream.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IcyTitlesTest {
    @Test
    fun clean_keepsPlainTitles() {
        assertEquals("Tiempo de Juego", IcyTitles.clean("Tiempo de Juego"))
        assertEquals("Artist - Song", IcyTitles.clean("  'Artist - Song' "))
    }

    @Test
    fun clean_extractsIHeartText() {
        val raw = "zc4732 - text=\"The Herd with Colin Cowherd (M-F 12p-3p ET)\" song_spot=\"T\" MediaBaseId=\"0\""
        assertEquals("The Herd with Colin Cowherd (M-F 12p-3p ET)", IcyTitles.clean(raw))
    }

    @Test
    fun clean_dropsEmptyAndPlaceholderTitles() {
        assertNull(IcyTitles.clean(null))
        assertNull(IcyTitles.clean(""))
        assertNull(IcyTitles.clean("_"))
        assertNull(IcyTitles.clean(" - "))
        assertNull(IcyTitles.clean("zc1 - text=\"\" song_spot=\"T\""))
    }
}
