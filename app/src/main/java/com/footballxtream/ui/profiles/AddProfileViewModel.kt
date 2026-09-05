package com.footballxtream.ui.profiles

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.footballxtream.FootballXtreamApp
import com.footballxtream.R
import com.footballxtream.data.ContentRepository
import com.footballxtream.data.XtreamAuthException
import com.footballxtream.data.local.ProfileDao
import com.footballxtream.data.local.ProfileEntity
import com.footballxtream.data.local.ProfileType
import com.footballxtream.data.local.Secret
import com.footballxtream.model.XtreamProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

data class AddProfileUiState(
    val mode: String = ProfileType.XTREAM,
    val name: String = "",
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    /** Password shown in clear text (default on TV: typing blind with a remote is error-prone). */
    val showPassword: Boolean = false,
    val isConnecting: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false,
) {
    val isM3u: Boolean get() = mode == ProfileType.M3U
    val isDirect: Boolean get() = mode == ProfileType.DIRECT

    /** Both M3U and direct-URL modes use the single URL field. */
    val usesUrlField: Boolean get() = isM3u || isDirect

    val canSubmit: Boolean
        get() = !isConnecting && if (usesUrlField) {
            m3uUrl.isNotBlank()
        } else {
            server.isNotBlank() && username.isNotBlank() && password.isNotBlank()
        }
}

class AddProfileViewModel(
    private val profileDao: ProfileDao,
    private val repository: ContentRepository,
    private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AddProfileUiState(
            showPassword = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK),
        ),
    )
    val state: StateFlow<AddProfileUiState> = _state.asStateFlow()

    // Non-null when editing an existing profile; drives update-in-place instead of insert.
    private var editingId: Long? = null

    /** Loads an existing profile into the form so it can be edited. No-op for a new profile. */
    fun load(profileId: Long) {
        if (profileId < 0 || editingId != null) return
        viewModelScope.launch {
            val p = profileDao.byId(profileId) ?: return@launch
            editingId = p.id
            _state.update {
                it.copy(
                    mode = p.type,
                    name = p.name,
                    server = p.serverUrl.value,
                    username = p.username.value,
                    password = p.password.value,
                    m3uUrl = p.m3uUrl.value,
                    isEditing = true,
                )
            }
        }
    }

    fun onModeChange(mode: String) = _state.update { it.copy(mode = mode, error = null) }
    fun onNameChange(value: String) = _state.update { it.copy(name = value) }
    fun onServerChange(value: String) = _state.update { it.copy(server = value, error = null) }
    fun onUsernameChange(value: String) = _state.update { it.copy(username = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onM3uUrlChange(value: String) = _state.update { it.copy(m3uUrl = value, error = null) }
    fun onTogglePassword() = _state.update { it.copy(showPassword = !it.showPassword) }

    fun save(onSaved: () -> Unit) {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(isConnecting = true, error = null) }

        viewModelScope.launch {
            if (current.isDirect) {
                // A raw HLS/DASH URL: nothing to validate against (no #EXTM3U header), so just bind
                // it and let the player surface any error.
                val url = current.m3uUrl.trim()
                val name = current.name.ifBlank { defaultM3uName(url) }
                repository.bindDirect(url)
                repository.setActiveProfileName(name)
                persist(ProfileEntity(name = name, type = ProfileType.DIRECT, m3uUrl = Secret(url)))
                onSaved()
            } else if (current.isM3u) {
                val url = current.m3uUrl.trim()
                val name = current.name.ifBlank { defaultM3uName(url) }
                repository.validateM3u(url)
                    .onSuccess {
                        repository.setActiveProfileName(name)
                        persist(ProfileEntity(name = name, type = ProfileType.M3U, m3uUrl = Secret(url)))
                        onSaved()
                    }
                    .onFailure { fail(context.getString(R.string.error_m3u_load)) }
            } else {
                // Trim everything: a TV keyboard easily leaves a trailing space when accepting a
                // suggestion, and many panels reject "user " even when "user" is valid.
                val username = current.username.trim()
                val profile = XtreamProfile(
                    name = current.name.trim().ifBlank { username },
                    serverUrl = current.server.trim(),
                    username = username,
                    password = current.password.trim(),
                )
                repository.validateXtream(profile)
                    .onSuccess {
                        repository.setActiveProfileName(profile.name)
                        persist(
                            ProfileEntity(
                                name = profile.name,
                                type = ProfileType.XTREAM,
                                serverUrl = Secret(profile.serverUrl),
                                username = Secret(profile.username),
                                password = Secret(profile.password),
                            ),
                        )
                        onSaved()
                    }
                    .onFailure {
                        Log.w(TAG, "validateXtream failed: ${it.javaClass.simpleName}: ${it.message}")
                        fail(describeXtreamError(it))
                    }
            }
        }
    }

    /**
     * A readable default name for an unnamed M3U list, derived from its URL so two lists don't both
     * show "Lista M3U": the file name without extension (e.g. .../countries/es.m3u → "es"), falling
     * back to the host when the file name is generic (get.php, playlist, index…).
     */
    private fun defaultM3uName(url: String): String {
        val base = url.substringBefore('?').trimEnd('/').substringAfterLast('/').substringBeforeLast('.')
        val host = Regex("""https?://([^/:]+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.removePrefix("www.").orEmpty()
        val generic = setOf("", "get", "index", "playlist", "list", "tv", "iptv", "m3u", "m3u8", "live")
        return when {
            base.lowercase() !in generic -> base
            host.isNotBlank() -> host
            else -> context.getString(R.string.default_m3u_name)
        }
    }

    // Update the existing row when editing (preserving its id); otherwise insert a new profile.
    private suspend fun persist(entity: ProfileEntity) {
        val id = editingId
        if (id != null) profileDao.update(entity.copy(id = id)) else profileDao.upsert(entity)
    }

    /**
     * Tells the user WHICH part failed. Before, every failure (host blocked in their country,
     * https on an http port, pasted /player_api.php, wrong password) showed the same generic
     * "check server, username and password", which is impossible to act on from a remote.
     */
    private fun describeXtreamError(e: Throwable): String = when (e) {
        is XtreamAuthException -> context.getString(R.string.error_xtream_auth)
        is HttpException -> context.getString(R.string.error_xtream_http, e.code())
        is UnknownHostException -> context.getString(R.string.error_xtream_unreachable, "DNS")
        is SocketTimeoutException -> context.getString(R.string.error_xtream_unreachable, "timeout")
        is SSLException -> context.getString(R.string.error_xtream_unreachable, "TLS")
        is ConnectException -> context.getString(R.string.error_xtream_unreachable, "TCP")
        is IOException -> context.getString(R.string.error_xtream_unreachable, e.javaClass.simpleName)
        // Retrofit rejects a malformed base URL (e.g. a space in the host) with this.
        is IllegalArgumentException -> context.getString(R.string.error_xtream_unreachable, "URL")
        else -> context.getString(R.string.error_xtream_bad_response)
    }

    private fun fail(message: String) {
        _state.update { it.copy(isConnecting = false, error = message) }
    }

    companion object {
        private const val TAG = "AddProfileViewModel"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FootballXtreamApp
                AddProfileViewModel(app.container.profileDao, app.container.repository, app)
            }
        }
    }
}
