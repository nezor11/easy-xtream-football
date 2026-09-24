package com.footballxtream.ui.player

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import kotlin.math.abs
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.footballxtream.R
import com.footballxtream.ui.components.isTv
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.Factory),
) {
    if (!viewModel.canPlay) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    // Timestamp of the OK key-down, to tell a short press (menu) from a long press (toggle favorite).
    // 0L = idle (no press in progress); -1L = long-press already handled on key-down.
    val okDownAt = remember { LongArray(1) }
    // A short OK opens the menu only if a second short OK doesn't follow within the system's
    // double-tap window: two quick OKs are play/pause (the Chromecast remote has no ⏯ key).
    val scope = rememberCoroutineScope()
    val doubleTapMs = LocalViewConfiguration.current.doubleTapTimeoutMillis
    val pendingOk = remember { arrayOfNulls<Job>(1) }

    // Back closes the options menu first; otherwise it leaves the player.
    BackHandler(enabled = ui.menuOpen) { viewModel.closeMenu() }
    BackHandler(enabled = !ui.menuOpen, onBack = onBack)

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    // Pause when the app is backgrounded so the audio stops (and system audio focus is released)
    // instead of playing on, then resume when it comes back to the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onBackground()
                Lifecycle.Event.ON_START -> viewModel.onForeground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Overlays sit 20 dp from the edges on a phone; on TV they stay inside the overscan-safe area.
    val overlayPadding = if (isTv()) PaddingValues(horizontal = 48.dp, vertical = 28.dp) else PaddingValues(20.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                // Any key press slides the Ko-fi "bug" away; the key still does its normal job.
                if (ui.showCoffeeBug && event.type == KeyEventType.KeyDown) viewModel.dismissCoffeeBug()
                // Media keys of TV remotes work whether or not the OK menu is open. Stop leaves the
                // player; the channel keys zap only while the menu is closed (▲▼ drive the menu there).
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.MediaPlayPause -> { viewModel.togglePlayPause(); return@onKeyEvent true }
                        Key.MediaPlay -> { viewModel.setPaused(false); return@onKeyEvent true }
                        Key.MediaPause -> { viewModel.setPaused(true); return@onKeyEvent true }
                        Key.MediaStop -> { onBack(); return@onKeyEvent true }
                        Key.ChannelUp -> if (!ui.menuOpen) { viewModel.nextChannel(); return@onKeyEvent true }
                        Key.ChannelDown -> if (!ui.menuOpen) { viewModel.previousChannel(); return@onKeyEvent true }
                        else -> Unit
                    }
                }
                if (ui.menuOpen) {
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> { viewModel.moveMenuSelection(-1); true }
                        Key.DirectionDown -> { viewModel.moveMenuSelection(1); true }
                        Key.DirectionLeft -> { viewModel.moveMenuSection(-1); true }
                        Key.DirectionRight -> { viewModel.moveMenuSection(1); true }
                        Key.DirectionCenter, Key.Enter -> { viewModel.confirmMenuSelection(); true }
                        else -> false
                    }
                } else {
                    val isOk = event.key == Key.DirectionCenter || event.key == Key.Enter
                    when {
                        // Short OK opens the menu; holding OK toggles the channel favorite. Detected
                        // by the native long-press flag (set on real long-presses and adb injection)
                        // and, as a fallback, by the key-down→key-up hold time (>= 450 ms).
                        isOk && event.type == KeyEventType.KeyDown -> {
                            val native = event.nativeKeyEvent
                            if (native.isLongPress) {
                                viewModel.toggleCurrentChannelFavorite()
                                okDownAt[0] = -1L
                            } else if (native.repeatCount == 0) {
                                okDownAt[0] = System.currentTimeMillis()
                            }
                            true
                        }
                        isOk && event.type == KeyEventType.KeyUp -> {
                            when {
                                // No key-down was recorded for this press (0L = idle): it belongs to
                                // another gesture — typically the OK that just confirmed and closed the
                                // menu, whose key-up only reaches this branch now that the menu is gone.
                                // Ignore it so confirming a menu option never toggles the favorite.
                                okDownAt[0] == 0L -> Unit
                                okDownAt[0] == -1L -> Unit
                                System.currentTimeMillis() - okDownAt[0] >= 450L ->
                                    viewModel.toggleCurrentChannelFavorite()
                                pendingOk[0]?.isActive == true -> {
                                    // Second short OK inside the window: it's a double press.
                                    pendingOk[0]?.cancel()
                                    pendingOk[0] = null
                                    viewModel.togglePlayPause()
                                }
                                else -> pendingOk[0] = scope.launch {
                                    delay(doubleTapMs)
                                    pendingOk[0] = null
                                    viewModel.openMenu()
                                }
                            }
                            okDownAt[0] = 0L
                            true
                        }
                        event.type == KeyEventType.KeyDown &&
                            event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_LAST_CHANNEL -> {
                            viewModel.jumpToLastChannel(); true
                        }
                        event.type == KeyEventType.KeyDown -> when (event.key) {
                            Key.DirectionLeft -> { viewModel.previousChannel(); true }
                            Key.DirectionRight -> { viewModel.nextChannel(); true }
                            Key.DirectionUp -> { viewModel.stepQuality(-1); true }
                            Key.DirectionDown -> { viewModel.stepQuality(1); true }
                            else -> false
                        }
                        else -> false
                    }
                }
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                    // Keep the device awake while the player is open so it doesn't go idle and put
                    // the TV into standby via HDMI-CEC (happens on dead/buffering channels too).
                    keepScreenOn = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
        )

        // Touch controls (phones/tablets), on a layer above the video so the PlayerView never sees
        // them: they mirror the remote — a tap is OK (open/close the menu), a double tap is
        // play/pause, a horizontal swipe is ◀▶ (channel, or menu section while the menu is open) and
        // a vertical swipe is ▲▼ (quality).
        // The overlays drawn later sit on top, so their own taps (menu options, QR) still win.
        val swipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(ui.menuOpen) {
                    detectTapGestures(
                        onDoubleTap = { if (!ui.menuOpen) viewModel.togglePlayPause() },
                        onTap = { if (ui.menuOpen) viewModel.closeMenu() else viewModel.openMenu() },
                    )
                }
                .pointerInput(ui.menuOpen) {
                    var dx = 0f
                    var dy = 0f
                    detectDragGestures(
                        onDragStart = { dx = 0f; dy = 0f },
                        onDrag = { change, amount -> change.consume(); dx += amount.x; dy += amount.y },
                        onDragEnd = {
                            when {
                                abs(dx) >= swipeThreshold && abs(dx) > abs(dy) * 1.5f -> when {
                                    ui.menuOpen -> viewModel.moveMenuSection(if (dx < 0) 1 else -1)
                                    dx < 0 -> viewModel.nextChannel()
                                    else -> viewModel.previousChannel()
                                }
                                abs(dy) >= swipeThreshold && abs(dy) > abs(dx) * 1.5f && !ui.menuOpen ->
                                    viewModel.stepQuality(if (dy < 0) -1 else 1)
                            }
                        },
                    )
                },
        )

        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(overlayPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // The channel info (stats + now/next) can be hidden globally from the OK menu for a clean
            // view; the OK menu itself stays available regardless. On a zap it's briefly revealed even
            // when hidden ([infoFlash]) so you always see what channel you landed on.
            if (ui.infoVisible || ui.infoFlash) {
                StatsOverlay(
                    channelName = ui.channelName,
                    channelPosition = ui.channelPosition,
                    emissionLabel = ui.emissionLabel,
                    throughputMbps = ui.throughputMbps,
                    resolution = ui.resolution,
                    isBuffering = ui.isBuffering,
                    isFavorite = ui.isFavorite,
                )
                ui.nowProgram?.let { now ->
                    EpgOverlay(now = now, next = ui.nextProgram)
                }
            }
            if (ui.menuOpen) {
                if (ui.menuCoffee) {
                    CoffeeMenuPanel(section = ui.menuSection)
                } else {
                    OptionsMenu(
                        section = ui.menuSection,
                        options = ui.menuOptions,
                        selectedIndex = ui.menuSelectedIndex,
                        onSelect = viewModel::selectMenuOption,
                    )
                }
            }
        }

        ui.errorMessage?.let { msg ->
            Text(
                text = stringResource(R.string.player_error_with_hint, msg),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE6EAEE),
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Centre of the screen: the radio view (there is no picture to show) and the paused label.
        val radioView = (ui.isRadio || ui.audioOnly) && ui.errorMessage == null
        if (radioView || ui.paused) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (radioView) {
                    RadioOverlay(
                        name = ui.channelName,
                        iconUrl = ui.channelIconUrl,
                        nowPlaying = ui.nowPlaying,
                        isBuffering = ui.isBuffering,
                    )
                }
                if (ui.paused) {
                    Text(
                        text = stringResource(R.string.player_paused),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFE6EAEE),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xE60A0E12))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        ui.notice?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE6EAEE),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xE60A0E12))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // While the menu is open, show its navigation hint.
        if (ui.menuOpen) {
            Text(
                text = stringResource(if (isTv()) R.string.menu_nav_hint else R.string.menu_nav_hint_touch),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0x99FFFFFF),
                modifier = Modifier.align(Alignment.BottomEnd).padding(overlayPadding),
            )
        }
        // Controls legend: only the first few times — fades in, stays a few seconds, fades out.
        AnimatedVisibility(
            visible = ui.showControlsHint && !ui.menuOpen,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(overlayPadding),
        ) {
            Text(
                text = stringResource(if (isTv()) R.string.controls_legend else R.string.controls_legend_touch),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0x99FFFFFF),
            )
        }
        // Ko-fi "bug": one shared card (same module) used both for the timed reminder and the OK-menu
        // "Café" section. Slides up from the bottom-right; any key dismisses it (sliding back down).
        AnimatedVisibility(
            visible = (ui.showCoffeeBug && !ui.menuOpen) || (ui.menuOpen && ui.menuCoffee),
            enter = slideInVertically(animationSpec = tween(450)) { it } + fadeIn(tween(450)),
            exit = slideOutVertically(animationSpec = tween(350)) { it } + fadeOut(tween(350)),
            modifier = Modifier.align(Alignment.BottomEnd).padding(overlayPadding),
        ) {
            CoffeeCard()
        }
    }
}

