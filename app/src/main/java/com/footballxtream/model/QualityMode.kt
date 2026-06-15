package com.footballxtream.model

/** Quality filter selected by the user (the top chips). */
enum class QualityMode(val label: String) {
    AUTO("Auto"),
    UHD_4K("4K"),
    FHD("FHD"),
    HD("HD"),
    SD("SD");

    /** The forced tier for fixed modes, or null for AUTO (no filter, adaptive quality). */
    val fixedQuality: Quality?
        get() = when (this) {
            UHD_4K -> Quality.UHD_4K
            FHD -> Quality.FHD
            HD -> Quality.HD
            SD -> Quality.SD
            AUTO -> null
        }
}
