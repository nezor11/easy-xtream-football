package com.footballxtream.model

import kotlinx.serialization.Serializable

/**
 * Stream quality tier parsed from a channel name. [typicalBitrateBps] is a rough estimate of the
 * bandwidth each tier needs, used by the Auto mode to pick a variant that fits the measured network.
 */
@Serializable
enum class Quality(
    val rank: Int,
    val label: String,
    val typicalBitrateBps: Long,
) {
    UHD_4K(5, "4K", 20_000_000),
    QHD(4, "2K", 10_000_000),
    FHD(3, "FHD", 6_000_000),
    HD(2, "HD", 3_000_000),
    SD(1, "SD", 1_200_000),
    UNKNOWN(0, "SD", 1_500_000);

    companion object {
        /** Selectable tiers, highest first. */
        val tiers: List<Quality> = listOf(UHD_4K, QHD, FHD, HD, SD)
    }
}
