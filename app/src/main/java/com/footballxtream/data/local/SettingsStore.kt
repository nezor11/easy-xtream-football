package com.footballxtream.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.footballxtream.model.QualityMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val QUALITY_MODE = stringPreferencesKey("quality_mode")
        val BANDWIDTH_BPS = longPreferencesKey("bandwidth_bps")
        val RECENT_CHANNELS = stringPreferencesKey("recent_channel_keys")
        val FAVORITE_CHANNELS = stringPreferencesKey("favorite_channel_keys")
        val LAST_PROFILE_ID = longPreferencesKey("last_profile_id")
        val PLAYER_HINTS_SHOWN = intPreferencesKey("player_hints_shown")
        val CREDENTIALS_ENCRYPTED = booleanPreferencesKey("credentials_encrypted")
        val CHANNEL_INFO_VISIBLE = booleanPreferencesKey("channel_info_visible")
        val COFFEE_REMINDER_DISMISSED = booleanPreferencesKey("coffee_reminder_dismissed")
    }

    /** Whether the on-screen channel info (stats + now/next guide) is shown in the player. */
    val channelInfoVisible: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.CHANNEL_INFO_VISIBLE] ?: true
    }

    suspend fun setChannelInfoVisible(visible: Boolean) {
        context.settingsDataStore.edit { it[Keys.CHANNEL_INFO_VISIBLE] = visible }
    }

    /** Whether the user permanently silenced the Ko-fi donation reminder ("bug"). */
    val coffeeReminderDismissed: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.COFFEE_REMINDER_DISMISSED] ?: false
    }

    suspend fun setCoffeeReminderDismissed(dismissed: Boolean) {
        context.settingsDataStore.edit { it[Keys.COFFEE_REMINDER_DISMISSED] = dismissed }
    }

    val qualityMode: Flow<QualityMode> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.QUALITY_MODE]?.let { runCatching { QualityMode.valueOf(it) }.getOrNull() }
            ?: QualityMode.AUTO
    }

    suspend fun setQualityMode(mode: QualityMode) {
        context.settingsDataStore.edit { it[Keys.QUALITY_MODE] = mode.name }
    }

    /** Keys of recently watched channels, most-recent first (max [MAX_RECENT]). */
    val recentChannelKeys: Flow<List<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.RECENT_CHANNELS].orEmpty().split('\n').filter { it.isNotBlank() }
    }

    /** Records [key] as the most recent channel, de-duplicating and capping the history length. */
    suspend fun pushRecentChannel(key: String) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_CHANNELS].orEmpty().split('\n').filter { it.isNotBlank() }
            val updated = (listOf(key) + current.filterNot { it == key }).take(MAX_RECENT)
            prefs[Keys.RECENT_CHANNELS] = updated.joinToString("\n")
        }
    }

    /** Keys of channels marked as favorite, in the user's chosen order (newest added last). */
    val favoriteChannelKeys: Flow<List<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.FAVORITE_CHANNELS].orEmpty().split('\n').filter { it.isNotBlank() }
    }

    /** Adds the channel to the end of favorites, or removes it if already there. */
    suspend fun toggleFavoriteChannel(key: String) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_CHANNELS].orEmpty()
                .split('\n').filter { it.isNotBlank() }.toMutableList()
            if (current.remove(key)) {
                // already a favorite → unfavorite
            } else {
                current.add(key)
            }
            prefs[Keys.FAVORITE_CHANNELS] = current.joinToString("\n")
        }
    }

    /** Moves a favorite by [delta] positions (-1 left, +1 right); a no-op at the list edges. */
    suspend fun moveFavoriteChannel(key: String, delta: Int) {
        context.settingsDataStore.edit { prefs ->
            val list = prefs[Keys.FAVORITE_CHANNELS].orEmpty()
                .split('\n').filter { it.isNotBlank() }.toMutableList()
            val i = list.indexOf(key)
            val j = i + delta
            if (i < 0 || j < 0 || j > list.lastIndex) return@edit
            list[i] = list[j].also { list[j] = list[i] }
            prefs[Keys.FAVORITE_CHANNELS] = list.joinToString("\n")
        }
    }

    /** How many times the player has shown the on-screen controls legend. */
    suspend fun playerHintsShown(): Int =
        context.settingsDataStore.data.first()[Keys.PLAYER_HINTS_SHOWN] ?: 0

    suspend fun incrementPlayerHintsShown() {
        context.settingsDataStore.edit { it[Keys.PLAYER_HINTS_SHOWN] = (it[Keys.PLAYER_HINTS_SHOWN] ?: 0) + 1 }
    }

    /** Whether the one-time pass that encrypts pre-existing profile credentials has run. */
    suspend fun credentialsEncrypted(): Boolean =
        context.settingsDataStore.data.first()[Keys.CREDENTIALS_ENCRYPTED] ?: false

    suspend fun setCredentialsEncrypted() {
        context.settingsDataStore.edit { it[Keys.CREDENTIALS_ENCRYPTED] = true }
    }

    /** Id of the last profile the user entered, or null. Used to skip the picker on startup. */
    suspend fun lastProfileId(): Long? =
        context.settingsDataStore.data.first()[Keys.LAST_PROFILE_ID]

    suspend fun setLastProfileId(id: Long) {
        context.settingsDataStore.edit { it[Keys.LAST_PROFILE_ID] = id }
    }

    /** Last measured network throughput in bits/sec, 0 if never measured. */
    suspend fun bandwidthBps(): Long =
        context.settingsDataStore.data.first()[Keys.BANDWIDTH_BPS] ?: 0L

    suspend fun setBandwidthBps(bps: Long) {
        if (bps <= 0) return
        context.settingsDataStore.edit { it[Keys.BANDWIDTH_BPS] = bps }
    }

    private companion object {
        const val MAX_RECENT = 10
    }
}
