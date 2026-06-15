package com.footballxtream.data.remote.dto

import com.footballxtream.model.ChannelCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveCategoryDto(
    @SerialName("category_id") val categoryId: String = "",
    @SerialName("category_name") val categoryName: String = "",
) {
    fun toModel() = ChannelCategory(id = categoryId, name = categoryName.trim())
}

@Serializable
data class LiveStreamDto(
    val num: String? = null,
    val name: String = "",
    @SerialName("stream_id") val streamId: Int = 0,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("stream_type") val streamType: String? = null,
)
