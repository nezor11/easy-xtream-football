package com.footballxtream.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.footballxtream.data.local.SettingsStore
import com.footballxtream.data.remote.XtreamClient
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Creates ExoPlayer instances tuned for IPTV: a shared bandwidth meter (so the Auto quality logic
 * can read a real throughput estimate) and a load control with generous pre-buffering to smooth out
 * the jittery delivery typical of Xtream live streams.
 *
 * Uses an OkHttp-backed data source so multi-hop, cross-host 302 redirects (common in IPTV CDNs
 * with tokenized URLs) are followed exactly like a normal client; the framework DefaultHttpDataSource
 * mishandled those chains and returned 401.
 */
@OptIn(UnstableApi::class)
class PlayerEngine(
    private val context: Context,
    private val settingsStore: SettingsStore,
) {
    val bandwidthMeter: DefaultBandwidthMeter = DefaultBandwidthMeter.Builder(context).build()

    // A fresh LoadControl per player: a single shared DefaultLoadControl across ExoPlayer instances
    // (each with its own playback thread) throws "Players that share the same LoadControl must share
    // the same playback thread", which would loop the player into an error/auto-skip storm.
    //
    // Buffer kept moderate (15–30 s) rather than huge: a live stream can't use a long look-ahead
    // anyway, and a 30 s cap roughly halves the in-memory buffer (~30 MB vs ~60 MB at high bitrate),
    // which matters on low-RAM TV boxes — while still absorbing the jitter typical of Xtream streams.
    private fun newLoadControl(): DefaultLoadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ 20_000,
            /* maxBufferMs = */ 50_000,
            // Build a bigger cushion before (re)starting so bursty/jittery links (e.g. 4G) coast through
            // dips instead of stalling. Slightly slower to start, but far steadier on unstable networks.
            /* bufferForPlaybackMs = */ 4_000,
            /* bufferForPlaybackAfterRebufferMs = */ 10_000,
        )
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        // Timeouts loose enough to survive a slow/weak network (a marginal Wi-Fi link needs more than a
        // few seconds to connect), but still bounded so a truly dead host fails over instead of hanging.
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", XtreamClient.USER_AGENT)
                    .build(),
            )
        }
        .build()

    // Counts the bytes actually pulled off the network so the UI can show the live download rate
    // (the bandwidth meter only exposes a smoothed estimate, not what's flowing right now).
    private val byteCounter = ByteCounter()

    private val httpDataSourceFactory: OkHttpDataSource.Factory =
        OkHttpDataSource.Factory(okHttpClient).also { it.setTransferListener(byteCounter) }

    /**
     * Media-usage audio attributes with [handleAudioFocus] on, so ExoPlayer requests system audio
     * focus on play and ABANDONS it on pause/stop — otherwise the audio focus (and Cast/system audio
     * routing) stays held after the app is backgrounded. [setHandleAudioBecomingNoisy] also pauses
     * when headphones/output are unplugged.
     */
    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    /** The display's current output mode (e.g. 1920x1080), to avoid decoding above what it shows. */
    private val displayMode: android.view.Display.Mode? by lazy {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
        dm?.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.mode
    }

    /** Output resolution of the device's display, defaulting to 1080p if unknown. */
    val displayWidth: Int get() = displayMode?.physicalWidth ?: 1920
    val displayHeight: Int get() = displayMode?.physicalHeight ?: 1080

    fun build(): ExoPlayer =
        ExoPlayer.Builder(context, mediaRenderersFactory(context))
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(newLoadControl())
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

    /** Current network throughput estimate in bits per second (smoothed; used by Auto). */
    fun bitrateEstimateBps(): Long = bandwidthMeter.bitrateEstimate

    /** Total bytes pulled from the network so far; deltas give the live download rate. */
    fun bytesTransferred(): Long = byteCounter.total

    /** Tallies every network byte ExoPlayer reads, for an instantaneous (not smoothed) rate. */
    private class ByteCounter : TransferListener {
        private val bytes = AtomicLong(0L)
        val total: Long get() = bytes.get()

        override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit
        override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit
        override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit
        override fun onBytesTransferred(
            source: DataSource,
            dataSpec: DataSpec,
            isNetwork: Boolean,
            bytesTransferred: Int,
        ) {
            if (isNetwork) bytes.addAndGet(bytesTransferred.toLong())
        }
    }
}
