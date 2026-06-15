package com.footballxtream.data

import com.footballxtream.data.remote.XtreamClient
import com.footballxtream.model.XtreamProfile

/**
 * Builds playable stream URLs following the Xtream Codes scheme.
 * Live: {server}/live/{user}/{pass}/{streamId}.{ext}
 *
 * VOD/series URLs (/movie/, /series/) will be added when those sections land.
 */
object StreamUrlBuilder {

    fun liveUrl(profile: XtreamProfile, streamId: Int, extension: String = "ts"): String {
        val base = XtreamClient.normalizeBaseUrl(profile.serverUrl).trimEnd('/')
        return "$base/live/${profile.username}/${profile.password}/$streamId.$extension"
    }
}
