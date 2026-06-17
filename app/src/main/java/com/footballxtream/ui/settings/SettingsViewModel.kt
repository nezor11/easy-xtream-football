package com.footballxtream.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.footballxtream.FootballXtreamApp
import com.footballxtream.data.ContentRepository
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: ContentRepository) : ViewModel() {

    /** Clears the channel/guide caches; [onDone] runs on the main thread when finished. */
    fun clearCache(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.clearCache()
            onDone()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as FootballXtreamApp).container
                SettingsViewModel(container.repository)
            }
        }
    }
}
