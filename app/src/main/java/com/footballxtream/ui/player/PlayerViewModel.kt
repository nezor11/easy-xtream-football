package com.footballxtream.ui.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.footballxtream.FootballXtreamApp
import com.footballxtream.R
import com.footballxtream.data.ContentRepository
import com.footballxtream.data.local.SettingsStore
import com.footballxtream.model.ChannelGroup
import com.footballxtream.model.ChannelVariant
import com.footballxtream.model.EpgProgram
import com.footballxtream.model.Quality
import com.footballxtream.player.PlaybackSession
import com.footballxtream.player.PlayerEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val channelName: String = "",
    /** Currently selected emission type ("Auto" or a quality label) — shown as the overlay select. */
    val emissionLabel: String = "Auto",
    val throughputMbps: Double = 0.0,
    val resolution: String? = null,
    val isBuffering: Boolean = true,
    val menuOpen: Boolean = false,
    /** Localized label of the OK-menu section currently shown (Quality / Audio / Subtitles / Guide). */
    val menuSection: String = "",
    val menuOptions: List<String> = emptyList(),
    val menuSelectedIndex: Int = 0,
    /** "Now / next" programme titles from EPG (Xtream API or M3U's XMLTV), null when unavailable. */
    val nowProgram: String? = null,
    val nextProgram: String? = null,
    /** Set when every variant of the channel failed to load (e.g. a dead stream). */
    val errorMessage: String? = null,
    /** Transient toast-like message (e.g. "only one quality"), auto-cleared after a moment. */
    val notice: String? = null,
    /** Whether to show the on-screen controls legend (only the first few times). */
    val showControlsHint: Boolean = false,
    /** Whether the channel currently playing is marked as a favorite. */
    val isFavorite: Boolean = false,
)

