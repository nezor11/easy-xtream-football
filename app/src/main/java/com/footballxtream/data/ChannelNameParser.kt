package com.footballxtream.data

import com.footballxtream.model.Quality

/**
 * Heuristics to make sense of the wildly inconsistent channel naming used by IPTV panels:
 * extract a quality tier, normalize the base channel name (so duplicate-quality streams collapse
 * into one logical channel), and classify sports / football content.
 */
object ChannelNameParser {

    // Order matters: 4K before 2K before FHD before HD ("FULL HD" contains "HD"). [tokenRegex] matches
    // a quality either delimited by non-alphanumerics ("LaLiga HD") or glued to the end of a word
    // ("LaLigaTVHD", "...HD+"), which IPTV panels do constantly.
    private val qualityPatterns: List<Pair<Quality, Regex>> = listOf(
        Quality.UHD_4K to tokenRegex("4k", "uhd", "2160p?"),
        Quality.QHD to tokenRegex("2k", "1440p?", "qhd"),
        Quality.FHD to tokenRegex("fhd", """full\s*hd""", "1080p?"),
        Quality.HD to tokenRegex("hd", "720p?"),
        Quality.SD to tokenRegex("sd", "480p?", "360p?"),
    )

    // A quality token matches whether it is spaced ("LaLiga HD") or glued to a word ("LaLigaTVHD"):
    // only the right side must be a boundary (end, space, symbol or emoji — anything non-alphanumeric),
    // and it must not start mid-number (so "1080" isn't read as "108"+"0"). Names are noise-stripped
    // first by [normalizeForTokens], so trailing flags/punctuation don't block the match.
    private fun tokenRegex(vararg tokens: String): Regex {
        val group = tokens.joinToString("|")
        return Regex("""(?<![0-9])($group)(?![a-z0-9])""", RegexOption.IGNORE_CASE)
    }

    private val allQualityTokens = Regex(
        """(?<![0-9])(4k|uhd|2160p?|2k|1440p?|qhd|fhd|full\s*hd|1080p?|hd|720p?|sd|480p?|360p?)(?![a-z0-9])""",
        RegexOption.IGNORE_CASE,
    )

    // Leading provider/country prefix: "ES|", "ES:", "EN -", "VIP >" and the pipe-wrapped form
    // "|IT| ", "|DE|  " that many M3U panels use. The optional leading separator catches the
    // wrapped form; a trailing separator is always required, so a plain word ("La Liga", "Al
    // Jazeera") is never mistaken for a prefix.
    private val leadingPrefix = Regex("""^\s*[|:>\-•]?\s*[\p{L}0-9]{1,4}\s*[|:>\-•]\s*""")
    private val bracketed = Regex("""[\[(][^\])]*[\])]""")
    // Strip emoji / flags / leftover symbol noise (keep letters, numbers, spaces, basic punctuation).
    private val noise = Regex("""[^\p{L}\p{N}\s&+./'-]""")
    private val multiSpace = Regex("""\s{2,}""")

