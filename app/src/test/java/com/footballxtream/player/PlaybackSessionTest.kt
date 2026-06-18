package com.footballxtream.player

import com.footballxtream.model.ChannelFolder
import com.footballxtream.model.ChannelGroup
import com.footballxtream.model.ChannelVariant
import com.footballxtream.model.LiveChannel
import com.footballxtream.model.Quality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSessionTest {

    @Test
    fun next_cyclesWithinFolderAndWraps() {
        val session = PlaybackSession().apply { start(listOf(folder("F", "A", "B", "C")), 0, 0) }

        assertEquals("B", session.next()?.displayName)
        assertEquals("C", session.next()?.displayName)
        assertEquals("A", session.next()?.displayName) // wraps back to the first
    }

    @Test
    fun previous_wrapsToLastChannel() {
        val session = PlaybackSession().apply { start(listOf(folder("F", "A", "B", "C")), 0, 0) }

        assertEquals("C", session.previous()?.displayName)
    }

    @Test
    fun singleChannelFolder_nextStepsToTheNextFolder() {
        // A folder with one channel has nothing to cycle through, so next() must move to the next
        // folder rather than staying put on the same channel.
        val folders = listOf(folder("Solo", "Only"), folder("Multi", "X", "Y"))
        val session = PlaybackSession().apply { start(folders, 0, 0) }

        val next = session.next()

        assertEquals("X", next?.displayName)
        assertEquals(1, session.folderIndex)
        assertEquals(0, session.channelIndex)
    }

    @Test
    fun nextFolder_landsOnFirstChannelOfNextFolderAndWraps() {
        val folders = listOf(folder("F0", "A", "B"), folder("F1", "C", "D"))
        val session = PlaybackSession().apply { start(folders, 0, 1) }

        assertEquals("C", session.nextFolder()?.displayName)
        assertEquals(0, session.channelIndex)
        assertEquals("A", session.nextFolder()?.displayName) // wraps back to F0
    }

    @Test
    fun jumpToLast_togglesBetweenTheTwoMarkedChannels() {
        val session = PlaybackSession().apply { start(listOf(folder("F", "A", "B", "C", "D")), 0, 0) }

        // Mark A as "last", then move away to C.
        session.markCurrentAsLast()
        session.next() // B
        session.next() // C
        assertEquals("C", session.current?.displayName)

        // Zap-to-last returns to A and remembers C, so a second zap toggles back.
        assertEquals("A", session.jumpToLast()?.displayName)
        assertEquals("C", session.jumpToLast()?.displayName)
        assertEquals("A", session.jumpToLast()?.displayName)
    }

    @Test
    fun autoSkip_doesNotOverwriteTheLastChannel() {
        // The bug this guards: pressing next on a live channel marks it as "last"; if the next
        // channel is dead the player auto-skips by calling next() again WITHOUT marking. The zap must
        // still return to the channel the user actually chose, not the dead one we skipped past.
        val session = PlaybackSession().apply { start(listOf(folder("F", "A", "B", "C", "D")), 0, 0) }

        // Manual zap A -> B: mark current (A), then step.
        session.markCurrentAsLast()
        session.next() // now on B
        // B is dead: auto-skip steps again WITHOUT marking.
        session.next() // now on C
        assertEquals("C", session.current?.displayName)

        // Zap-to-last must go back to A (the chosen channel), not B (the skipped dead one).
        assertEquals("A", session.jumpToLast()?.displayName)
    }

    @Test
    fun start_coercesOutOfRangeIndices() {
        val session = PlaybackSession().apply { start(listOf(folder("F", "A", "B")), 9, 9) }

        assertEquals(0, session.folderIndex)
        assertEquals(1, session.channelIndex) // last valid channel
        assertEquals("B", session.current?.displayName)
    }

    @Test
    fun refreshFavorites_isANoOpUnlessItIsTheFavoritesList() {
        val session = PlaybackSession().apply { start(listOf(folder("F", "A", "B", "C")), 0, 0) }

        assertFalse(session.refreshFavorites(setOf("A"), "A"))
        assertEquals(3, session.size) // untouched
    }

    @Test
    fun refreshFavorites_dropsUnfavoritedAndKeepsTheCurrentChannel() {
        // On C; unfavorite B and D → the list shrinks to [A, C] and stays on C.
        val session = PlaybackSession().apply {
            start(listOf(folder("F", "A", "B", "C", "D")), 0, 2, isFavoritesList = true)
        }

        assertTrue(session.refreshFavorites(setOf("A", "C"), "C"))
        assertEquals(2, session.size)
        assertEquals("C", session.current?.displayName)
        assertEquals(1, session.channelIndex)
    }

    @Test
    fun refreshFavorites_reAddingAChannelFromTheSnapshotBringsItBack() {
        // Watching A; unfavorite it → [B, C], then re-favorite A → it returns and the index lands on it.
        val session = PlaybackSession().apply {
            start(listOf(folder("F", "A", "B", "C")), 0, 0, isFavoritesList = true)
        }

        assertTrue(session.refreshFavorites(setOf("B", "C"), "A"))
        assertEquals(2, session.size)

        assertTrue(session.refreshFavorites(setOf("A", "B", "C"), "A"))
        assertEquals(3, session.size)
        assertEquals("A", session.current?.displayName)
        assertEquals(0, session.channelIndex)
    }

    @Test
    fun refreshFavorites_whenCurrentRemoved_nextLandsOnTheFollowingChannel() {
        // On B; unfavorite B → [A, C, D]. Pressing next (◀) should land on C — the channel that
        // followed B — not skip over it.
        val session = PlaybackSession().apply {
            start(listOf(folder("F", "A", "B", "C", "D")), 0, 1, isFavoritesList = true)
        }

        assertTrue(session.refreshFavorites(setOf("A", "C", "D"), "B"))
        assertEquals(3, session.size)
        assertEquals("C", session.next()?.displayName)
    }

    @Test
    fun current_isNullWhenNoFolders() {
        val session = PlaybackSession()

        assertNull(session.current)
        assertNull(session.next())
        assertNull(session.jumpToLast())
    }

    private fun folder(name: String, vararg channelNames: String) = ChannelFolder(
        name = name,
        iconUrl = null,
        isFootball = false,
        channels = channelNames.map { group(it) },
    )

    private fun group(name: String) = ChannelGroup(
        key = name,
        displayName = name,
        iconUrl = null,
        isFootball = false,
        variants = listOf(
            ChannelVariant(
                LiveChannel(
                    streamId = name.hashCode(),
                    name = name,
                    iconUrl = null,
                    categoryName = null,
                    streamUrl = "http://example/$name",
                ),
                Quality.UNKNOWN,
            ),
        ),
    )
}
