package com.footballxtream.model

import kotlinx.serialization.Serializable

/**
 * A family that bundles its numbered channels, e.g. "Canal Deporte" → [Canal Deporte 1, 2, 3...].
 * A single-channel family is just a folder with one channel (shown as a normal channel card).
 */
@Serializable
data class ChannelFolder(
    val name: String,
    val iconUrl: String?,
    val isFootball: Boolean,
    val channels: List<ChannelGroup>,
    /** ISO country code shared by the folder's channels (from the M3U `tvg-id`), if known. */
    val country: String? = null,
    /** True when every channel in the folder is flagged geo-blocked. */
    val geoBlocked: Boolean = false,
) {
    val isSingle: Boolean get() = channels.size == 1
    val single: ChannelGroup get() = channels.first()
}
