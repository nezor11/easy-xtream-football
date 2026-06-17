package com.footballxtream.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory

/**
 * "full" flavor: renderers backed by the NextLib FFmpeg extension. EXTENSION_RENDERER_MODE_ON keeps
 * the device's hardware decoders first (e.g. AAC, H.264) and falls back to the software FFmpeg
 * decoders only when no hardware one exists — which is the case for the AC-3 / E-AC-3 (Dolby) and
 * MP2 audio that many IPTV channels use, that would otherwise be silent.
 */
@OptIn(UnstableApi::class)
fun mediaRenderersFactory(context: Context): DefaultRenderersFactory =
    NextRenderersFactory(context)
        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
