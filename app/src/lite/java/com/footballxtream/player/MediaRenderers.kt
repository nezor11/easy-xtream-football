package com.footballxtream.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory

/**
 * "lite" flavor: stock Media3 renderers, no bundled FFmpeg. Much smaller APK, but audio codecs without
 * a hardware decoder (AC-3 / E-AC-3 / DTS / MP2 — common on IPTV) will be silent. EXTENSION_RENDERER
 * mode stays OFF since there are no extension renderers to fall back to.
 */
@OptIn(UnstableApi::class)
fun mediaRenderersFactory(context: Context): DefaultRenderersFactory =
    DefaultRenderersFactory(context)
