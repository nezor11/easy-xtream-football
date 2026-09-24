package com.footballxtream.data

import com.footballxtream.model.Quality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelNameParserTest {

    // --- quality() ---

    @Test
    fun quality_spacedTags() {
        assertEquals(Quality.UHD_4K, ChannelNameParser.quality("Canal 4K"))
        assertEquals(Quality.QHD, ChannelNameParser.quality("Canal 2K"))
        assertEquals(Quality.FHD, ChannelNameParser.quality("Canal FHD"))
        assertEquals(Quality.FHD, ChannelNameParser.quality("Canal Full HD"))
        assertEquals(Quality.HD, ChannelNameParser.quality("Canal HD"))
        assertEquals(Quality.SD, ChannelNameParser.quality("Canal SD"))
        assertEquals(Quality.UNKNOWN, ChannelNameParser.quality("Canal sin etiqueta"))
    }

    @Test
    fun quality_gluedToWord() {
        assertEquals(Quality.HD, ChannelNameParser.quality("LaLigaTVHD"))
    }

    @Test
    fun quality_gluedWithTrailingFlagEmoji() {
        // Noise (flags/emoji) is stripped before quality is read, so the tag still resolves.
        assertEquals(Quality.HD, ChannelNameParser.quality("LigaSmartBankHD🇪🇸"))
    }

    @Test
    fun quality_doesNotReadNumberAsTag() {
        // "1080" must not be read as a quality just because it contains digits around HD/SD.
        assertEquals(Quality.FHD, ChannelNameParser.quality("Canal 1080"))
    }

    // --- baseName() / folderName() ---

    @Test
    fun baseName_stripsQualityAndPipeWrappedCountryPrefix() {
        assertEquals("Canal Sport News", ChannelNameParser.baseName("|DE|  Canal Sport News HD"))
        assertEquals("Canal Sport 24", ChannelNameParser.baseName("|IT| Canal Sport 24 HD"))
    }

    @Test
    fun baseName_stripsEventOnlyTagBareOrBracketed() {
        // The same provider writes the tag in parentheses over Xtream and bare in its M3U.
        assertEquals("CANAL DEPORTE3", ChannelNameParser.baseName("CANAL DEPORTE3 (SOLO EVENTOS)"))
        assertEquals("CANAL DEPORTE3", ChannelNameParser.baseName("CANAL DEPORTE3  SOLO EVENTOS"))
        assertEquals("Canal Sport 5", ChannelNameParser.baseName("|BE| Canal Sport 5 HD (ONLY EVENTS)"))
        assertEquals("Canal Sport 5", ChannelNameParser.baseName("|BE| Canal Sport 5 HD  ONLY EVENTS"))
        assertEquals("Canal Sport 5", ChannelNameParser.baseName("Canal Sport 5 EVENTS ONLY"))
    }

    @Test
    fun baseName_keepsChannelNamesThatMerelyContainEvent() {
        // Only the whole availability phrase is a tag; "event" inside a real name is untouched.
        assertEquals("Canal Sports Main Event", ChannelNameParser.baseName("|UK| Canal Sports Main Event"))
        assertEquals("CANAL EVENTOS 1", ChannelNameParser.baseName("CANAL EVENTOS 1"))
        assertEquals("Eventos Solo Fútbol", ChannelNameParser.baseName("Eventos Solo Fútbol"))
    }

    @Test
    fun groupKey_matchesAcrossXtreamAndM3uSpellingsOfEventOnlyTag() {
        assertEquals(
            ChannelNameParser.groupKey("CANAL DEPORTE3 (SOLO EVENTOS)"),
            ChannelNameParser.groupKey("CANAL DEPORTE3  SOLO EVENTOS"),
        )
        assertEquals(
            ChannelNameParser.groupKey("|BE| Canal Sport 6  HD (ONLY EVENTS)"),
            ChannelNameParser.groupKey("|BE| Canal Sport 6  HD  ONLY EVENTS"),
        )
    }

    @Test
    fun baseName_keepsPlainTwoLetterWords() {
        // Without a separator a leading short word is part of the name, never a prefix.
        assertEquals("La Liga TV", ChannelNameParser.baseName("La Liga TV"))
        assertEquals("Al Jazeera Sport", ChannelNameParser.baseName("Al Jazeera Sport"))
    }

    @Test
    fun folderKey_groupsByFirstWordUpToSixLetters() {
        // Multi-word brands stay together (keyed by the first word, here only 4 letters).
        assertEquals("dazn", ChannelNameParser.folderKey("DAZN LaLiga"))
        assertEquals("dazn", ChannelNameParser.folderKey("DAZN F1"))
        // "Sport …" becomes its own group.
        assertEquals("sport", ChannelNameParser.folderKey("Sport TV"))
        // Look-alikes are separated within the first 6 letters.
        assertEquals("eurosp", ChannelNameParser.folderKey("Eurosport"))
        assertEquals("eurone", ChannelNameParser.folderKey("Euronews"))
        // Fewer than 6 letters: use what's there.
        assertEquals("la", ChannelNameParser.folderKey("La 1"))
        // A 1-2 letter first word is extended with the rest so unrelated brands don't collide.
        assertEquals("mlalig", ChannelNameParser.folderKey("M+ LaLiga"))
        assertEquals("mmonde", ChannelNameParser.folderKey("2M Monde"))
        assertEquals("cmoreg", ChannelNameParser.folderKey("C More Golf"))
    }

    @Test
    fun commonFolderName_usesLongestSharedLeadingWords() {
        assertEquals("DAZN", ChannelNameParser.commonFolderName(listOf("DAZN F1", "DAZN LaLiga")))
        assertEquals("C More", ChannelNameParser.commonFolderName(listOf("C More Golf", "C More Sport")))
        // No shared leading word → first channel's first word.
        assertEquals("2M", ChannelNameParser.commonFolderName(listOf("2M Monde", "M+ LaLiga")))
    }

    @Test
    fun folderName_usesFirstWord() {
        // Folders bundle by the first word, so a shared prefix groups channels together.
        assertEquals("Canal", ChannelNameParser.folderName("Canal Deporte 1"))
        assertEquals("Eurosport", ChannelNameParser.folderName("Eurosport 2"))
        assertEquals("DAZN", ChannelNameParser.folderName("DAZN LaLiga"))
        assertEquals("DAZN", ChannelNameParser.folderName("DAZN F1"))
        // A single-word name is its own folder.
        assertEquals("LaLiga", ChannelNameParser.folderName("LaLiga"))
    }

    // --- isSports() ---

    @Test
    fun isSports_byNameTerm() {
        // A generic sport term is enough — no brand list involved.
        assertTrue(ChannelNameParser.isSports("Canal Deporte News", null))
        assertTrue(ChannelNameParser.isSports("Canal Sport 1", null)) // "sport" as a word
        assertTrue(ChannelNameParser.isSports("LaLiga TV", null)) // matches "laliga"
        // Strong roots are caught even glued mid-word.
        assertTrue(ChannelNameParser.isSports("Megasport 1", null)) // "sport" mid-word
        assertTrue(ChannelNameParser.isSports("Polideporte", null)) // "deporte" mid-word
        assertTrue(ChannelNameParser.isSports("Canal Deportes", null))
    }

    @Test
    fun isSports_substringDoesNotMatchTransportOrPassport() {
        // The "sport" root must not leak via non-sport hosts.
        assertFalse(ChannelNameParser.isSports("Transport TV", null))
        assertFalse(ChannelNameParser.isSports("Passport Channel", null))
    }

    @Test
    fun isSports_byCategory() {
        // A brand-only name (no sport term, here a fictional "Zeta 1") qualifies via its category.
        assertTrue(ChannelNameParser.isSports("Zeta 1", "Deportes"))
        assertTrue(ChannelNameParser.isSports("La 1", "Deportes"))
    }

    @Test
    fun isSports_rejectedWhenNeitherNameNorCategoryIsSport() {
        assertFalse(ChannelNameParser.isSports("CNN", "Noticias"))
        assertFalse(ChannelNameParser.isSports("Película", "Cine"))
        // Brand-only name with no sport category is no longer auto-detected (agnostic trade-off).
        assertFalse(ChannelNameParser.isSports("Zeta 1", "General"))
    }

    // --- isFootball() ---

    @Test
    fun isFootball_byCompetitionOrSportTerm() {
        assertTrue(ChannelNameParser.isFootball("LaLiga TV", null))
        assertTrue(ChannelNameParser.isFootball("LaLiga en directo", null)) // via the competition
        // A generic sports channel is sport but not specifically football.
        assertFalse(ChannelNameParser.isFootball("Canal Sport 1", null))
        assertFalse(ChannelNameParser.isFootball("NBA TV", null))
    }

    // --- isVodCategory() ---

    @Test
    fun isVodCategory_wholeWordOnly() {
        assertTrue(ChannelNameParser.isVodCategory("VOD FR"))
        assertFalse(ChannelNameParser.isVodCategory("VODAFONE"))
        assertFalse(ChannelNameParser.isVodCategory("Deportes"))
        assertFalse(ChannelNameParser.isVodCategory(null))
    }

    @Test
    fun isRadioCategory_matchesWholeWordOnly() {
        assertTrue(ChannelNameParser.isRadioCategory("Radio"))
        assertTrue(ChannelNameParser.isRadioCategory("ES | RADIOS DEPORTIVAS"))
        assertTrue(ChannelNameParser.isRadioCategory("Радио"))
        assertFalse(ChannelNameParser.isRadioCategory("Sports"))
        assertFalse(ChannelNameParser.isRadioCategory("Radiotelevisión"))
        assertFalse(ChannelNameParser.isRadioCategory(null))
    }
}
