package com.footballxtream.ui.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
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
                                else -> viewModel.openMenu()
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

        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // The channel info (stats + now/next) can be hidden globally from the OK menu for a clean
            // view; the OK menu itself stays available regardless.
            if (ui.infoVisible) {
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
                OptionsMenu(
                    section = ui.menuSection,
                    options = ui.menuOptions,
                    selectedIndex = ui.menuSelectedIndex,
                )
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
                text = stringResource(R.string.menu_nav_hint),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0x99FFFFFF),
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            )
        }
        // Controls legend: only the first few times — fades in, stays a few seconds, fades out.
        AnimatedVisibility(
            visible = ui.showControlsHint && !ui.menuOpen,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.controls_legend),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0x99FFFFFF),
            )
        }
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
            )
        }
    }
}
