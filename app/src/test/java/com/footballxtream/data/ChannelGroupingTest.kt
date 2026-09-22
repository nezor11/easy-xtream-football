package com.footballxtream.data

import com.footballxtream.model.LiveChannel
import com.footballxtream.model.Quality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelGroupingTest {

    private fun channel(name: String, category: String?, epgId: String? = null) =
        LiveChannel(
            streamId = name.hashCode(),
            name = name,
            iconUrl = null,
            categoryName = category,
            streamUrl = "http://host/${name.hashCode()}",
            epgId = epgId,
        )

    @Test
    fun build_mergesQualityVariantsOfSameChannel() {
        val groups = ChannelGrouping.build(
            listOf(
                channel("LaLiga TV HD", "Sports", epgId = "b1"),
                channel("LaLiga TV SD", "Sports", epgId = "b1"),
            ),
        )
        assertEquals(1, groups.size)
        val g = groups.first()
        assertEquals("LaLiga TV", g.displayName)
        // Variants sorted highest quality first.
        assertEquals(listOf(Quality.HD, Quality.SD), g.variants.map { it.quality })
        assertEquals(setOf(Quality.HD, Quality.SD), g.availableQualities)
        assertEquals("b1", g.epgId)
        assertTrue(g.isFootball) // via the "laliga" competition term, not any brand
    }

    @Test
    fun build_dropsNonSportsAndVod() {
        val groups = ChannelGrouping.build(
            listOf(
                channel("Canal Deporte 1", "Sports"),
                channel("CNN", "News"),
                channel("Película Top", "VOD Movies"), // sports? no, and VOD category anyway
                channel("Liga en directo", "VOD Sports"), // VOD category is dropped even if sporty
            ),
        )
        assertEquals(1, groups.size)
        assertEquals("Canal Deporte 1", groups.first().displayName)
    }

    @Test
    fun build_sameChannelKeyedAlikeFromXtreamAndM3uNaming() {
        // Favorites are keyed by ChannelGroup.key, so the two spellings one provider uses for an
        // event-only feed (parenthesised over Xtream, bare in its M3U) must yield one key.
        val fromXtream = ChannelGrouping.build(listOf(channel("CANAL DEPORTE3 (SOLO EVENTOS)", "Deportes")))
        val fromM3u = ChannelGrouping.build(listOf(channel("CANAL DEPORTE3  SOLO EVENTOS", "Deportes")))
        assertEquals(fromXtream.single().key, fromM3u.single().key)
        assertEquals("CANAL DEPORTE3", fromM3u.single().displayName)
    }

    @Test
    fun build_separatesDifferentChannelNumbers() {
        val groups = ChannelGrouping.build(
            listOf(
                channel("Canal Deporte 1 HD", "Sports"),
                channel("Canal Deporte 2 HD", "Sports"),
            ),
        )
        assertEquals(2, groups.size)
    }
}
