package com.footballxtream.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.footballxtream.FootballXtreamApp
import com.footballxtream.R
import com.footballxtream.data.ChannelNameParser
import com.footballxtream.data.ContentRepository
import com.footballxtream.data.local.FavoriteFolderDao
import com.footballxtream.data.local.FavoriteFolderEntity
import com.footballxtream.data.local.SettingsStore
import com.footballxtream.model.ChannelFolder
import com.footballxtream.model.ChannelGroup
import com.footballxtream.model.Quality
import com.footballxtream.model.QualityMode
import com.footballxtream.player.PlaybackSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChannelRow(val titleRes: Int, val folders: List<ChannelFolder>)

/** A favorite/recent channel with the programme airing on it right now (for the "live now" strip). */
data class LiveNowItem(
    val group: ChannelGroup,
    val title: String,
    val start: Long,
    val end: Long,
)

sealed interface ChannelsUiState {
    data object Loading : ChannelsUiState
    data class Error(val messageRes: Int) : ChannelsUiState
    data class Content(
        val rows: List<ChannelRow>,
        val qualityMode: QualityMode,
        /** Up to 10 recently watched channels, most-recent first. Empty while searching. */
        val recent: List<ChannelGroup> = emptyList(),
        /** Channels marked as favorite, alphabetical. Empty while searching. */
        val favoriteChannels: List<ChannelGroup> = emptyList(),
        /** Favorites/recents with a programme on air right now ("Ahora en directo"). Empty while searching. */
        val liveNow: List<LiveNowItem> = emptyList(),
        /** Total channels in the loaded list (unfiltered), shown as the animated count. */
        val totalChannels: Int = 0,
    ) : ChannelsUiState
}

