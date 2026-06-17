package com.footballxtream.data

import java.net.URI

/**
 * Reduces a URL to `scheme://host[:port]/…` for logging, dropping userinfo, path and query — which is
 * where IPTV credentials live (M3U `?username=…&password=…`, or `user:pass@host`). So a failed-request
 * log line can name the server without ever writing credentials to logcat (warnings survive release).
 */
internal fun redactUrl(url: String): String = runCatching {
    val uri = URI(url.trim())
    val host = uri.host ?: return "<url>"
    val port = if (uri.port > 0) ":${uri.port}" else ""
    "${uri.scheme}://$host$port/…"
}.getOrDefault("<url>")
