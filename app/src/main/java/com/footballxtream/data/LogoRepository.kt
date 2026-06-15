package com.footballxtream.data

import android.util.Log
import com.footballxtream.data.remote.XtreamClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Resolves channel logos from the public iptv-org database (channels.json + logos.json), which is
 * cross-referenced into a normalized name -> logo URL map and cached compactly on disk. Used to give
 * channels a logo when the playlist itself provides none. Coil handles caching the actual images.
 */
class LogoRepository(private val cacheDir: File) {

    @Volatile
    private var nameToLogo: Map<String, String>? = null

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        if (nameToLogo != null) return@withContext
        nameToLogo = runCatching { loadFromCache() }.getOrNull()
            ?: runCatching { buildAndCache() }.getOrElse {
                Log.w(TAG, "logo map build failed", it)
                emptyMap()
            }
    }

    fun logoFor(name: String): String? {
        val key = normalize(name)
        if (key.isBlank()) return null
        return nameToLogo?.get(key)
    }

    private fun loadFromCache(): Map<String, String>? {
        val file = mapFile()
        if (!file.exists() || System.currentTimeMillis() - file.lastModified() > CACHE_TTL_MS) {
            return null
        }
        val cached = json.decodeFromString<Map<String, String>>(file.readText())
        return cached.ifEmpty { null }
    }

    private fun buildAndCache(): Map<String, String> {
        val channels = json.decodeFromString<List<IptvChannel>>(XtreamClient.fetchText(CHANNELS_URL))
        val logos = json.decodeFromString<List<IptvLogo>>(XtreamClient.fetchText(LOGOS_URL))

        val logoById = HashMap<String, String>(logos.size)
        logos.forEach { logo ->
            if (logo.url.isNotBlank()) logoById.putIfAbsent(logo.channel, logo.url)
        }

        val map = HashMap<String, String>()
        channels.forEach { channel ->
            val url = logoById[channel.id] ?: return@forEach
            (listOf(channel.name) + channel.altNames).forEach { candidate ->
                val key = normalize(candidate)
                if (key.isNotBlank()) map.putIfAbsent(key, url)
            }
        }
        runCatching { mapFile().writeText(json.encodeToString(map)) }
        return map
    }

    private fun mapFile() = File(cacheDir, "logo_map_v$CACHE_VERSION.json")

    private fun normalize(name: String): String =
        name.lowercase().filter { it.isLetterOrDigit() }

    @Serializable
    private data class IptvChannel(
        val id: String = "",
        val name: String = "",
        @SerialName("alt_names") val altNames: List<String> = emptyList(),
    )

    @Serializable
    private data class IptvLogo(
        val channel: String = "",
        val url: String = "",
    )

    private companion object {
        const val TAG = "FXLogo"
        const val CACHE_VERSION = 1
        const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
        const val CHANNELS_URL = "https://iptv-org.github.io/api/channels.json"
        const val LOGOS_URL = "https://iptv-org.github.io/api/logos.json"
    }
}