/**
 * What replaces the (black) picture while a radio station plays: the station logo when it has one,
 * otherwise a radio glyph; the station name; and the song/programme on air from the ICY metadata,
 * or an "audio only" line when the stream doesn't announce one.
 */
@Composable
private fun RadioOverlay(
    name: String,
    iconUrl: String?,
    nowPlaying: String?,
    isBuffering: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.widthIn(max = 560.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(168.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141A20)),
            contentAlignment = Alignment.Center,
        ) {
            var imageFailed by remember(iconUrl) { mutableStateOf(false) }
            if (iconUrl != null && !imageFailed) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    onError = { imageFailed = true },
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_radio),
                    contentDescription = stringResource(R.string.radio_badge_desc),
                    colorFilter = ColorFilter.tint(colors.primary),
                    modifier = Modifier.size(96.dp),
                )
            }
            // Small radio badge on the logo, so a logo alone still reads as "radio" (redundant when
            // the big glyph is already showing).
            if (iconUrl != null && !imageFailed) {
                Image(
                    painter = painterResource(R.drawable.ic_radio),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.primary),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(26.dp),
                )
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFFE6EAEE),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = when {
                !nowPlaying.isNullOrBlank() -> stringResource(R.string.radio_now_playing, nowPlaying)
                isBuffering -> "⟳"
                else -> stringResource(R.string.radio_audio_only)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (isBuffering) colors.primary else Color(0xCCE6EAEE),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatsOverlay(
    channelName: String,
    channelPosition: String,
    emissionLabel: String,
    throughputMbps: Double,
    resolution: String?,
    isBuffering: Boolean,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (isBuffering) MaterialTheme.colorScheme.primary else Color(0xCCE6EAEE)
    val style = MaterialTheme.typography.labelSmall
    val separator = "  •  "

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x990A0E12))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (channelName.isNotBlank()) {
            // Only the channel name stands out: a step larger and in the brand green.
            Text(
                text = if (isFavorite) "★ $channelName" else channelName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
            Text(separator, style = style, color = color)
        }
        if (channelPosition.isNotBlank()) {
            Text(channelPosition, style = style.copy(fontFeatureSettings = "tnum"), color = color, maxLines = 1)
            Text(separator, style = style, color = color)
        }
        Text("‹ $emissionLabel ›", style = style, color = color, maxLines = 1)
        Text(separator, style = style, color = color)
        // Tabular figures keep the digits steady; the leading zero keeps single-digit rates aligned.
        Text(
            text = "⬇ %04.1f Mbps".format(throughputMbps),
            style = style.copy(fontFeatureSettings = "tnum"),
            color = color,
            maxLines = 1,
        )
        resolution?.let {
            Text(separator, style = style, color = color)
            Text(it, style = style, color = color, maxLines = 1)
        }
        if (isBuffering) {
            Text(separator, style = style, color = color)
            Text("⟳", style = style, color = color)
        }
    }
}