class ChannelsViewModel(
    private val repository: ContentRepository,
    private val favoriteDao: FavoriteFolderDao,
    private val settingsStore: SettingsStore,
    private val playbackSession: PlaybackSession,
) : ViewModel() {

    private sealed interface Load {
        data object Loading : Load
        data class Error(val messageRes: Int) : Load
        data class Data(val folders: List<ChannelFolder>) : Load
    }

    /** Custom/default profile name for the header ("Live sports of <name>"); null when unnamed. */
    val activeProfileName: String? = repository.activeProfileName

    private val load = MutableStateFlow<Load>(Load.Loading)

    private val _openedFolder = MutableStateFlow<ChannelFolder?>(null)
    val openedFolder: StateFlow<ChannelFolder?> = _openedFolder.asStateFlow()

    private val query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = query.asStateFlow()

    val favoriteNames: StateFlow<Set<String>> = favoriteDao.observeAll()
        .map { entities -> entities.map { it.name }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val favoriteChannelKeys: StateFlow<List<String>> = settingsStore.favoriteChannelKeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // "Live now" programmes for the favorites/recents set, computed off the main combine (it needs
    // async EPG lookups) and merged back in below.
    private val liveNowFlow = MutableStateFlow<List<LiveNowItem>>(emptyList())

    // Recents + channel favorites + live-now folded into one flow so the main combine stays within
    // 5 sources.
    private val recentsAndFavorites = combine(
        settingsStore.recentChannelKeys,
        favoriteChannelKeys,
        liveNowFlow,
    ) { recents, favorites, liveNow -> Triple(recents, favorites, liveNow) }

    val uiState: StateFlow<ChannelsUiState> =
        combine(
            load,
            favoriteNames,
            settingsStore.qualityMode,
            query,
            recentsAndFavorites,
        ) { loadState, favorites, mode, q, (recentKeys, favKeys, liveNowItems) ->
            when (loadState) {
                Load.Loading -> ChannelsUiState.Loading
                is Load.Error -> ChannelsUiState.Error(loadState.messageRes)
                is Load.Data -> ChannelsUiState.Content(
                    rows = buildRows(loadState.folders, favorites, mode, q),
                    qualityMode = mode,
                    // Only surface recent / favorite channels on the normal (unsearched) grid.
                    recent = if (q.isBlank()) {
                        recentKeys.mapNotNull { findChannel(loadState.folders, it) }
                    } else {
                        emptyList()
                    },
                    // Keep the user's chosen favorite order (no alphabetical sort).
                    favoriteChannels = if (q.isBlank()) {
                        favKeys.mapNotNull { findChannel(loadState.folders, it) }
                    } else {
                        emptyList()
                    },
                    liveNow = if (q.isBlank()) liveNowItems else emptyList(),
                    totalChannels = loadState.folders.sumOf { it.channels.size },
                )
            }
        }
            // Build the rows (filtering/sorting large lists) off the main thread to avoid UI freezes.
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChannelsUiState.Loading)

    init {
        refresh()
        observeLiveNow()
    }

    /**
     * Recomputes the "Ahora en directo" strip whenever the loaded channels or the favorites/recents
     * set change. Only the favorites+recents subset is queried (Xtream needs one EPG call per channel,
     * so polling all 722 is out of the question); [collectLatest] cancels an in-flight pass if the set
     * changes again, and the repository caches each lookup so repeated channels stay cheap.
     */
    private fun observeLiveNow() {
        viewModelScope.launch {
            combine(
                load,
                settingsStore.recentChannelKeys,
                favoriteChannelKeys,
            ) { loadState, recents, favorites -> Triple(loadState, recents, favorites) }
                .collectLatest { (loadState, recents, favorites) ->
                    val folders = (loadState as? Load.Data)?.folders
                    if (folders == null) {
                        liveNowFlow.value = emptyList()
                        return@collectLatest
                    }
                    val groups = (recents + favorites)
                        .mapNotNull { findChannel(folders, it) }
                        .distinctBy { it.key }
                    liveNowFlow.value = computeLiveNow(groups)
                }
        }
    }

    private suspend fun computeLiveNow(groups: List<ChannelGroup>): List<LiveNowItem> {
        if (groups.isEmpty()) return emptyList()
        return groups.chunked(LIVE_NOW_CONCURRENCY).flatMap { chunk ->
            coroutineScope {
                chunk.map { group ->
                    async(Dispatchers.IO) {
                        repository.nowProgram(group)
                            ?.let { LiveNowItem(group, it.title, it.start, it.end) }
                    }
                }.awaitAll()
            }
        }.filterNotNull()
    }

    fun refresh(forceRefresh: Boolean = false) {
        load.value = Load.Loading
        viewModelScope.launch {
            repository.loadLiveGroups(forceRefresh).fold(
                onSuccess = { groups ->
                    // Show channels right away (with whatever logos the playlist carries)…
                    // Grouping/sorting runs off the main thread so large lists don't freeze the UI.
                    load.value = Load.Data(withContext(Dispatchers.Default) { foldIntoFolders(groups) })
                    // …then fill missing logos in the background and refresh the grid.
                    launch {
                        val enriched = repository.enrichLogos(groups)
                        if (enriched != groups) {
                            load.value = Load.Data(withContext(Dispatchers.Default) { foldIntoFolders(enriched) })
                        }
                    }
                },
                onFailure = { load.value = Load.Error(R.string.error_load_channels) },
            )
        }
    }

    fun reload() = refresh(forceRefresh = true)

    fun selectQuality(mode: QualityMode) {
        viewModelScope.launch { settingsStore.setQualityMode(mode) }
    }

    fun openFolder(folder: ChannelFolder) {
        _openedFolder.value = folder
    }

    fun closeFolder() {
        _openedFolder.value = null
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun toggleFavorite(folder: ChannelFolder) {
        viewModelScope.launch {
            if (favoriteNames.value.contains(folder.name)) {
                favoriteDao.remove(folder.name)
            } else {
                favoriteDao.add(FavoriteFolderEntity(folder.name))
            }
        }
    }

    fun toggleFavoriteChannel(group: ChannelGroup) {
        viewModelScope.launch { settingsStore.toggleFavoriteChannel(group.key) }
    }

    /**
     * Starts playback at [folder]'s channel [index]. The whole folder list is handed to the session
     * so the player can zap channels within the folder and jump folders (quick double-press).
     */
    fun play(folder: ChannelFolder, index: Int, onReady: () -> Unit) {
        val allFolders = (load.value as? Load.Data)?.folders ?: listOf(folder)
        val folderIndex = allFolders.indexOf(folder).coerceAtLeast(0)
        playbackSession.start(allFolders, folderIndex, index)
        onReady()
    }

    /**
     * Plays [channels] as a self-contained zap context (the favorites / recents / "live now" strips),
     * positioned at [index]. The whole list becomes the player's "folder", so ◀▶ cycles within it —
     * just like opening a folder, but for the strip the user launched from.
     */
    fun playList(channels: List<ChannelGroup>, index: Int, isFavorites: Boolean, onReady: () -> Unit) {
        if (index !in channels.indices) return
        val folder = ChannelFolder(name = "", iconUrl = null, isFootball = false, channels = channels)
        playbackSession.start(listOf(folder), folderIndex = 0, channelIndex = index, isFavoritesList = isFavorites)
        onReady()
    }

    /** Moves a favorite channel by [delta] positions (-1 left, +1 right). */
    fun moveFavorite(key: String, delta: Int) {
        viewModelScope.launch { settingsStore.moveFavoriteChannel(key, delta) }
    }

    private fun findChannel(folders: List<ChannelFolder>, key: String?): ChannelGroup? {
        if (key == null) return null
        folders.forEach { folder ->
            folder.channels.firstOrNull { it.key == key }?.let { return it }
        }
        return null
    }

    private fun foldIntoFolders(groups: List<ChannelGroup>): List<ChannelFolder> =
        groups.groupBy { ChannelNameParser.folderKey(it.displayName) }
            .map { (_, members) ->
                val sorted = members.sortedBy { channelNumber(it.displayName) }
                ChannelFolder(
                    name = ChannelNameParser.commonFolderName(sorted.map { it.displayName }),
                    iconUrl = sorted.firstNotNullOfOrNull { it.iconUrl },
                    isFootball = sorted.any { it.isFootball },
                    channels = sorted,
                    country = sorted.firstNotNullOfOrNull { it.country },
                    geoBlocked = sorted.all { it.geoBlocked },
                )
            }
            .sortedBy { it.name.lowercase() }

    private fun channelNumber(displayName: String): Int =
        Regex("""(\d{1,3})\s*$""").find(displayName.trim())?.groupValues?.get(1)?.toIntOrNull() ?: 0

    private fun buildRows(
        folders: List<ChannelFolder>,
        favorites: Set<String>,
        mode: QualityMode,
        query: String,
    ): List<ChannelRow> {
        val fixed = mode.fixedQuality
        val byQuality = if (fixed == null) {
            folders
        } else {
            // A channel with no quality tag (UNKNOWN) is unclassified, not "not this quality", so it
            // stays visible under every fixed filter. Otherwise free lists whose channels carry no
            // quality in the name (e.g. Free-TV) would look empty under SD/HD/FHD/4K.
            folders.filter { folder ->
                folder.channels.any {
                    it.availableQualities.contains(fixed) ||
                        it.availableQualities.contains(Quality.UNKNOWN)
                }
            }
        }

        val needle = query.trim()
        val visible = if (needle.isBlank()) {
            byQuality
        } else {
            byQuality.filter { it.name.contains(needle, ignoreCase = true) }
        }

        fun isFavorite(folder: ChannelFolder) = favorites.contains(folder.name)

        val favoriteFolders = visible.filter(::isFavorite)
        val footballFolders = visible.filter { it.isFootball && !isFavorite(it) }
        val otherFolders = visible.filter { !it.isFootball && !isFavorite(it) }

        return buildList {
            if (favoriteFolders.isNotEmpty()) add(ChannelRow(R.string.section_favorite_folders, favoriteFolders))
            if (footballFolders.isNotEmpty()) add(ChannelRow(R.string.section_football, footballFolders))
            if (otherFolders.isNotEmpty()) add(ChannelRow(R.string.section_more_sports, otherFolders))
        }
    }

    companion object {
        /** Max concurrent EPG lookups while building the "live now" strip. */
        private const val LIVE_NOW_CONCURRENCY = 6

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FootballXtreamApp).container
                ChannelsViewModel(
                    container.repository,
                    container.favoriteFolderDao,
                    container.settingsStore,
                    container.playbackSession,
                )
            }
        }
    }
}