    // Brand-agnostic on purpose: only generic sport terms, sport types and competitions — never
    // commercial channel brands. A channel whose name is only a brand (no sport term) is caught by
    // its category instead (see [isSports]). These match at a word start (see [boundaryRegex]); the
    // few strong roots in [sportsSubstringRegex] also match glued mid-word, so a name that fuses a
    // sport term into one word (e.g. "…sport"/"…deporte") is detected too, still naming no brand.
    private val sportsKeywords = listOf(
        // Generic "sport" in many languages
        "sport", "deporte", "esporte", "esport", "spor", "رياضة", "رياضي", "اسبور",
        "спорт", "체육", "运动", "體育",
        // Football / soccer (multi-language)
        "futbol", "fútbol", "football", "soccer", "fußball", "fussball", "calcio", "futebol",
        "voetbal", "fudbal", "ποδόσφαιρο", "كرة",
        // Leagues & competitions
        "laliga", "la liga", "liga", "ligue", "premier league", "championship", "bundesliga",
        "eredivisie", "serie a", "serie b", "primeira", "champions", "uefa", "europa league",
        "conference league", "copa", "libertadores", "sudamericana", "fa cup", "efl", "mls",
        "super lig", "jupiler", "superliga", "eliteserien", "allsvenskan", "fifa",
        // Basketball
        "baloncesto", "basket", "basketball", "nba", "acb", "euroleague", "euroliga", "basquet",
        // Tennis
        "tennis", "tenis", "atp", "wta", "roland garros", "wimbledon",
        // Motorsport
        "f1 ", "formula 1", "fórmula 1", "formula1", "motogp", "moto gp", "nascar", "indycar",
        "wrc", "dakar", "superbike", "motor",
        // US / combat sports
        "nfl", "nhl", "mlb", "ufc", "wwe", "boxing", "boxeo", "boxe", "mma", "wrestling", "fight",
        "combat", "lucha",
        // Other sports
        "rugby", "cricket", "golf", "ciclismo", "cycling", "hockey", "handball", "balonmano",
        "voleibol", "volleyball", "volley", "atletismo", "athletics", "padel", "pádel", "snooker",
        "darts", "racing", "olympic", "olimpic", "juegos olimpicos", "extreme",
    )

    // Football by sport/competition terms only — no channel brands. A football channel named after
    // its competition still matches via that competition (LaLiga, Champions…), brand or not.
    private val footballKeywords = listOf(
        "futbol", "fútbol", "football", "soccer", "fußball", "fussball", "calcio", "futebol",
        "voetbal", "fudbal", "ποδόσφαιρο",
        "laliga", "la liga", "premier league", "championship", "bundesliga", "eredivisie",
        "serie a", "serie b", "primeira liga", "ligue 1", "ligue 2",
        "champions", "uefa", "europa league", "conference league", "copa del rey", "copa",
        "libertadores", "sudamericana", "fa cup", "efl", "mls", "super lig", "jupiler", "fifa",
    )

    // First whitespace-delimited word, with surrounding separators trimmed off.
    private val firstWord = Regex("""^\S+""")

    /**
     * Folder/brand label shown on the card: the channel's first word (everything up to the first
     * space), e.g. "Eurosport 1" → "Eurosport", "DAZN LaLiga" → "DAZN".
     */
    fun folderName(baseName: String): String {
        val trimmed = baseName.trim()
        val word = firstWord.find(trimmed)?.value?.trim('-', '_', '·', '|', ':') ?: trimmed
        return word.ifBlank { trimmed }
    }

    private val nonLetters = Regex("""\P{L}+""")

    private val whitespace = Regex("""\s+""")

    // How many leading letters define a folder. Tune here to make grouping more/less aggressive.
    private const val FOLDER_KEY_LEN = 6

    /**
     * Grouping key: the channel's first word capped at [FOLDER_KEY_LEN] letters (ignoring digits and
     * punctuation). This keeps multi-word brands together ("DAZN LaLiga"/"DAZN F1" → "dazn"), gives
     * "Sport …" its own group ("sport"), and separates look-alikes that only share a shorter prefix
     * ("Eurosport" → "eurosp" vs "Euronews" → "eurone"). A very short first word (1-2 letters like
     * "M+", "2M", "C") can't identify a brand on its own, so it's extended with the rest of the
     * name's letters to avoid lumping unrelated channels together ("M+ LaLiga" → "mlalig").
     */
    fun folderKey(baseName: String): String {
        val trimmed = baseName.trim()
        val firstWordLetters = nonLetters.replace(firstWord.find(trimmed)?.value.orEmpty(), "").lowercase()
        val basis = if (firstWordLetters.length >= 3) {
            firstWordLetters
        } else {
            nonLetters.replace(trimmed, "").lowercase()
        }
        return when {
            basis.length >= FOLDER_KEY_LEN -> basis.take(FOLDER_KEY_LEN)
            basis.isNotEmpty() -> basis
            else -> trimmed.lowercase()
        }
    }

