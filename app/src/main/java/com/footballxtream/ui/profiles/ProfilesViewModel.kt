package com.footballxtream.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.footballxtream.FootballXtreamApp
import com.footballxtream.data.ContentRepository
import com.footballxtream.data.local.ProfileDao
import com.footballxtream.data.local.ProfileEntity
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import com.footballxtream.data.local.Secret
import com.footballxtream.data.local.ProfileType
import com.footballxtream.data.SampleLists
import com.footballxtream.data.local.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfilesViewModel(
    private val profileDao: ProfileDao,
    private val repository: ContentRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    // True once the database has answered at least once, so the screen can tell "no profiles yet"
    // (offer the sample playlists) from the momentarily empty flow while loading.
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    val profiles: StateFlow<List<ProfileEntity>> = profileDao.observeAll()
        .onEach { _loaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun select(profile: ProfileEntity, onSelected: () -> Unit) {
        repository.bindProfile(profile)
        viewModelScope.launch { settingsStore.setLastProfileId(profile.id) }
        onSelected()
    }

    fun delete(profile: ProfileEntity) {
        viewModelScope.launch { profileDao.delete(profile) }
    }

    /** Adds the two sample M3U profiles (free-to-air sports TV and sports radio). */
    fun addSampleLists(sportsName: String, radioName: String) {
        viewModelScope.launch {
            profileDao.upsert(
                ProfileEntity(name = sportsName, type = ProfileType.M3U, m3uUrl = Secret(SampleLists.SPORTS_TV_URL)),
            )
            profileDao.upsert(
                ProfileEntity(name = radioName, type = ProfileType.M3U, m3uUrl = Secret(SampleLists.SPORTS_RADIO_URL)),
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FootballXtreamApp).container
                ProfilesViewModel(container.profileDao, container.repository, container.settingsStore)
            }
        }
    }
}
