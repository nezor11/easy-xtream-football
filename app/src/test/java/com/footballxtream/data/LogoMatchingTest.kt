package com.footballxtream.data

import com.footballxtream.model.ChannelGroup
import com.footballxtream.model.ChannelVariant
import com.footballxtream.model.LiveChannel
import com.footballxtream.model.Quality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoMatchingTest {

    @Test
    fun candidates_exactNameComesFirst() {
        assertEquals("canaldeporte2", LogoMatching.candidates("Canal Deporte 2").first())
    }

    @Test
    fun candidates_includeSportSportsAndTrailingTvVariants() {
        val keys = LogoMatching.candidates("Canal Sport 1")
        assertTrue(keys.contains("canalsports1"))
        assertTrue(LogoMatching.candidates("Canal Sports").contains("canalsport"))
        assertTrue(LogoMatching.candidates("Canal Deporte TV").contains("canaldeporte"))
    }

    @Test
    fun candidates_dropTrailingWordsBeforeLeadingOnes() {
        val keys = LogoMatching.candidates("Pais Canal Deporte Norte Alternate")
        // Trailing runs (suffix removed) are tried before the run that lost its country prefix.
        assertTrue(keys.indexOf("paiscanaldeportenorte") < keys.indexOf("canaldeportenortealternate"))
        assertTrue(keys.contains("paiscanaldeporte"))
        // Runs shrink from one end at a time, never both at once.
        assertFalse(keys.contains("canaldeportenorte"))
    }

    @Test
    fun candidates_neverShrinkToOneWordWithLetters() {
        val keys = LogoMatching.candidates("Canal Deporte 2")
        assertFalse(keys.contains("canal"))
        assertFalse(keys.contains("deporte2"))
    }

    @Test
    fun candidates_leadingDropKeepsAWholeWordAndEnoughLength() {
        // "TV Sports" left over after dropping the country would claim an unrelated logo: rejected.
        val keys = LogoMatching.candidates("Pais TV Sports")
        assertFalse(keys.contains("tvsports"))
        assertFalse(keys.contains("tvsport"))
        // A real brand after the prefix is fine.
        assertTrue(LogoMatching.candidates("Pais Canal Deporte 2").contains("canaldeporte2"))
    }

    private fun group(name: String, iconUrl: String?): ChannelGroup {
        val ch = LiveChannel(name.hashCode(), name, iconUrl, "Sports", "http://host/${name.hashCode()}")
        return ChannelGroup(
            key = name.lowercase(),
            displayName = name,
            iconUrl = iconUrl,
            isFootball = false,
            variants = listOf(ChannelVariant(ch, Quality.UNKNOWN)),
        )
    }

    @Test
    fun fillFromFolderSiblings_borrowsTheFamilyLogoAndLeavesOrphansAlone() {
        val filled = LogoMatching.fillFromFolderSiblings(
            listOf(
                group("Canal Deporte", "http://logo/deporte.png"),
                group("Canal Deporte 2", null),
                group("Otro Canal", null),
            ),
        )
        assertEquals("http://logo/deporte.png", filled[1].iconUrl)
        assertNull(filled[2].iconUrl)
        // A group that already has a logo keeps its own.
        assertEquals("http://logo/deporte.png", filled[0].iconUrl)
    }
}
