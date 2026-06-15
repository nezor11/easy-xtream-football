package com.footballxtream.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Builds a [XtreamApi] bound to a given panel base URL. */
object XtreamClient {

    /**
     * Player User-Agent. IPTV panels/CDNs gate by User-Agent: this provider's panel rejects the
     * default OkHttp UA, and its stream CDN rejects browser UAs (401) — but a media-player UA like
     * VLC is accepted everywhere (login, M3U and stream). Used for all requests, API and playback.
     */
    const val USER_AGENT: String = "VLC/3.0.20 LibVLC/3.0.20"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    /** Ensures a scheme is present and the URL ends with a single trailing slash. */
    fun normalizeBaseUrl(raw: String): String {
        var url = raw.trim()
        if (url.isEmpty()) return url
        if (!url.startsWith("http://", ignoreCase = true) &&
            !url.startsWith("https://", ignoreCase = true)
        ) {
            url = "http://$url"
        }
        return url.trimEnd('/') + "/"
    }

    fun create(serverUrl: String): XtreamApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(serverUrl))
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(XtreamApi::class.java)
    }

    /** Blocking GET that returns the response body as text (uses the shared UA'd client). */
    fun fetchText(url: String): String {
        val request = Request.Builder().url(url.trim()).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    /** Reads only the first line of the response — cheap way to validate a (possibly huge) M3U. */
    fun fetchFirstLine(url: String): String {
        val request = Request.Builder().url(url.trim()).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            return response.body?.source()?.readUtf8Line().orEmpty()
        }
    }

    /**
     * Streams a GET body to [block] without buffering it all in memory — used to parse large XMLTV
     * EPG files. The response stays open for the duration of [block]; do not leak the stream.
     */
    fun <T> withStream(url: String, block: (java.io.InputStream) -> T): T {
        val request = Request.Builder().url(url.trim()).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty body")
            return block(body.byteStream())
        }
    }
}
