package com.footballxtream.data

import android.util.Log
import android.util.Xml
import com.footballxtream.data.remote.XtreamClient
import com.footballxtream.model.EpgProgram
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream

/**
 * Reads XMLTV guide files (as declared by an M3U's `x-tvg-url`) and builds a "now / next" index for
 * a known set of channel ids. Files may be plain XML or gzipped and are streamed (never fully
 * buffered). Only programmes for the requested channel ids that are still current or upcoming are
 * kept, so memory stays bounded regardless of how huge the source is.
 */
object XmltvEpg {

    private const val TAG = "FXEpg"
    // EPG sources can list dozens of huge files (national guides). Cap how many we pull so a single
    // guide request can never download the whole world. We pull the most relevant ones first (see
    // [prioritize]), so a handful is enough to cover the channels we actually carry.
    private const val MAX_SOURCES = 8

    /** Programmes per channel id, sorted by start time, for the channels in [neededIds]. */
    fun index(urls: List<String>, neededIds: Set<String>): Map<String, List<EpgProgram>> {
        if (urls.isEmpty() || neededIds.isEmpty()) return emptyMap()
        val now = System.currentTimeMillis()
        val out = HashMap<String, MutableList<EpgProgram>>()
        for (url in prioritize(urls, neededIds).take(MAX_SOURCES)) {
            runCatching {
                XtreamClient.withStream(url) { raw -> parse(unGzipIfNeeded(raw), neededIds, now, out) }
            }.onFailure { Log.w(TAG, "XMLTV source failed: $url", it) }
        }
        return out.mapValues { (_, list) -> list.sortedBy { it.start } }
    }

    /**
     * When a playlist declares many national guides (epgshare01 lists ~50), taking the first
     * [MAX_SOURCES] blindly often grabs guides for countries we carry no channels from, leaving the
     * relevant ones unfetched. Reorder so the guides that match our channels' countries (read from the
     * tvg-id suffix, e.g. "Rete8Sport.it" → it) come first, then broad "all sources" guides, then the
     * rest. Stable, so playlists with a single guide (or no country hints) are left untouched.
     */
    internal fun prioritize(urls: List<String>, neededIds: Set<String>): List<String> {
        val countries = neededIds.mapNotNullTo(HashSet()) { id ->
            id.substringAfterLast('.', "").lowercase()
                .takeIf { it.length == 2 && it.all(Char::isLetter) }
        }
        if (countries.isEmpty()) return urls
        // Matches the country token in a feed file name: epgshare01's "…_IT1.xml.gz", "…_ES.xml", etc.
        val countryFeed = Regex("[_/.](" + countries.joinToString("|") + ")\\d*[._]")
        return urls.sortedBy { url ->
            val u = url.lowercase()
            when {
                countryFeed.containsMatchIn(u) -> 0
                "all_sources" in u -> 1
                else -> 2
            }
        }
    }

    private fun unGzipIfNeeded(raw: InputStream): InputStream {
        val buffered = BufferedInputStream(raw)
        buffered.mark(2)
        val b0 = buffered.read()
        val b1 = buffered.read()
        buffered.reset()
        val isGzip = b0 == 0x1f && b1 == 0x8b
        return if (isGzip) GZIPInputStream(buffered) else buffered
    }

    private fun parse(
        input: InputStream,
        neededIds: Set<String>,
        now: Long,
        out: MutableMap<String, MutableList<EpgProgram>>,
    ) {
        val parser = Xml.newPullParser()
        parser.setInput(input, null) // null → auto-detect encoding from the XML declaration

        var channel: String? = null
        var capture = false
        var start = 0L
        var stop = 0L
        var titleDone = false
        var inTitle = false
        var title = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "programme" -> {
                        channel = parser.getAttributeValue(null, "channel")
                        capture = channel != null && neededIds.contains(channel)
                        if (capture) {
                            start = parseTime(parser.getAttributeValue(null, "start"))
                            stop = parseTime(parser.getAttributeValue(null, "stop"))
                            titleDone = false
                            title = StringBuilder()
                        }
                    }
                    "title" -> if (capture && !titleDone) inTitle = true
                }

                XmlPullParser.TEXT -> if (inTitle) title.append(parser.text)

                XmlPullParser.END_TAG -> when (parser.name) {
                    "title" -> if (inTitle) {
                        inTitle = false
                        titleDone = true
                    }
                    "programme" -> {
                        if (capture && channel != null) {
                            val text = title.toString().trim()
                            // Keep what is still airing or yet to come; drop already-finished shows.
                            if (text.isNotEmpty() && (stop == 0L || stop >= now)) {
                                out.getOrPut(channel!!) { mutableListOf() }
                                    .add(EpgProgram(text, start, stop, nowFlag = false))
                            }
                        }
                        capture = false
                        channel = null
                    }
                }
            }
            event = parser.next()
        }
    }

    /**
     * Parses an XMLTV timestamp ("20260525200000 +0200" or bare UTC) to epoch millis, 0 if invalid.
     * Internal (not private) so it can be unit-tested directly.
     */
    internal fun parseTime(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        val v = value.trim()
        val digits = v.takeWhile { it.isDigit() }
        if (digits.length < 14) return 0L
        val offset = v.drop(digits.length).replace(" ", "")
        return runCatching {
            if (offset.isNotEmpty()) {
                SimpleDateFormat("yyyyMMddHHmmssZ", Locale.US).parse(digits + offset)?.time ?: 0L
            } else {
                SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
                    .apply { timeZone = TimeZone.getTimeZone("UTC") }
                    .parse(digits)?.time ?: 0L
            }
        }.getOrDefault(0L)
    }
}
