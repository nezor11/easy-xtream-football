package com.footballxtream.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    private val playlist = """
        #EXTM3U x-tvg-url="http://epg.example/guide.xml.gz"
        #EXTINF:-1 tvg-id="deporte1.es" tvg-logo="http://logo/deporte.png" group-title="Sports",Canal Deporte 1 HD
        http://host/live/deporte1.ts
        #EXTINF:-1 group-title="Movies",Una Película
        http://host/movie/123.mp4
        #EXTINF:-1 group-title="VOD",Otra
        http://host/stream/9.mkv
    """.trimIndent()

    @Test
    fun parse_keepsLiveChannelsAndCapturesAttributes() {
        val channels = M3uParser.parse(playlist)
        // The movie (/movie/ + .mp4) and the .mkv VOD-group entry are dropped; only the live one stays.
        assertEquals(1, channels.size)
        val ch = channels.first()
        assertEquals("Canal Deporte 1 HD", ch.name)
        assertEquals("deporte1.es", ch.epgId)
        assertEquals("http://logo/deporte.png", ch.iconUrl)
        assertEquals("Sports", ch.categoryName)
        assertEquals("http://host/live/deporte1.ts", ch.streamUrl)
    }

    @Test
    fun parse_usesTrailingNameWhenNoTvgName() {
        val channels = M3uParser.parse(playlist)
        assertEquals("Canal Deporte 1 HD", channels.first().name)
    }

    @Test
    fun parse_channelWithoutTvgIdHasNullEpgId() {
        val content = """
            #EXTM3U
            #EXTINF:-1 group-title="Sports",Canal Deporte
            http://host/live/x.ts
        """.trimIndent()
        assertNull(M3uParser.parse(content).first().epgId)
    }

    @Test
    fun parse_extractsCountryFromTvgIdSuffix() {
        // "deporte1.es" → country "es".
        assertEquals("es", M3uParser.parse(playlist).first().country)
    }

    @Test
    fun parse_extractsCountryWhenTvgIdHasFeedSuffix() {
        // iptv-org appends a feed tag after the country: "...es@SD" → country "es".
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="3CatCameres.es@SD" group-title="Sports",3Cat
            http://host/live/x.ts
        """.trimIndent()
        assertEquals("es", M3uParser.parse(content).first().country)
    }

    @Test
    fun parse_dotTvSuffixIsNotTreatedAsCountry() {
        // ".tv" is Tuvalu's ISO code but here it's the generic vanity domain, not a country.
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="Teledeporte.tv" group-title="Sports",Teledeporte
            http://host/live/x.ts
        """.trimIndent()
        assertNull(M3uParser.parse(content).first().country)
    }

    @Test
    fun parse_countryNullForNonCountrySuffix() {
        // A 2-letter suffix that isn't an ISO country ("HD") must not be read as a country.
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="Sky.Sport.HD" group-title="Sports",Sky Sport
            http://host/live/x.ts
        """.trimIndent()
        assertNull(M3uParser.parse(content).first().country)
    }

    @Test
    fun parse_flagsGeoBlockedFromNameTag() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="x.es" group-title="Sports",Canal Deporte [Geo-blocked]
            http://host/live/x.ts
            #EXTINF:-1 tvg-id="y.es" group-title="Sports",Otro Canal
            http://host/live/y.ts
        """.trimIndent()
        val channels = M3uParser.parse(content)
        assertTrue(channels.first { it.name.startsWith("Canal") }.geoBlocked)
        assertFalse(channels.first { it.name.startsWith("Otro") }.geoBlocked)
    }

    @Test
    fun epgUrls_xTvgUrl() {
        assertEquals(listOf("http://epg.example/guide.xml.gz"), M3uParser.epgUrls(playlist))
    }

    @Test
    fun epgUrls_commaSeparatedUrlTvg() {
        val content = """#EXTM3U url-tvg="http://a.example/a.xml,http://b.example/b.xml.gz""""
        assertEquals(
            listOf("http://a.example/a.xml", "http://b.example/b.xml.gz"),
            M3uParser.epgUrls(content),
        )
    }

    @Test
    fun epgUrls_emptyWhenNoneDeclared() {
        assertTrue(M3uParser.epgUrls("#EXTM3U\n#EXTINF:-1,Foo\nhttp://x/y.ts").isEmpty())
    }
}
