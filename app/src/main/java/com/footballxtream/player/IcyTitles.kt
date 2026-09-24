package com.footballxtream.player

/** Turns the raw ICY `StreamTitle` of an internet radio into something worth showing, or null. */
object IcyTitles {
    // iHeart-style payloads: `zc4732 - text="The Herd with Colin Cowherd" song_spot="T" ...`.
    private val quotedText = Regex("""\btext="([^"]*)"""")

    fun clean(raw: String?): String? {
        val t = raw?.trim().orEmpty()
        // A structured payload is never shown raw: only its text="…" part, or nothing.
        quotedText.find(t)?.let { m -> return m.groupValues[1].trim().takeIf { it.isNotEmpty() } }
        // Stations that keep the slot but send nothing useful ("", "_", "-", "...").
        if (t.length < 2 || t.none { it.isLetterOrDigit() }) return null
        // Some encoders wrap the title in quotes.
        return t.trim('"', '\'').trim().takeIf { it.isNotEmpty() }
    }
}
