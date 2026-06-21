package com.footballxtream.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.footballxtream.FootballXtreamApp
import com.footballxtream.data.ContentRepository
import com.footballxtream.data.local.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: ContentRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    /** Whether the user has permanently silenced the Ko-fi reminder; drives the toggle label. */
    val coffeeReminderDismissed: StateFlow<Boolean> = settingsStore.coffeeReminderDismissed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Clears the channel/guide caches; [onDone] runs on the main thread when finished. */
    fun clearCache(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.clearCache()
            onDone()
        }
    }

    fun setCoffeeReminderDismissed(dismissed: Boolean) {
        viewModelScope.launch { settingsStore.setCoffeeReminderDismissed(dismissed) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FootballXtreamApp).container
                SettingsViewModel(container.repository, container.settingsStore)
            }
        }
    }
}