@OptIn(UnstableApi::class)
class PlayerViewModel(
    private val playbackSession: PlaybackSession,
    private val settingsStore: SettingsStore,
    private val playerEngine: PlayerEngine,
    private val repository: ContentRepository,
    private val context: Context,
) : ViewModel() {

    val canPlay: Boolean = playbackSession.current != null

    val player: ExoPlayer = playerEngine.build()

    private val _ui = MutableStateFlow(PlayerUiState())
    val ui: StateFlow<PlayerUiState> = _ui.asStateFlow()

    private var currentGroup: ChannelGroup? = null
    private var currentQuality: Quality = Quality.UNKNOWN

    /** Keys of favorite channels, kept in sync so the overlay star and toggle reflect the latest. */
    @Volatile
    private var favoriteKeys: Set<String> = emptySet()

    /** Full EPG of the current channel, for the "Guía" menu section (now + upcoming). */
    private var currentEpg: List<EpgProgram> = emptyList()

    /**
     * Manual emission-type override chosen in the player; null = Auto (pick by measured network).
     * It is sticky: once the user fixes a quality, zapping to another channel keeps that choice.
     */
    private var selectedEmission: Quality? = null
    private var rebufferCount = 0
    private var hasStartedOnce = false
    private var noticeJob: Job? = null

    /** Watchdog that fires if the current channel never starts; drives the auto-skip. */
    private var failoverJob: Job? = null

    /** Consecutive channels auto-skipped because they were dead; reset on a successful start. */
    private var autoSkipCount = 0

    /** Direction of the last manual zap (+1 forward, -1 backward); auto-skip follows it. */
    private var lastDirection = 1

    /** Timestamps of the last left/right press, to detect a quick double-press (folder jump). */
    private var lastNextPressAt = 0L
    private var lastPrevPressAt = 0L

    /** False while the app is backgrounded: nothing may start playback or fail over then. */
    @Volatile
    private var foreground = true

    /** Bytes seen at the previous stats tick, to derive the instantaneous download rate. */
    private var lastBytesTransferred = 0L
    private var smoothedThroughputMbps = 0.0

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            _ui.update { it.copy(isBuffering = state == Player.STATE_BUFFERING) }
            when (state) {
                Player.STATE_READY -> {
                    hasStartedOnce = true
                    rebufferCount = 0
                    autoSkipCount = 0
                    failoverJob?.cancel()
                    // Record in "recent" only once the channel actually starts, so channels skipped
                    // past (dead ones, or the intermediate channel of a folder double-press) don't
                    // pollute the history.
                    currentGroup?.let { g -> viewModelScope.launch { settingsStore.pushRecentChannel(g.key) } }
                    _ui.update { it.copy(errorMessage = null) }
                }
                Player.STATE_BUFFERING -> {
                    // Only auto-adapt while on Auto; a manual emission type is respected as-is.
                    if (hasStartedOnce && selectedEmission == null) {
                        rebufferCount++
                        if (rebufferCount >= 3) downshift()
                    }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            // Pull the HTTP status out of the error chain so 403/401 (geo/IP block, no access) can be
            // told apart from a plain dead stream.
            val code = generateSequence(error.cause) { it.cause }
                .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
                .firstOrNull()?.responseCode
            handleChannelFailure(httpCode = code)
        }
    }

    init {
        val first = playbackSession.current
        if (first != null) {
            player.addListener(listener)
            // The first few times the player opens, briefly reveal the controls legend, then fade
            // it out on its own (the screen handles the animation).
            viewModelScope.launch {
                if (settingsStore.playerHintsShown() < MAX_CONTROLS_HINTS) {
                    settingsStore.incrementPlayerHintsShown()
                    _ui.update { it.copy(showControlsHint = true) }
                    delay(CONTROLS_HINT_MS)
                    _ui.update { it.copy(showControlsHint = false) }
                }
            }
            viewModelScope.launch {
                // Seed the in-player select from the quality mode picked on the channels screen.
                selectedEmission = settingsStore.qualityMode.first().fixedQuality
                playGroup(first)
                pollStats()
            }
            // Keep the favorite state in sync with what's stored, for the overlay star.
            viewModelScope.launch {
                settingsStore.favoriteChannelKeys.collect { keys ->
                    favoriteKeys = keys
                    _ui.update { it.copy(isFavorite = currentGroup?.key in keys) }
                }
            }
        }
    }

    // --- Channel zapping: left/right; a quick double-press jumps to the next/previous folder ---

    fun nextChannel() {
        if (_ui.value.menuOpen) return
        val now = System.currentTimeMillis()
        val jumpFolder = now - lastNextPressAt < DOUBLE_PRESS_MS
        lastNextPressAt = now
        val folderBefore = playbackSession.folderIndex
        val group = (if (jumpFolder) playbackSession.nextFolder() else playbackSession.next()) ?: return
        lastDirection = 1
        autoSkipCount = 0 // a manual zap gets a fresh auto-skip budget
        // Announce the folder whenever it changed — by a double-press jump or a single step out of a
        // single-channel folder.
        if (playbackSession.folderIndex != folderBefore) showNotice("▸ ${playbackSession.currentFolderName}")
        viewModelScope.launch { playGroup(group) }
    }

    fun previousChannel() {
        if (_ui.value.menuOpen) return
        val now = System.currentTimeMillis()
        val jumpFolder = now - lastPrevPressAt < DOUBLE_PRESS_MS
        lastPrevPressAt = now
        val folderBefore = playbackSession.folderIndex
        val group =
            (if (jumpFolder) playbackSession.previousFolder() else playbackSession.previous()) ?: return
        lastDirection = -1
        autoSkipCount = 0
        if (playbackSession.folderIndex != folderBefore) showNotice("◂ ${playbackSession.currentFolderName}")
        viewModelScope.launch { playGroup(group) }
    }

    // --- Quality stepping: up/down change emission type directly (no menu) ---

    /**
     * Steps the emission type by [delta] in the same order as the OK menu ([emissionOptions]):
     * negative moves up the list (towards Auto / higher quality), positive moves down. If the channel
     * carries a single quality there is nothing to switch to, so it just flashes a notice.
     */
    fun stepQuality(delta: Int) {
        if (_ui.value.menuOpen) return
        val group = currentGroup ?: return
        val options = emissionOptions(group)
        if (options.count { it != null } < 2) {
            showNotice(context.getString(R.string.player_only_one_quality))
            return
        }
        val currentIndex = options.indexOf(selectedEmission).coerceAtLeast(0)
        val newIndex = (currentIndex + delta).coerceIn(0, options.lastIndex)
        if (newIndex == currentIndex) return
        selectedEmission = options[newIndex]
        rebufferCount = 0
        viewModelScope.launch {
            val variant = variantForEmission(group)
            currentQuality = variant.quality
            _ui.update { it.copy(emissionLabel = emissionLabel(selectedEmission), isBuffering = true) }
            playUri(variant)
        }
    }

    private fun showNotice(text: String) {
        noticeJob?.cancel()
        _ui.update { it.copy(notice = text) }
        noticeJob = viewModelScope.launch {
            delay(NOTICE_DURATION_MS)
            _ui.update { it.copy(notice = null) }
        }
    }

    // --- Lifecycle: stop the audio (and release system audio focus) when backgrounded ---

    /** App left the foreground (Home, another app): pause so audio stops and focus is released. */
    fun onBackground() {
        foreground = false
        failoverJob?.cancel() // don't let the watchdog auto-skip (and so restart audio) in the background
        player.playWhenReady = false
    }

    /** App returned to the foreground: resume live playback. */
    fun onForeground() {
        foreground = true
        if (canPlay) {
            player.playWhenReady = true
            if (!hasStartedOnce) armStartWatchdog()
        }
    }

    // --- OK menu with sections: Quality / Audio / Subtitles / Guide (◀▶ switches section) ---

    /** Stable identity of each menu section, independent of its (translated) display label. */
    private enum class MenuSection { QUALITY, AUDIO, SUBTITLES, GUIDE }

    private var currentSection = MenuSection.QUALITY

    /** Localized label shown for a section header. */
    private fun sectionLabel(section: MenuSection): String = context.getString(
        when (section) {
            MenuSection.QUALITY -> R.string.menu_section_quality
            MenuSection.AUDIO -> R.string.menu_section_audio
            MenuSection.SUBTITLES -> R.string.menu_section_subtitles
            MenuSection.GUIDE -> R.string.menu_section_guide
        },
    )

    /** Labels + current selection + how to apply a pick, for whichever section is showing. */
    private class MenuOptions(val labels: List<String>, val selected: Int, val apply: (Int) -> Unit)

    private var menuApply: (Int) -> Unit = {}

    fun openMenu() {
        showSection(MenuSection.QUALITY)
    }

    fun closeMenu() {
        _ui.update { it.copy(menuOpen = false) }
    }

    /** Cycles to the previous/next section (Quality ↔ Audio ↔ Subtitles ↔ Guide). */
    fun moveMenuSection(delta: Int) {
        if (!_ui.value.menuOpen) return
        val all = MenuSection.entries
        val current = all.indexOf(currentSection)
        val next = (current + delta + all.size) % all.size
        showSection(all[next])
    }

    fun moveMenuSelection(delta: Int) {
        _ui.update {
            if (!it.menuOpen || it.menuOptions.isEmpty()) {
                it
            } else {
                it.copy(menuSelectedIndex = (it.menuSelectedIndex + delta).coerceIn(0, it.menuOptions.lastIndex))
            }
        }
    }

    fun confirmMenuSelection() {
        if (!_ui.value.menuOpen) return
        menuApply(_ui.value.menuSelectedIndex)
        _ui.update { it.copy(menuOpen = false) }
    }

    private fun showSection(section: MenuSection) {
        currentSection = section
        val options = when (section) {
            MenuSection.AUDIO -> audioMenuOptions()
            MenuSection.SUBTITLES -> subtitleMenuOptions()
            MenuSection.GUIDE -> guideMenuOptions()
            MenuSection.QUALITY -> qualityMenuOptions()
        }
        menuApply = options.apply
        _ui.update {
            it.copy(
                menuOpen = true,
                menuSection = sectionLabel(section),
                menuOptions = options.labels,
                menuSelectedIndex = options.selected,
            )
        }
    }

    private fun qualityMenuOptions(): MenuOptions {
        val group = currentGroup
        val options = if (group != null) emissionOptions(group) else listOf<Quality?>(null)
        return MenuOptions(
            labels = options.map(::emissionLabel),
            selected = options.indexOf(selectedEmission).coerceAtLeast(0),
            apply = { index -> applyEmission(options.getOrNull(index)) },
        )
    }

    private fun applyEmission(emission: Quality?) {
        val group = currentGroup ?: return
        selectedEmission = emission
        rebufferCount = 0
        viewModelScope.launch {
            val variant = variantForEmission(group)
            currentQuality = variant.quality
            _ui.update { it.copy(emissionLabel = emissionLabel(selectedEmission), isBuffering = true) }
            playUri(variant)
        }
    }

    private fun audioMenuOptions(): MenuOptions {
        val labels = mutableListOf<String>()
        val overrides = mutableListOf<TrackSelectionOverride>()
        var selected = 0
        player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO }
            .forEach { g ->
                for (i in 0 until g.length) {
                    if (g.isTrackSelected(i)) selected = labels.size
                    labels += trackLabel(g.getTrackFormat(i), context.getString(R.string.audio_track_n, labels.size + 1))
                    overrides += TrackSelectionOverride(g.mediaTrackGroup, i)
                }
            }
        if (labels.isEmpty()) return MenuOptions(listOf(context.getString(R.string.audio_none)), 0) {}
        return MenuOptions(labels, selected) { index ->
            overrides.getOrNull(index)?.let { ov ->
                player.trackSelectionParameters =
                    player.trackSelectionParameters.buildUpon().setOverrideForType(ov).build()
            }
        }
    }

    private fun subtitleMenuOptions(): MenuOptions {
        val labels = mutableListOf(context.getString(R.string.subtitles_off))
        val overrides = mutableListOf<TrackSelectionOverride?>(null)
        val textDisabled = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
        var selected = 0
        player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_TEXT }
            .forEach { g ->
                for (i in 0 until g.length) {
                    if (!textDisabled && g.isTrackSelected(i)) selected = labels.size
                    labels += trackLabel(g.getTrackFormat(i), context.getString(R.string.subtitle_track_n, labels.size))
                    overrides += TrackSelectionOverride(g.mediaTrackGroup, i)
                }
            }
        return MenuOptions(labels, selected) { index ->
            val override = overrides.getOrNull(index)
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().apply {
                if (override == null) {
                    setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                } else {
                    setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    setOverrideForType(override)
                }
            }.build()
        }
    }

    /** Read-only schedule for the current channel: the now-playing programme plus the next few. */
    private fun guideMenuOptions(): MenuOptions {
        if (currentEpg.isEmpty()) return MenuOptions(listOf(context.getString(R.string.guide_none)), 0) {}
        val now = System.currentTimeMillis()
        val currentIndex = currentEpg.indexOfFirst { it.nowFlag }
            .takeIf { it >= 0 }
            ?: currentEpg.indexOfFirst { it.start in 1..now && now < it.end }.coerceAtLeast(0)
        val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val labels = currentEpg.drop(currentIndex).take(8)
            .map { "${fmt.format(java.util.Date(it.start))}  ${it.title}" }
        // The now-playing programme is first and pre-selected; OK just closes (read-only).
        return MenuOptions(labels, 0) {}
    }

    /** A readable label for an audio/text track: its name, else its language, else [fallback]. */
    private fun trackLabel(format: Format, fallback: String): String {
        format.label?.takeIf { it.isNotBlank() }?.let { return it }
        format.language?.takeIf { it.isNotBlank() && it != "und" }?.let { lang ->
            return java.util.Locale(lang).getDisplayLanguage(java.util.Locale.getDefault()).ifBlank { lang }
        }
        return fallback
    }

    // --- Internals ---

    private suspend fun playGroup(group: ChannelGroup) {
        currentGroup = group
        currentEpg = emptyList()
        rebufferCount = 0
        hasStartedOnce = false
        val variant = variantForEmission(group)
        currentQuality = variant.quality
        _ui.update {
            it.copy(
                channelName = group.displayName,
                emissionLabel = emissionLabel(selectedEmission),
                isBuffering = true,
                menuOpen = false,
                nowProgram = null,
                nextProgram = null,
                errorMessage = null,
                isFavorite = group.key in favoriteKeys,
            )
        }
        playUri(variant)
        loadEpg(group)
    }

    /** Toggles the playing channel as a favorite (long-press OK), with a brief on-screen notice. */
    fun toggleCurrentChannelFavorite() {
        if (_ui.value.menuOpen) return
        val group = currentGroup ?: return
        val willBeFavorite = group.key !in favoriteKeys
        viewModelScope.launch { settingsStore.toggleFavoriteChannel(group.key) }
        showNotice(
            context.getString(
                if (willBeFavorite) R.string.added_to_favorites else R.string.removed_from_favorites,
            ),
        )
    }

    /** Fetches "now / next" EPG for the playing channel (Xtream API or M3U's XMLTV guide). */
    private fun loadEpg(group: ChannelGroup) {
        viewModelScope.launch {
            val epg = repository.epgFor(group)
            if (currentGroup !== group || epg.isEmpty()) return@launch
            currentEpg = epg
            val now = System.currentTimeMillis()
            val current = epg.firstOrNull { it.nowFlag }
                ?: epg.firstOrNull { it.start in 1..now && now < it.end }
                ?: epg.first()
            val next = epg.getOrNull(epg.indexOf(current) + 1)
            _ui.update { it.copy(nowProgram = current.title, nextProgram = next?.title) }
        }
    }

    private fun playUri(variant: ChannelVariant) {
        applyResolutionCap()
        player.setMediaItem(MediaItem.fromUri(variant.channel.streamUrl))
        player.prepare()
        // Never auto-play (nor arm the failover watchdog) while backgrounded — that would revive audio.
        player.playWhenReady = foreground
        if (foreground) armStartWatchdog() else failoverJob?.cancel()
    }

    /**
     * On Auto, cap the decoded video to the display's resolution (no point decoding 4K on a 1080p TV).
     * On a manually-fixed quality, honour the user's choice with no cap, so picking 4K really plays it.
     */
    private fun applyResolutionCap() {
        val capToScreen = selectedEmission == null
        val width = if (capToScreen) playerEngine.displayWidth else Int.MAX_VALUE
        val height = if (capToScreen) playerEngine.displayHeight else Int.MAX_VALUE
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setMaxVideoSize(width, height)
            .build()
    }

    /**
     * Fails a channel over quickly: if it hasn't started within [START_TIMEOUT_MS] it's treated as
     * dead (a connect/read error already routes here via [onPlayerError]), so we don't sit buffering
     * on a broken stream. Re-armed on every [playUri]; cancelled once the channel reaches READY.
     */
    private fun armStartWatchdog() {
        failoverJob?.cancel()
        failoverJob = viewModelScope.launch {
            delay(START_TIMEOUT_MS)
            if (!hasStartedOnce) handleChannelFailure()
        }
    }

    private fun downshift() {
        if (selectedEmission != null) return
        val group = currentGroup ?: return
        val lower = group.variants.firstOrNull { it.quality.rank < currentQuality.rank } ?: return
        rebufferCount = 0
        currentQuality = lower.quality
        playUri(group.variantFor(lower.quality) ?: group.bestVariant())
    }

    /**
     * A channel failed to play. First try a lower-quality variant of the same channel; if the channel
     * has no variant left it's dead, so auto-skip to the next channel (forward), up to one full lap of
     * the playlist. If the whole list is unreachable, give up with a message.
     */
    private fun handleChannelFailure(httpCode: Int? = null) {
        failoverJob?.cancel()
        if (!foreground) return // never fail over / restart playback while backgrounded
        val group = currentGroup ?: return
        val lower = group.variants.firstOrNull { it.quality.rank < currentQuality.rank }
        if (lower != null) {
            currentQuality = lower.quality
            rebufferCount = 0
            _ui.update { it.copy(isBuffering = true, errorMessage = null) }
            playUri(group.variantFor(lower.quality) ?: group.bestVariant())
            return
        }
        // 403/401/451 means geo/IP-blocked or no access, not just a dead stream — say so.
        val blocked = httpCode == 403 || httpCode == 401 || httpCode == 451
        // Channel fully dead. Skip in the direction we were going, but only a few neighbours: if a
        // run of them all fail, the area/list is likely dead — stop instead of cycling endlessly.
        if (playbackSession.size > 1 && autoSkipCount < minOf(playbackSession.size - 1, MAX_AUTO_SKIPS)) {
            autoSkipCount++
            val next = (if (lastDirection >= 0) playbackSession.next() else playbackSession.previous())
                ?: return
            _ui.update { it.copy(isBuffering = true, errorMessage = null) }
            showNotice(
                if (blocked) context.getString(R.string.player_blocked_skipping, httpCode ?: 0)
                else context.getString(R.string.player_skipping_unavailable),
            )
            viewModelScope.launch { playGroup(next) }
        } else {
            val origin = group.country
                ?.let { context.getString(R.string.player_channel_from, countryName(it)) }
                .orEmpty()
            _ui.update {
                it.copy(
                    isBuffering = false,
                    errorMessage = if (blocked) {
                        context.getString(R.string.player_blocked_no_access, httpCode ?: 0, origin)
                    } else {
                        context.getString(R.string.player_no_channel)
                    },
                )
            }
        }
    }

    /** Localised country name for an ISO code (iptv-org's "uk" mapped to GB), falling back to the code. */
    private fun countryName(code: String): String {
        val norm = if (code.equals("uk", ignoreCase = true)) "GB" else code.uppercase()
        return java.util.Locale("", norm).getDisplayCountry(java.util.Locale.getDefault())
            .ifBlank { code.uppercase() }
    }

    /**
     * Auto plus the channel's available qualities (high→low); the null entry represents Auto.
     * Deduplicated by label so tiers that share a label (e.g. SD and UNKNOWN both show "SD") collapse.
     */
    private fun emissionOptions(group: ChannelGroup): List<Quality?> =
        (listOf<Quality?>(null) + group.variants.map { it.quality })
            .distinctBy(::emissionLabel)

    private fun emissionLabel(quality: Quality?): String = quality?.label ?: context.getString(R.string.quality_auto)

    private suspend fun variantForEmission(group: ChannelGroup): ChannelVariant {
        val fixed = selectedEmission
        if (fixed != null) return group.variantFor(fixed) ?: group.variantAtOrBelow(fixed)
        // Auto: don't pick a tier above what the screen can show (4K/2K on a 1080p TV is wasted),
        // then choose the highest remaining variant that fits the measured network.
        val cap = maxQualityForHeight(playerEngine.displayHeight)
        val allowed = group.variants.filter { it.quality.rank <= cap.rank }.ifEmpty { group.variants }
        val bandwidth = maxOf(settingsStore.bandwidthBps(), playerEngine.bitrateEstimateBps())
        return if (bandwidth <= 0L) {
            allowed.firstOrNull { it.quality.rank <= Quality.FHD.rank } ?: allowed.last()
        } else {
            val budget = (bandwidth * 0.8).toLong()
            allowed.firstOrNull { it.quality.typicalBitrateBps <= budget } ?: allowed.last()
        }
    }

    /** Highest quality tier worth playing on a display of [height] pixels. */
    private fun maxQualityForHeight(height: Int): Quality = when {
        height >= 2000 -> Quality.UHD_4K
        height >= 1300 -> Quality.QHD
        height >= 1000 -> Quality.FHD
        height >= 650 -> Quality.HD
        else -> Quality.SD
    }

    private fun pollStats() {
        viewModelScope.launch {
            lastBytesTransferred = playerEngine.bytesTransferred()
            var tick = 0
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                // Live download rate: bytes pulled off the network since the last tick.
                val totalBytes = playerEngine.bytesTransferred()
                val deltaBytes = (totalBytes - lastBytesTransferred).coerceAtLeast(0)
                lastBytesTransferred = totalBytes
                val instantMbps = deltaBytes * 8.0 * (1000.0 / POLL_INTERVAL_MS) / 1_000_000.0
                // Light smoothing so the readout doesn't flicker between bursts and idle ticks.
                smoothedThroughputMbps = smoothedThroughputMbps * 0.5 + instantMbps * 0.5

                val format = player.videoFormat
                val resolution = format?.takeIf { it.width > 0 && it.height > 0 }
                    ?.let { "${it.width}x${it.height}" }
                _ui.update {
                    it.copy(throughputMbps = smoothedThroughputMbps, resolution = resolution)
                }
                // The smoothed bandwidth estimate (not the instantaneous rate) is what Auto persists.
                if (++tick % BANDWIDTH_PERSIST_EVERY == 0) {
                    settingsStore.setBandwidthBps(playerEngine.bitrateEstimateBps())
                }
            }
        }
    }

    override fun onCleared() {
        failoverJob?.cancel()
        noticeJob?.cancel()
        player.removeListener(listener)
        player.release()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 500L
        private const val BANDWIDTH_PERSIST_EVERY = 10 // persist the estimate ~every 5 s
        private const val NOTICE_DURATION_MS = 2_500L
        private const val START_TIMEOUT_MS = 4_000L // a channel that hasn't started by now is dead
        private const val MAX_AUTO_SKIPS = 12 // stop auto-skipping after this many dead channels in a row
        private const val DOUBLE_PRESS_MS = 350L // two laterals within this window jump folders
        private const val MAX_CONTROLS_HINTS = 3 // show the controls legend only this many times
        private const val CONTROLS_HINT_MS = 6_000L // how long the legend stays before fading out

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FootballXtreamApp
                val container = app.container
                PlayerViewModel(
                    container.playbackSession,
                    container.settingsStore,
                    container.playerEngine,
                    container.repository,
                    app,
                )
            }
        }
    }
}
