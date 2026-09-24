package com.footballxtream.data

/**
 * The two sample playlists offered on the empty profiles screen ("Try with sample playlists").
 * Nothing is bundled or hosted by the app: they are public lists of free-to-air streams, added on
 * the user's request as ordinary M3U profiles that can be edited or deleted like any other.
 */
object SampleLists {
    /** iptv-org's community-maintained list of free-to-air sports channels. */
    const val SPORTS_TV_URL = "https://iptv-org.github.io/iptv/categories/sports.m3u"

    /** Sports talk radio, official public streams of the stations; kept in this project's repo. */
    const val SPORTS_RADIO_URL =
        "https://raw.githubusercontent.com/nezor11/easy-xtream-football/main/docs/playlists/sports-radio.m3u"
}
