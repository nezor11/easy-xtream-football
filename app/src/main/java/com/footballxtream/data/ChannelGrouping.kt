package com.footballxtream.data

import com.footballxtream.model.ChannelGroup
import com.footballxtream.model.ChannelVariant
import com.footballxtream.model.LiveChannel

/**
 * Turns a flat list of channels (from any source) into sports-only, quality-deduplicated groups.
 * Reused by both the Xtream and M3U sources.
 */
object ChannelGrouping {

    fun build(channels: List<LiveChannel>): List<ChannelGroup> {
        val sports = channels.filter {
            !ChannelNameParser.isVodCategory(it.categoryName) &&
                ChannelNameParser.isSports(it.name, it.categoryName)
        }

        return sports
            .groupBy { ChannelNameParser.groupKey(it.name).ifBlank { it.name.lowercase() } }
            .map { (key, group) ->
                val variants = group
                    .map { ChannelVariant(it, ChannelNameParser.quality(it.name)) }
                    .sortedByDescending { it.quality.rank }
                val representative = variants.first().channel
                ChannelGroup(
                    key = key,
                    displayName = ChannelNameParser.baseName(representative.name)
                        .ifBlank { representative.name },
                    iconUrl = variants.firstNotNullOfOrNull { it.channel.iconUrl },
                    isFootball = group.any {
                        ChannelNameParser.isFootball(it.name, it.categoryName)
                    },
                    variants = variants,
                    epgId = variants.firstNotNullOfOrNull { it.channel.epgId },
                    country = variants.firstNotNullOfOrNull { it.channel.country },
                    geoBlocked = variants.all { it.channel.geoBlocked },
                )
            }
            .sortedBy { it.displayName.lowercase() }
    }
}
