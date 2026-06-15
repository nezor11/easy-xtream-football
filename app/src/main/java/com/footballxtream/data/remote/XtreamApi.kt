package com.footballxtream.data.remote

import com.footballxtream.data.remote.dto.LiveCategoryDto
import com.footballxtream.data.remote.dto.LiveStreamDto
import com.footballxtream.data.remote.dto.LoginResponse
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamApi {

    @GET("player_api.php")
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String,
    ): LoginResponse

    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories",
    ): List<LiveCategoryDto>

    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String? = null,
        @Query("action") action: String = "get_live_streams",
    ): List<LiveStreamDto>

    // EPG endpoints return either {"epg_listings":[...]} or a bare [...] array depending on the
    // panel, so they're parsed as a raw JsonElement (see ContentRepository.shortEpg).
    @GET("player_api.php")
    suspend fun getShortEpg(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("stream_id") streamId: Int,
        @Query("limit") limit: Int = 8,
        @Query("action") action: String = "get_short_epg",
    ): JsonElement

    @GET("player_api.php")
    suspend fun getSimpleDataTable(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("stream_id") streamId: Int,
        @Query("action") action: String = "get_simple_data_table",
    ): JsonElement
}
