package com.footballxtream.data

import com.footballxtream.data.local.ProfileDao
import com.footballxtream.data.local.ProfileEntity
import com.footballxtream.data.local.ProfileType
import com.footballxtream.data.local.Secret

/**
 * The two sample playlists offered when there is no profile yet ("Try with sample playlists").
 * Nothing is bundled or hosted by the app: they are public lists of free-to-air streams, added on
 * the user's request as ordinary M3U profiles that can be edited or deleted like any other.
 */
object SampleLists {
    /** iptv-org's community-maintained list of free-to-air sports channels. */
    const val SPORTS_TV_URL = "https://iptv-org.github.io/iptv/categories/sports.m3u"

    /** Sports talk radio, official public streams of the stations; kept in this project's repo. */
    const val SPORTS_RADIO_URL =
        "https://raw.githubusercontent.com/nezor11/easy-xtream-football/main/docs/playlists/sports-radio.m3u"

    /** Inserts both sample profiles with the given (localized) names. */
    suspend fun add(profileDao: ProfileDao, sportsName: String, radioName: String) {
        profileDao.upsert(ProfileEntity(name = sportsName, type = ProfileType.M3U, m3uUrl = Secret(SPORTS_TV_URL)))
        profileDao.upsert(ProfileEntity(name = radioName, type = ProfileType.M3U, m3uUrl = Secret(SPORTS_RADIO_URL)))
    }
}
