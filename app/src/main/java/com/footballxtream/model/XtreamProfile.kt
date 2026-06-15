package com.footballxtream.model

/**
 * Credentials for a single Xtream Codes account.
 *
 * [serverUrl] is the panel base, e.g. "http://host:8080" (no trailing slash, no /player_api.php).
 */
data class XtreamProfile(
    val name: String,
    val serverUrl: String,
    val username: String,
    val password: String,
) {
    val isComplete: Boolean
        get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}
