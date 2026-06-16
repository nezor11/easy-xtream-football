package com.footballxtream.data

import android.util.Log
import com.footballxtream.data.remote.XtreamApi
import com.footballxtream.data.remote.XtreamClient
import com.footballxtream.data.remote.dto.EpgListingDto
import com.footballxtream.data.local.ProfileEntity
import com.footballxtream.model.ChannelGroup
import com.footballxtream.model.ChannelVariant
import com.footballxtream.model.EpgProgram
import com.footballxtream.model.LiveChannel
import com.footballxtream.model.Quality
import com.footballxtream.model.XtreamProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File
import java.io.IOException

class XtreamAuthException : Exception("Authentication rejected by the server")

/**
 * Single entry point for channel data. Holds the active source (Xtream API or an M3U playlist)
 * and exposes sports-grouped channels regardless of the source.
 */
class ContentRepository(
    private val cacheDir: File,
    private val logoRepository: LogoRepository,
) {

    private sealed interface Binding {
        data class Xtream(val profile: XtreamProfile, val api: XtreamApi) : Binding
        data class M3u(val url: String) : Binding
        data class Direct(val url: String) : Binding
    }

    @Volatile
    private var binding: Binding? = null

    /** Display name of the active source (profile name), shown as the channels screen header. */
    @Volatile
    var activeProfileName: String? = null
        private set

    fun setActiveProfileName(name: String) {
        activeProfileName = name.takeIf { it.isNotBlank() }
    }

    private val groupsJson = Json { ignoreUnknownKeys = true }

    // --- XMLTV EPG state for the active M3U source (Xtream uses get_short_epg instead) ---
    @Volatile private var m3uSourceKey: String = ""
    @Volatile private var m3uEpgUrls: List<String> = emptyList()
    @Volatile private var m3uNeededEpgIds: Set<String> = emptySet()
    @Volatile private var m3uEpgIndex: Map<String, List<EpgProgram>>? = null
    @Volatile private var m3uEpgBuiltKey: String = ""
    @Volatile private var m3uEpgBuiltAt: Long = 0L
    private val epgMutex = Mutex()

    // --- Restore a saved profile without a network round-trip ---

    fun bindXtream(profile: XtreamProfile) {
        binding = Binding.Xtream(profile, XtreamClient.create(profile.serverUrl))
    }

    fun bindM3u(url: String) {
        binding = Binding.M3u(url.trim())
    }

    fun bindDirect(url: String) {
        binding = Binding.Direct(url.trim())
    }

    /** Binds whichever source a saved profile points to and records its name for the header. */
    fun bindProfile(profile: ProfileEntity) {
        activeProfileName = profile.name.takeIf { it.isNotBlank() }
        when {
            profile.isDirect -> bindDirect(profile.m3uUrl.value)
            profile.isM3u -> bindM3u(profile.m3uUrl.value)
            else -> bindXtream(profile.toXtreamProfile())
        }
    }

    // --- Validate credentials/URL when adding a profile (binds on success) ---

    suspend fun validateXtream(profile: XtreamProfile): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val api = XtreamClient.create(profile.serverUrl)
            val response = api.login(profile.username, profile.password)
            val info = response.userInfo
            if (info == null || info.auth != 1) throw XtreamAuthException()
            binding = Binding.Xtream(profile, api)
        }
    }

    suspend fun validateM3u(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val firstLine = XtreamClient.fetchFirstLine(url)
            if (!firstLine.trimStart().startsWith("#EXTM3U", ignoreCase = true)) {
                throw IOException("La URL no devuelve una lista M3U válida (inicio: ${firstLine.take(60)})")
            }
            binding = Binding.M3u(url.trim())
        }.onFailure { Log.w(TAG, "validateM3u failed for $url", it) }
    }

    // --- Load the sports channel groups for the active source ---

    suspend fun loadLiveGroups(forceRefresh: Boolean = false): Result<List<ChannelGroup>> =
        withContext(Dispatchers.IO) {
            runCatching {
                when (val current = binding) {
                    is Binding.Xtream -> loadXtreamGroups(current, forceRefresh)
                    is Binding.M3u -> loadM3uGroups(current.url, forceRefresh)
                    is Binding.Direct -> loadDirect(current.url)
                    null -> error("No active content source")
                }
            }.onFailure { Log.w(TAG, "loadLiveGroups failed", it) }
        }

    /**
     * Sports channel groups for an M3U source. The parsed+grouped result is cached on disk so repeat
     * loads skip both the 11 MB download and the parse of thousands of entries (near-instant).
     */
    private suspend fun loadM3uGroups(url: String, forceRefresh: Boolean): List<ChannelGroup> {
        val cacheFile = File(cacheDir, "groups_v$CACHE_VERSION-${url.hashCode()}.json")
        if (!forceRefresh && cacheFile.exists() &&
            System.currentTimeMillis() - cacheFile.lastModified() < CACHE_TTL_MS
        ) {
            runCatching { groupsJson.decodeFromString<M3uCache>(cacheFile.readText()) }
                .getOrNull()
                ?.takeIf { it.groups.isNotEmpty() }
                ?.let { return it.groups.also { g -> bindEpgSource(url, it.epgUrls, g) } }
        }
        val content = XtreamClient.fetchText(url)
        val groups = ChannelGrouping.build(M3uParser.parse(content))
        val epgUrls = M3uParser.epgUrls(content)
        runCatching { cacheFile.writeText(groupsJson.encodeToString(M3uCache(epgUrls, groups))) }
        bindEpgSource(url, epgUrls, groups)
        return groups
    }

    /** Records the EPG source for the active M3U so [epgFor] can build the guide on demand. */
    private fun bindEpgSource(url: String, epgUrls: List<String>, groups: List<ChannelGroup>) {
        m3uSourceKey = url
        m3uEpgUrls = epgUrls
        m3uNeededEpgIds = groups.mapNotNull { it.epgId }.toSet()
    }

    /**
     * Fills in a logo for channels that don't carry one, using the iptv-org logo database.
     * Kept separate from [loadLiveGroups] so the grid can render immediately while this — which may
     * download the ~17 MB iptv-org database on a cold cache — runs in the background.
     */
    suspend fun enrichLogos(groups: List<ChannelGroup>): List<ChannelGroup> =
        withContext(Dispatchers.IO) {
            logoRepository.ensureLoaded()
            groups.map { group ->
                if (!group.iconUrl.isNullOrBlank()) {
                    group
                } else {
                    logoRepository.logoFor(group.displayName)?.let { group.copy(iconUrl = it) } ?: group
                }
            }
        }

    /**
     * "Now / next" EPG for a channel, whatever the source: Xtream via get_short_epg, M3U via the
     * XMLTV guide declared in the playlist (x-tvg-url). Returns empty when no guide is available.
     */
    suspend fun epgFor(group: ChannelGroup): List<EpgProgram> =
        when (binding) {
            is Binding.Xtream -> shortEpg(group.bestVariant().channel.streamId)
            is Binding.M3u -> withContext(Dispatchers.IO) { m3uEpg(group) }
            is Binding.Direct, null -> emptyList()
        }

    private suspend fun m3uEpg(group: ChannelGroup): List<EpgProgram> {
        val id = group.epgId ?: return emptyList()
        return ensureEpgIndex()?.get(id).orEmpty()
    }

    /**
     * Builds (once, then cached with a TTL) the XMLTV "now/next" index for the channels of the
     * active M3U source. The first guide request pays the download/parse; later ones are instant.
     */
    private suspend fun ensureEpgIndex(): Map<String, List<EpgProgram>>? {
        val urls = m3uEpgUrls
        val ids = m3uNeededEpgIds
        val key = m3uSourceKey
        if (urls.isEmpty() || ids.isEmpty()) return null
        m3uEpgIndex?.let { if (m3uEpgBuiltKey == key && isEpgFresh()) return it }
        return epgMutex.withLock {
            m3uEpgIndex?.let { if (m3uEpgBuiltKey == key && isEpgFresh()) return it }
            val built = XmltvEpg.index(urls, ids)
            m3uEpgIndex = built
            m3uEpgBuiltKey = key
            m3uEpgBuiltAt = System.currentTimeMillis()
            built
        }
    }

    private fun isEpgFresh(): Boolean =
        System.currentTimeMillis() - m3uEpgBuiltAt < EPG_TTL_MS

    /**
     * "Now / next" EPG for a stream. Only Xtream sources expose it (via get_short_epg); M3U returns
     * empty. Any server quirk is swallowed so the player simply shows no guide instead of failing.
     */
    suspend fun shortEpg(streamId: Int): List<EpgProgram> = withContext(Dispatchers.IO) {
        val current = binding as? Binding.Xtream ?: return@withContext emptyList()
        val user = current.profile.username
        val pass = current.profile.password
        runCatching {
            parseEpg(current.api.getShortEpg(user, pass, streamId))
                .ifEmpty { parseEpg(current.api.getSimpleDataTable(user, pass, streamId)) }
        }.onFailure { Log.w(TAG, "shortEpg failed for $streamId", it) }.getOrDefault(emptyList())
    }

    /** Reads listings from either `{"epg_listings":[...]}` or a bare `[...]` array. */
    private fun parseEpg(element: JsonElement): List<EpgProgram> {
        val listings = when (element) {
            is JsonArray -> element
            is JsonObject -> element["epg_listings"] as? JsonArray ?: return emptyList()
            else -> return emptyList()
        }
        return listings
            .mapNotNull {
                runCatching { groupsJson.decodeFromJsonElement<EpgListingDto>(it) }.getOrNull()?.toModel()
            }
            .sortedBy { it.start }
    }

    /**
     * Sports groups for an Xtream source, cached on disk like the M3U path. The API's get_live_streams
     * returns thousands of channels and there's no incremental fetch, so a cold load takes many
     * seconds; the cache makes every later open near-instant (the ↻ Recargar button forces a refresh).
     */
    private suspend fun loadXtreamGroups(binding: Binding.Xtream, forceRefresh: Boolean): List<ChannelGroup> {
        val key = (binding.profile.serverUrl + "|" + binding.profile.username).hashCode()
        val cacheFile = File(cacheDir, "groups_xt_v$CACHE_VERSION-$key.json")
        if (!forceRefresh && cacheFile.exists() &&
            System.currentTimeMillis() - cacheFile.lastModified() < CACHE_TTL_MS
        ) {
            runCatching { groupsJson.decodeFromString<List<ChannelGroup>>(cacheFile.readText()) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }
        val groups = loadXtream(binding)
        runCatching { cacheFile.writeText(groupsJson.encodeToString(groups)) }
        return groups
    }

    /** A single directly-playable stream (HLS/DASH) wrapped as one group, with no sports filtering. */
    private fun loadDirect(url: String): List<ChannelGroup> {
        val host = Regex("""https?://([^/:]+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.removePrefix("www.").orEmpty()
        val name = host.ifBlank { "Direct" }
        val channel = LiveChannel(
            streamId = url.hashCode(),
            name = name,
            iconUrl = null,
            categoryName = null,
            streamUrl = url,
        )
        return listOf(
            ChannelGroup(
                key = "direct-${url.hashCode()}",
                displayName = name,
                iconUrl = null,
                isFootball = false,
                variants = listOf(ChannelVariant(channel, Quality.UNKNOWN)),
            ),
        )
    }

    private suspend fun loadXtream(binding: Binding.Xtream): List<ChannelGroup> {
        val profile = binding.profile
        val api = binding.api
        val categoryName = api.getLiveCategories(profile.username, profile.password)
            .associate { it.categoryId to it.categoryName.trim() }
        val channels = api.getLiveStreams(profile.username, profile.password).map { stream ->
            LiveChannel(
                streamId = stream.streamId,
                name = stream.name.trim(),
                iconUrl = stream.streamIcon?.takeIf { it.isNotBlank() },
                categoryName = categoryName[stream.categoryId],
                streamUrl = StreamUrlBuilder.liveUrl(profile, stream.streamId),
            )
        }
        return ChannelGrouping.build(channels)
    }

    private companion object {
        const val TAG = "FXContent"
        const val CACHE_TTL_MS = 12L * 60 * 60 * 1000 // 12 h
        const val CACHE_VERSION = 17 // bump when parsing/filtering/grouping logic or cache shape changes
        const val EPG_TTL_MS = 2L * 60 * 60 * 1000 // 2 h — rebuild the XMLTV index at most this often
    }
}

/** On-disk cache for an M3U source: the EPG URLs from its header plus the grouped sports channels. */
@Serializable
private data class M3uCache(
    val epgUrls: List<String> = emptyList(),
    val groups: List<ChannelGroup>,
)
