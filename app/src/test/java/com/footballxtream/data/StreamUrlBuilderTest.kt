package com.footballxtream.data

import com.footballxtream.data.remote.XtreamClient
import com.footballxtream.model.XtreamProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamUrlBuilderTest {

    @Test
    fun normalizeBaseUrl_addsSchemeAndSingleTrailingSlash() {
        assertEquals("http://host:8080/", XtreamClient.normalizeBaseUrl("host:8080"))
        assertEquals("http://host:8080/", XtreamClient.normalizeBaseUrl("http://host:8080"))
        assertEquals("http://host:8080/", XtreamClient.normalizeBaseUrl("http://host:8080///"))
        assertEquals("https://host/", XtreamClient.normalizeBaseUrl("https://host/"))
    }

    @Test
    fun liveUrl_followsXtreamScheme() {
        val profile = XtreamProfile(name = "p", serverUrl = "http://host:8080", username = "u", password = "p")
        assertEquals("http://host:8080/live/u/p/123.ts", StreamUrlBuilder.liveUrl(profile, 123))
    }

    @Test
    fun liveUrl_honoursExtensionAndNormalizesServer() {
        val profile = XtreamProfile(name = "p", serverUrl = "host:8080/", username = "u", password = "x")
        assertEquals("http://host:8080/live/u/x/9.m3u8", StreamUrlBuilder.liveUrl(profile, 9, "m3u8"))
    }
}