@Composable
private fun EpgOverlay(now: String, next: String?, modifier: Modifier = Modifier) {
    val nowText = stringResource(R.string.epg_now, now)
    val nextText = if (!next.isNullOrBlank()) stringResource(R.string.epg_next_suffix, next) else ""
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x990A0E12))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = nowText + nextText,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xCCE6EAEE),
        )
    }
}

@Composable
private fun OptionsMenu(
    section: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .width(280.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE60A0E12))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Section header with ‹ › to hint that left/right switches between Calidad/Audio/Subtítulos.
        Text(
            text = "‹ $section ›",
            style = MaterialTheme.typography.labelMedium,
            color = colors.primary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Text(
                text = (if (selected) "● " else "○ ") + label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) colors.primary else Color(0xFFE6EAEE),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Tappable on touch screens; a full-width row so the target isn't just the text.
                modifier = Modifier.fillMaxWidth().clickable { onSelect(index) }.padding(vertical = 2.dp),
            )
        }
    }
}

/** The "Café" OK-menu section's bottom-left panel: same card format as the other sections, but it only
 *  carries the section header and how to turn the reminder off — the QR rides in the shared [CoffeeCard]
 *  that slides in bottom-right (see the AnimatedVisibility in the player). */
@Composable
private fun CoffeeMenuPanel(section: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .width(280.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE60A0E12))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "‹ $section ›",
            style = MaterialTheme.typography.labelMedium,
            color = colors.primary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = stringResource(R.string.coffee_disable_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xCCE6EAEE),
        )
    }
}

/** Shared Ko-fi card (QR + invite + thanks) used by both the timed reminder "bug" and the OK-menu
 *  "Café" section, so they look and animate identically. */
@Composable
private fun CoffeeCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE60A0E12)) // same opacity as the OK menu
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.qr_kofi),
            contentDescription = stringResource(R.string.support_qr_desc),
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(6.dp))
                // Tapping the QR opens Ko-fi directly (handy on a phone; harmless on TV — no browser).
                .clickable {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://" + context.getString(R.string.support_kofi_handle)),
                            ),
                        )
                    }
                }
                .background(Color.White)
                .padding(5.dp),
        )
        Column(
            modifier = Modifier.width(160.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.support_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.support_entry),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.coffee_thanks),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