    /**
     * Display name for a folder grouping these channels: their longest common run of leading words,
     * so "C More Golf"/"C More Sport" → "C More" and a DAZN group stays "DAZN". Falls back to the
     * first channel's first word when they share no leading word.
     */
    fun commonFolderName(names: List<String>): String {
        if (names.isEmpty()) return ""
        val wordLists = names.map { whitespace.split(it.trim()).filter { w -> w.isNotEmpty() } }
        val firstWords = wordLists.first()
        var common = 0
        while (common < firstWords.size &&
            wordLists.all { it.getOrNull(common)?.equals(firstWords[common], ignoreCase = true) == true }
        ) {
            common++
        }
        return if (common > 0) firstWords.take(common).joinToString(" ") else folderName(names.first())
    }

    fun quality(rawName: String): Quality {
        val name = normalizeForTokens(rawName)
        for ((quality, pattern) in qualityPatterns) {
            if (pattern.containsMatchIn(name)) return quality
        }
        return Quality.UNKNOWN
    }

    /** Canonical channel name with quality tags, prefixes and noise removed. */
    fun baseName(rawName: String): String {
        val name = allQualityTokens.replace(normalizeForTokens(rawName), " ")
        return multiSpace.replace(name, " ").trim().trim('-', '|', ':', '.', '·').trim()
    }

    /**
     * Strips brackets, a leading provider prefix and symbol/emoji noise — but NOT quality tags —
     * so quality detection runs on a clean string. Crucially this removes trailing flag emojis
     * before quality is read, so a glued tag like "LigaSmartBankHD🇪🇸" still resolves to "HD".
     */
    private fun normalizeForTokens(rawName: String): String {
        var name = bracketed.replace(rawName, " ")
        name = leadingPrefix.replace(name, "")
        name = noise.replace(name, " ")
        return multiSpace.replace(name, " ").trim()
    }

    /** Stable identity used to group quality variants of the same channel. */
    fun groupKey(rawName: String): String = baseName(rawName).lowercase()

    // Keywords match only at the start of a word (not preceded by a letter), so "liga" no longer
    // matches "alligator"/"hooligans"/"obligations"/"caligari" while still catching "LaLiga",
    // "Liga SmartBank", and prefixes like "spor" -> "Sport"/"SporTV".
    private val sportsRegex = boundaryRegex(sportsKeywords)
    private val footballRegex = boundaryRegex(footballKeywords)

    // Strong, unambiguous generic roots that also count when glued mid-word, so a name that fuses a
    // sport term into one word ("…sport"/"…deporte") is detected without listing any brand. "sport"
    // excludes the non-sport hosts "transport"/"passport" via lookbehind.
    private val sportsSubstringRegex = Regex(
        """(?<!tran)(?<!pas)sport|deporte|fútbol|futbol|football|calcio""",
        RegexOption.IGNORE_CASE,
    )

    // Agnostic: a channel is sport if a sport term appears in its name or its category — whole-word
    // for the general list, plus the few strong roots above even mid-word. No specific channel names
    // are listed anywhere, neither to include (brands) nor to exclude (broadcasters).
    fun isSports(channelName: String, categoryName: String?): Boolean =
        isSportText(channelName) || isSportText(categoryName)

    private fun isSportText(text: String?): Boolean =
        matchesAny(text, sportsRegex) || matchesAny(text, sportsSubstringRegex)

    fun isFootball(channelName: String, categoryName: String?): Boolean =
        matchesAny(channelName, footballRegex) || matchesAny(categoryName, footballRegex)

    // "VOD" as a whole word in the category (so "VOD FR" matches but "VODAFONE" does not). Used to
    // drop video-on-demand that panels file under live categories (e.g. Xtream get_live_streams).
    private val vodCategory = Regex("""\bvod\b""", RegexOption.IGNORE_CASE)

    fun isVodCategory(category: String?): Boolean =
        category != null && vodCategory.containsMatchIn(category)

    private fun boundaryRegex(keywords: List<String>): Regex {
        val alternation = keywords.joinToString("|") { Regex.escape(it) }
        return Regex("""(?<!\p{L})($alternation)""", RegexOption.IGNORE_CASE)
    }

    private fun matchesAny(text: String?, regex: Regex): Boolean {
        if (text.isNullOrBlank()) return false
        return regex.containsMatchIn(text)
    }
}
