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

    val profiles: StateFlow<List<ProfileEntity>> = profileDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun select(profile: ProfileEntity, onSelected: () -> Unit) {
        repository.bindProfile(profile)
        viewModelScope.launch { settingsStore.setLastProfileId(profile.id) }
        onSelected()
    }

    fun delete(profile: ProfileEntity) {
        viewModelScope.launch { profileDao.delete(profile) }
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
