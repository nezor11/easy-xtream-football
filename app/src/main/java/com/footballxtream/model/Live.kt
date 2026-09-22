package com.footballxtream.model

import kotlinx.serialization.Serializable

/** A category from the Xtream API (used only while parsing Xtream responses). */
data class ChannelCategory(
    val id: String,
    val name: String,
)

/**
 * A single playable live channel, independent of the source (Xtream API or M3U playlist).
 * [streamUrl] is the directly playable URL. [streamId] is the source's own id (Xtream stream id, or a
 * hash of the M3U URL) and is NOT what favorites are keyed by: those use [ChannelGroup.key], derived
 * from the normalized name, so they survive switching between profiles of the same provider.
 */
@Serializable
data class LiveChannel(
    val streamId: Int,
    val name: String,
    val iconUrl: String?,
    val categoryName: String?,
    val streamUrl: String,
    /** XMLTV channel id (M3U `tvg-id`), used to look up the guide for M3U sources. */
    val epgId: String? = null,
    /** ISO country code of the channel's origin (from the M3U `tvg-id` suffix), if known. */
    val country: String? = null,
    /** True when the playlist flags the channel as geo-blocked (iptv-org's "[Geo-blocked]" tag). */
    val geoBlocked: Boolean = false,
)
