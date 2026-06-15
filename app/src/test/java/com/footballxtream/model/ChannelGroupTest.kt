package com.footballxtream.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelGroupTest {

    private fun variant(q: Quality) = ChannelVariant(
        channel = LiveChannel(
            streamId = q.ordinal,
            name = "Canal ${q.label}",
            iconUrl = null,
            categoryName = null,
            streamUrl = "http://host/${q.name}",
        ),
        quality = q,
    )

    /** Built the way ChannelGrouping does: variants sorted highest quality first. */
    private fun group(vararg qualities: Quality) = ChannelGroup(
        key = "k",
        displayName = "Canal",
        iconUrl = null,
        isFootball = false,
        variants = qualities.map(::variant).sortedByDescending { it.quality.rank },
    )

    @Test
    fun bestVariant_isHighestQuality() {
        assertEquals(Quality.FHD, group(Quality.SD, Quality.FHD, Quality.HD).bestVariant().quality)
    }

    @Test
    fun variantFor_returnsExactOrNull() {
        val g = group(Quality.FHD, Quality.HD, Quality.SD)
        assertEquals(Quality.HD, g.variantFor(Quality.HD)?.quality)
        assertNull(g.variantFor(Quality.UHD_4K))
    }

    @Test
    fun variantAtOrBelow_picksHighestNotAbove() {
        val g = group(Quality.FHD, Quality.HD, Quality.SD)
        // Requesting 4K (none): take the highest available at or below → FHD.
        assertEquals(Quality.FHD, g.variantAtOrBelow(Quality.UHD_4K).quality)
        assertEquals(Quality.HD, g.variantAtOrBelow(Quality.HD).quality)
        assertEquals(Quality.SD, g.variantAtOrBelow(Quality.SD).quality)
    }

    @Test
    fun variantAtOrBelow_fallsBackToLowestWhenAllAreHigher() {
        // Only FHD available but SD requested → fall back to the lowest (only) variant.
        assertEquals(Quality.FHD, group(Quality.FHD).variantAtOrBelow(Quality.SD).quality)
    }
}
