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

    private val currentChannels: List<ChannelGroup>
        get() = folders.getOrNull(folderIndex)?.channels.orEmpty()

    val current: ChannelGroup?
        get() = currentChannels.getOrNull(channelIndex)

    /** Channels in the current folder; bounds the per-channel auto-skip. */
    val size: Int
        get() = currentChannels.size

    val currentFolderName: String?
        get() = folders.getOrNull(folderIndex)?.name

    fun start(folders: List<ChannelFolder>, folderIndex: Int, channelIndex: Int) {
        this.folders = folders
        this.folderIndex = folderIndex.coerceIn(0, (folders.size - 1).coerceAtLeast(0))
        this.channelIndex = channelIndex.coerceIn(0, (currentChannels.size - 1).coerceAtLeast(0))
    }

    fun next(): ChannelGroup? = stepChannel(1)

    fun previous(): ChannelGroup? = stepChannel(-1)

    /** Jumps to the first channel of the next folder (wraps around). */
    fun nextFolder(): ChannelGroup? = stepFolder(1)

    fun previousFolder(): ChannelGroup? = stepFolder(-1)

    /** Zaps back to the channel that was playing just before the current one (and remembers this). */
    fun jumpToLast(): ChannelGroup? {
        if (folders.isEmpty()) return null
        val f = lastFolderIndex.coerceIn(0, folders.size - 1)
        rememberCurrent()
        folderIndex = f
        channelIndex = lastChannelIndex.let {
            val n = folders.getOrNull(f)?.channels?.size ?: 0
            it.coerceIn(0, (n - 1).coerceAtLeast(0))
        }
        return current
    }

    private fun rememberCurrent() {
        lastFolderIndex = folderIndex
        lastChannelIndex = channelIndex
    }

    private fun stepChannel(delta: Int): ChannelGroup? {
        val n = currentChannels.size
        if (n == 0) return null
        // A folder with a single channel has nothing to cycle through, so a plain next/previous
        // steps to the adjacent folder instead of staying put on the same channel.
        if (n == 1) return stepFolder(delta)
        rememberCurrent()
        channelIndex = (channelIndex + delta + n) % n
        return current
    }

    private fun stepFolder(delta: Int): ChannelGroup? {
        val n = folders.size
        if (n == 0) return null
        rememberCurrent()
        folderIndex = (folderIndex + delta + n) % n
        channelIndex = 0
        return current
    }
}
