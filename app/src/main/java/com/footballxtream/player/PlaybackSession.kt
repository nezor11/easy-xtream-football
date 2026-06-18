package com.footballxtream.player

import com.footballxtream.model.ChannelFolder
import com.footballxtream.model.ChannelGroup

/**
 * Holds the playback context for the player without serializing it through navigation args: the full
 * folder list plus the current folder and the current channel within it. The player zaps channels
 * (left/right) within the current folder and jumps whole folders (quick double-press) with the remote.
 */
class PlaybackSession {

    @Volatile
    var folders: List<ChannelFolder> = emptyList()
        private set

    @Volatile
    var folderIndex: Int = 0
        private set

    @Volatile
    var channelIndex: Int = 0
        private set

    // The position held just before the last change, so the remote can zap back to it.
    private var lastFolderIndex: Int = 0
    private var lastChannelIndex: Int = 0

    // True when this session's single folder is the live "favorites" list, so it can be recomputed as
    // the launch snapshot ∩ current favorites when the user (un)favorites a channel while watching.
    @Volatile
    var isFavoritesList: Boolean = false
        private set

    // The full favorites list captured at launch; the active list is this ∩ current favorites, so
    // re-favoriting the channel you just removed brings it back (not only removals stick).
    private var favoritesSnapshot: List<ChannelGroup> = emptyList()

    private val currentChannels: List<ChannelGroup>
        get() = folders.getOrNull(folderIndex)?.channels.orEmpty()

    val current: ChannelGroup?
        get() = currentChannels.getOrNull(channelIndex)

    /** Channels in the current folder; bounds the per-channel auto-skip. */
    val size: Int
        get() = currentChannels.size

    val currentFolderName: String?
        get() = folders.getOrNull(folderIndex)?.name

    fun start(
        folders: List<ChannelFolder>,
        folderIndex: Int,
        channelIndex: Int,
        isFavoritesList: Boolean = false,
    ) {
        this.folders = folders
        this.folderIndex = folderIndex.coerceIn(0, (folders.size - 1).coerceAtLeast(0))
        this.channelIndex = channelIndex.coerceIn(0, (currentChannels.size - 1).coerceAtLeast(0))
        this.isFavoritesList = isFavoritesList
        this.favoritesSnapshot = if (isFavoritesList) currentChannels else emptyList()
    }

    /**
     * For the favorites zap list only: rebuilds it as the launch snapshot filtered to the current
     * favorites [keys] — so unfavoriting a channel shrinks the list and re-favoriting one that was in
     * it brings it back. The index stays on [currentKey] (the playing channel) when present, otherwise
     * clamps onto its old slot so ◀▶ continue through the rest. Returns true if the list changed.
     */
    fun refreshFavorites(keys: Set<String>, currentKey: String?): Boolean {
        if (!isFavoritesList) return false
        val folder = folders.getOrNull(folderIndex) ?: return false
        val kept = favoritesSnapshot.filter { it.key in keys }
        if (kept.isEmpty() || kept.map { it.key } == folder.channels.map { it.key }) return false
        folders = folders.toMutableList().also { it[folderIndex] = folder.copy(channels = kept) }
        val keptIndex = kept.indexOfFirst { it.key == currentKey }
        channelIndex = if (keptIndex >= 0) {
            keptIndex
        } else {
            // The playing channel was the one removed: sit just before its old slot so the next ◀
            // lands on the channel that followed it, rather than skipping over it.
            (channelIndex - 1).coerceIn(0, kept.lastIndex)
        }
        return true
    }

    fun next(): ChannelGroup? = stepChannel(1)

    fun previous(): ChannelGroup? = stepChannel(-1)

    /** Jumps to the first channel of the next folder (wraps around). */
    fun nextFolder(): ChannelGroup? = stepFolder(1)

    fun previousFolder(): ChannelGroup? = stepFolder(-1)

    /**
     * Records the current channel as the "last" one to zap back to. Called on manual zaps only (not
     * on the automatic skip over dead channels), so the zap returns to the channel you chose, not to
     * an unavailable one we skipped past.
     */
    fun markCurrentAsLast() {
        lastFolderIndex = folderIndex
        lastChannelIndex = channelIndex
    }

    /** Zaps back to the previously chosen channel (and remembers the current one, so it toggles). */
    fun jumpToLast(): ChannelGroup? {
        if (folders.isEmpty()) return null
        val targetFolder = lastFolderIndex.coerceIn(0, folders.size - 1)
        val targetChannel = lastChannelIndex
        markCurrentAsLast()
        folderIndex = targetFolder
        val n = folders.getOrNull(targetFolder)?.channels?.size ?: 0
        channelIndex = targetChannel.coerceIn(0, (n - 1).coerceAtLeast(0))
        return current
    }

    private fun stepChannel(delta: Int): ChannelGroup? {
        val n = currentChannels.size
        if (n == 0) return null
        // A folder with a single channel has nothing to cycle through, so a plain next/previous
        // steps to the adjacent folder instead of staying put on the same channel.
        if (n == 1) return stepFolder(delta)
        channelIndex = (channelIndex + delta + n) % n
        return current
    }

    private fun stepFolder(delta: Int): ChannelGroup? {
        val n = folders.size
        if (n == 0) return null
        folderIndex = (folderIndex + delta + n) % n
        channelIndex = 0
        return current
    }
}
