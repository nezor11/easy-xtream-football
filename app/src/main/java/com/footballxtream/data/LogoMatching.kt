package com.footballxtream.data

import com.footballxtream.model.ChannelGroup

/**
 * How a channel that ships without a logo gets one from the logo database: which name keys to look
 * up, and a last resort from its own folder. Brand-agnostic: nothing here names a channel.
 */
object LogoMatching {

    /** The database is keyed by name with everything but letters and digits removed. */
    fun normalize(name: String): String = name.lowercase().filter { it.isLetterOrDigit() }

    private val whitespace = Regex("""\s+""")
    private val letters = Regex("""\p{L}""")
    private val sports = Regex("sports")
    private val sport = Regex("sport(?!s)")
    private val trailingMedium = Regex("(tv|channel)$")

    /**
     * Keys to try in order, most specific first. Playlists decorate the same channel in many ways
     * ("<country> Canal Sport 2", "Canal Sport 2 Alternate", "Canal Sport 2 FR"), so after the exact
     * name come its lexical variants (sport/sports, a trailing "TV"), then progressively shorter runs
     * of its words: first dropping words from the end (a regional or feed suffix), then from the start
     * (a country or provider prefix). A run must keep at least two words with letters, and a run that
     * lost its start must still begin with a real word (3+ letters) and be 8+ characters, so a short,
     * generic tail ("TV Sports") never claims some unrelated channel's logo.
     */
    fun candidates(displayName: String): List<String> {
        val words = whitespace.split(displayName.trim()).filter { it.isNotEmpty() }
        val keys = LinkedHashSet<String>()
        keys += variants(normalize(displayName))
        for (end in words.size - 1 downTo 2) {
            val run = words.subList(0, end)
            if (run.count { letters.containsMatchIn(it) } >= 2) keys += variants(normalize(run.joinToString(" ")))
        }
        for (start in 1 until words.size - 1) {
            val run = words.subList(start, words.size)
            val key = normalize(run.joinToString(" "))
            if (run.count { letters.containsMatchIn(it) } >= 2 &&
                letters.findAll(run.first()).count() >= 3 &&
                key.length >= 8
            ) {
                keys += variants(key)
            }
        }
        return keys.filter { it.isNotBlank() }
    }

    private fun variants(key: String): List<String> = listOf(
        key,
        sports.replace(key, "sport"),
        sport.replace(key, "sports"),
        trailingMedium.replace(key, ""),
    )

    /**
     * Gives a group that still has no logo the logo of a sibling in its folder (same brand family by
     * [ChannelNameParser.folderKey]), e.g. a numbered feed borrowing its main channel's logo. Groups
     * whose folder has no logo at all are returned unchanged.
     */
    fun fillFromFolderSiblings(groups: List<ChannelGroup>): List<ChannelGroup> {
        val folderLogo = HashMap<String, String>()
        groups.forEach { g ->
            val url = g.iconUrl
            if (!url.isNullOrBlank()) folderLogo.putIfAbsent(ChannelNameParser.folderKey(g.displayName), url)
        }
        if (folderLogo.isEmpty()) return groups
        return groups.map { g ->
            if (!g.iconUrl.isNullOrBlank()) g
            else folderLogo[ChannelNameParser.folderKey(g.displayName)]?.let { g.copy(iconUrl = it) } ?: g
        }
    }
}
