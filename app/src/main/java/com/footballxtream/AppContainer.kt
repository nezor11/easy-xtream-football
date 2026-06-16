package com.footballxtream

import android.content.Context
import androidx.room.Room
import com.footballxtream.data.ContentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.footballxtream.data.LogoRepository
import com.footballxtream.data.local.ALL_MIGRATIONS
import com.footballxtream.data.local.AppDatabase
import com.footballxtream.data.local.FavoriteFolderDao
import com.footballxtream.data.local.ProfileDao
import com.footballxtream.data.local.SettingsStore
import com.footballxtream.player.PlaybackSession
import com.footballxtream.player.PlayerEngine

/** Manual dependency container. One instance lives in [FootballXtreamApp]. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "football-xtream.db",
    )
        // Real migrations preserve saved profiles/favorites across app updates. A downgrade can't be
        // migrated forward, so only then do we fall back to recreating the database.
        .addMigrations(*ALL_MIGRATIONS)
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()

    val profileDao: ProfileDao = database.profileDao()
    val favoriteFolderDao: FavoriteFolderDao = database.favoriteFolderDao()
    val settingsStore: SettingsStore = SettingsStore(appContext)
    val logoRepository: LogoRepository = LogoRepository(appContext.cacheDir)
    val repository: ContentRepository = ContentRepository(appContext.cacheDir, logoRepository)
    val playbackSession: PlaybackSession = PlaybackSession()
    val playerEngine: PlayerEngine = PlayerEngine(appContext, settingsStore)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        encryptExistingCredentials()
    }

    // One-time pass for profiles saved before encryption existed: reading decrypts (plain text
    // passes through untouched), and re-saving writes them back through the encrypting converter,
    // so any legacy plaintext credential becomes ciphertext at rest.
    private fun encryptExistingCredentials() {
        scope.launch {
            if (settingsStore.credentialsEncrypted()) return@launch
            runCatching { profileDao.allOnce().forEach { profileDao.update(it) } }
            settingsStore.setCredentialsEncrypted()
        }
    }
}
