package com.footballxtream.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.footballxtream.FootballXtreamApp
import com.footballxtream.data.ContentRepository
import com.footballxtream.data.local.ProfileDao
import com.footballxtream.data.local.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StartState { LOADING, HAS_PROFILES, NO_PROFILES, GO_CHANNELS }

/**
 * Picks the start destination: jump straight into the last-used profile's channels when one is
 * remembered, otherwise the profile picker (or add-profile when there are none).
 */
class RootViewModel(
    private val profileDao: ProfileDao,
    private val repository: ContentRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(StartState.LOADING)
    val state: StateFlow<StartState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (profileDao.count() == 0) {
                _state.value = StartState.NO_PROFILES
                return@launch
            }
            // If a last profile is remembered and still exists, bind it and skip the picker.
            val last = settingsStore.lastProfileId()?.let { profileDao.byId(it) }
            _state.value = if (last != null) {
                repository.bindProfile(last)
                StartState.GO_CHANNELS
            } else {
                StartState.HAS_PROFILES
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FootballXtreamApp).container
                RootViewModel(container.profileDao, container.repository, container.settingsStore)
            }
        }
    }
}
