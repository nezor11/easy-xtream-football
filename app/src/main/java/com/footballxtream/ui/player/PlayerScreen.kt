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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.unit.Dp
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
import com.footballxtream.ui.components.findActivity
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
    // While paused, a single OK/tap just resumes (no menu). The moment of that resume is kept so the
    // second press of a habitual double-OK, arriving right after, is swallowed instead of opening the menu.
    val resumedAt = remember { LongArray(1) }

    // A coffee picked in the Café section: open the Google Play purchase sheet (needs the Activity).
    val context = LocalContext.current
    LaunchedEffect(ui.coffeeToBuy) {
        val product = ui.coffeeToBuy ?: return@LaunchedEffect
        viewModel.coffeePurchaseLaunched()
        context.findActivity()?.let { viewModel.buyCoffee(it, product) }
    }

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

    // Measured height of everything stacked along the bottom edge (channel info + the touch menu
    // sheet). The centred overlays keep out of it, so in landscape — where the sheet takes the lower
    // half — the radio card no longer sits on top of the section tabs.
    var bottomStackPx by remember { mutableStateOf(0) }
    val centreInset = if (!isTv() && ui.menuOpen) {
        with(LocalDensity.current) { bottomStackPx.toDp() }
    } else {
        0.dp
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                // While the reminder card is on screen it acts as a button: OK opens the Café section
                // right away. Any other key just slides it away and then does its normal job.
                if (ui.showCoffeeBug && event.type == KeyEventType.KeyDown) {
                    val okOnBug = !ui.menuOpen &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter) &&
                        !event.nativeKeyEvent.isLongPress
                    viewModel.dismissCoffeeBug()
                    if (okOnBug) {
                        viewModel.openCoffeeSection()
                        // Swallow the key-up of this same press so it doesn't also toggle the favorite.
                        okDownAt[0] = -1L
                        return@onKeyEvent true
                    }
                }
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
                                ui.paused -> {
                                    // Paused: OK resumes straight away, no menu.
                                    pendingOk[0]?.cancel()
                                    pendingOk[0] = null
                                    resumedAt[0] = System.currentTimeMillis()
                                    viewModel.setPaused(false)
                                }
                                System.currentTimeMillis() - resumedAt[0] < doubleTapMs -> Unit
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
                .pointerInput(ui.menuOpen, ui.paused) {
                    detectTapGestures(
                        onDoubleTap = { if (!ui.menuOpen) viewModel.togglePlayPause() },
                        onTap = {
                            when {
                                ui.menuOpen -> viewModel.closeMenu()
                                ui.paused -> viewModel.setPaused(false) // paused: a tap resumes, no menu
                                else -> viewModel.openMenu()
                            }
                        },
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

        val touch = !isTv()
        val landscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp

        ui.errorMessage?.let { msg ->
            Text(
                text = stringResource(R.string.player_error_with_hint, msg),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE6EAEE),
                textAlign = TextAlign.Center,
                // Same dark pill as the rest: an error printed straight onto the video was the one
                // message you most need to read and the hardest one to read.
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE60A0E12))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        // Centre of the screen: the radio view (there is no picture to show) and the paused label.
        val radioView = (ui.isRadio || ui.audioOnly) && ui.errorMessage == null
        if (radioView || ui.paused) {
            Column(
                // Centred in whatever is left above the sheet, not in the whole screen: in landscape
                // the sheet takes the lower half and the radio card used to sit right on top of the
                // section tabs.
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = centreInset)
                    // unbounded: the card keeps its natural size and just moves up. Without this the
                    // reduced area squeezed it and the radio glyph visibly shrank when the menu rose.
                    .wrapContentSize(Alignment.Center, unbounded = true),
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

        // Everything that lives along the bottom edge, stacked in one column so nothing can land on
        // top of anything else. On a phone the menu is a full-width sheet at the very bottom and the
        // channel info rides above it; on TV the info and the 280 dp menu card keep the old layout.
        // Drawn *after* the centred overlays on purpose: when the radio card does not fit above the
        // sheet, the sheet covers it instead of the card covering the section tabs.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .onSizeChanged { bottomStackPx = it.height },
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (touch) {
                // On a phone held upright the coffee reminder (the full card) and the controls legend
                // join the stack instead of floating bottom-right: absolutely positioned, they landed
                // on the channel info. They are never on screen together (the legend hides while the
                // reminder is up). Held sideways there is room beside the channel info, so a small
                // reminder floats in the corner like on TV (see the block after this column).
                AnimatedVisibility(
                    visible = ui.showCoffeeBug && !ui.menuOpen && !landscape,
                    enter = slideInVertically(animationSpec = tween(450)) { it } + fadeIn(tween(450)),
                    exit = slideOutVertically(animationSpec = tween(350)) { it } + fadeOut(tween(350)),
                ) {
                    CoffeeCard(
                        showQr = !ui.coffeeViaBilling,
                        compact = true,
                        onOpenCoffee = viewModel::openCoffeeSection,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                AnimatedVisibility(
                    visible = ui.showControlsHint && !ui.menuOpen && !ui.showCoffeeBug,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut(),
                ) {
                    HintChips(
                        legend = stringResource(R.string.controls_legend_touch),
                        maxWidth = 520.dp,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(overlayPadding),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // On TV the channel info and the remote's help share one row, bottom-aligned. They used
                // to drift apart: the help was pinned to the bottom-right corner while the info was
                // pushed up by the menu card underneath it, so opening the menu left them at different
                // heights.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // The channel info (stats + now/next) can be hidden globally from the OK menu for
                        // a clean view; the OK menu itself stays available regardless. On a zap it's
                        // briefly revealed even when hidden ([infoFlash]) so you always see what channel
                        // you landed on.
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
                    }
                    if (!touch) {
                        // Menu open: how to move around it. Menu closed: the controls legend, only the
                        // first few times. Never while the coffee card holds that same corner.
                        if (ui.menuOpen) {
                            if (!ui.menuCoffee) {
                                HintChips(legend = stringResource(R.string.menu_nav_hint), maxWidth = 460.dp)
                            }
                        } else {
                            AnimatedVisibility(
                                visible = ui.showControlsHint && !ui.showCoffeeBug,
                                enter = fadeIn() + slideInVertically { it / 2 },
                                exit = fadeOut(),
                            ) {
                                HintChips(legend = stringResource(R.string.controls_legend), maxWidth = 620.dp)
                            }
                        }
                    }
                }
                if (ui.menuOpen && !touch) {
                    if (ui.menuCoffee && !ui.coffeeViaBilling) {
                        CoffeeMenuPanel(
                            section = ui.menuSection,
                            onStepSection = viewModel::moveMenuSection,
                        )
                    } else {
                        OptionsMenu(
                            section = ui.menuSection,
                            options = ui.menuOptions,
                            selectedIndex = ui.menuSelectedIndex,
                            onSelect = viewModel::selectMenuOption,
                            onStepSection = viewModel::moveMenuSection,
                        )
                    }
                }
            }
            if (ui.menuOpen && touch) {
                TouchMenuSheet(
                    sections = ui.menuSections,
                    sectionIndex = ui.menuSectionIndex,
                    options = ui.menuOptions,
                    selectedIndex = ui.menuSelectedIndex,
                    showCoffeeCard = ui.menuCoffee && !ui.coffeeViaBilling,
                    onSelectSection = viewModel::selectMenuSection,
                    onSelectOption = viewModel::selectMenuOption,
                    onStepSection = viewModel::moveMenuSection,
                    onOpenCoffee = viewModel::openCoffeeSection,
                )
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

        // While the menu is open, show its navigation hint — on TV only: the touch sheet says the same
        // thing with tappable tabs, and on a phone this line had nowhere to go (the 280 dp card left
        // it ~60 dp of width in portrait, so it wrapped over the menu).
        // Controls legend: only the first few times — fades in, stays a few seconds, fades out.
        // Hidden while the coffee reminder is up: both used to be anchored bottom-right, and the card
        // (drawn later) simply covered the legend.
        // Ko-fi "bug": one shared card (same module) used both for the timed reminder and the OK-menu
        // "Café" section. Slides up from the bottom-right; any key dismisses it (sliding back down).
        AnimatedVisibility(
            // TV, plus the timed reminder on a phone held sideways: a small card in the corner, like
            // on TV, instead of a full-width one that climbed over the radio card. Upright, the
            // reminder rides in the bottom stack; the Café section always lives inside the sheet.
            visible = (!touch && ((ui.showCoffeeBug && !ui.menuOpen) || (ui.menuOpen && ui.menuCoffee))) ||
                (touch && landscape && ui.showCoffeeBug && !ui.menuOpen),
            enter = slideInVertically(animationSpec = tween(450)) { it } + fadeIn(tween(450)),
            exit = slideOutVertically(animationSpec = tween(350)) { it } + fadeOut(tween(350)),
            modifier = Modifier.align(Alignment.BottomEnd).padding(overlayPadding),
        ) {
            CoffeeCard(
                showQr = !ui.coffeeViaBilling,
                // Small QR in the floating reminder; a scannable one when the Café section is open.
                compact = !ui.menuOpen,
                small = touch,
                onOpenCoffee = viewModel::openCoffeeSection,
            )
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
    // The TV sizes read well from the sofa; on a phone the same card filled the picture, so
    // everything comes down a notch (logo box, glyph, badge, both lines, spacing).
    val tv = isTv()
    Column(
        modifier = modifier.widthIn(max = 560.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (tv) 14.dp else 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (tv) 168.dp else 112.dp)
                .clip(RoundedCornerShape(if (tv) 24.dp else 18.dp))
                .background(Color(0xFF141A20)),
            contentAlignment = Alignment.Center,
        ) {
            var imageFailed by remember(iconUrl) { mutableStateOf(false) }
            if (iconUrl != null && !imageFailed) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    onError = { imageFailed = true },
                    modifier = Modifier.fillMaxSize().padding(if (tv) 20.dp else 14.dp),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_radio),
                    contentDescription = stringResource(R.string.radio_badge_desc),
                    colorFilter = ColorFilter.tint(colors.primary),
                    modifier = Modifier.size(if (tv) 96.dp else 64.dp),
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
                        .padding(if (tv) 10.dp else 7.dp)
                        .size(if (tv) 26.dp else 20.dp),
                )
            }
        }
        Text(
            text = name,
            style = if (tv) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
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
            style = if (tv) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
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

/**
 * A help line drawn as a card of chips instead of one long string of text. Used for the touch and
 * the remote control legends and for the menu's navigation hint, so every device gets the same look.
 *
 * It reuses the existing strings rather than adding new ones to all 24 locales: every translation is
 * written as `gesture: action  ·  gesture: action  …`, so each "·" fragment becomes a chip and the
 * part before the colon is picked out as the gesture, shown in the brand green. The chips match the
 * section tabs of the menu sheet, so the hint looks like part of the app and not like a debug string
 * printed over the video. A fragment without a colon (the navigation hint is written that way in
 * several locales) simply renders whole.
 */
@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HintChips(legend: String, maxWidth: Dp, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val items = remember(legend) {
        legend.split('·').mapNotNull { fragment ->
            val text = fragment.trim()
            if (text.isEmpty()) return@mapNotNull null
            // ':' in most locales, '：' in Chinese; French writes " : ", which trim() handles.
            val halves = text.split(':', '：', limit = 2)
            if (halves.size == 2 && halves[1].isNotBlank()) {
                halves[0].trim() to halves[1].trim()
            } else {
                "" to text
            }
        }
    }
    // Deliberately the same recipe as StatsOverlay (the channel info): 8 dp corners, the same
    // translucent black, the same 10/5 padding and the same labelSmall body with the accent in
    // labelMedium green. Each fragment is its own pill, with no card around them, so a hint sitting
    // next to the channel info reads as the same kind of object instead of a heavier, darker block.
    FlowRow(
        modifier = modifier.widthIn(max = maxWidth),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { (gesture, action) ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x990A0E12))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (gesture.isNotEmpty()) {
                    Text(
                        text = gesture,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        maxLines = 1,
                    )
                }
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xCCE6EAEE),
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The touch (phone/tablet) form of the OK menu: a full-width sheet pinned to the bottom edge.
 *
 * It replaces the 280 dp card, which in portrait took most of the width and left the help lines and
 * the coffee card fighting for the same bottom-right corner. Here every section is a tappable tab, so
 * nothing has to be explained in writing, and the Café section is a row inside the sheet instead of a
 * card floating over it. Swiping across the sheet still changes section, and so does the D-pad on the
 * rare touch device that has one, because the key handler is untouched.
 */
@Composable
private fun TouchMenuSheet(
    sections: List<String>,
    sectionIndex: Int,
    options: List<String>,
    selectedIndex: Int,
    showCoffeeCard: Boolean,
    onSelectSection: (Int) -> Unit,
    onSelectOption: (Int) -> Unit,
    onStepSection: (Int) -> Unit,
    onOpenCoffee: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xF20A0E12))
            // Swallow taps: without this a tap on the sheet's own background would reach the
            // full-screen gesture layer underneath and close the menu the user just opened.
            .pointerInput(Unit) { detectTapGestures { } }
            .sectionSwipe(onStepSection)
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Grabber: says "this panel belongs to the bottom edge" without a word of text.
        Box(
            modifier = Modifier
                .padding(vertical = 10.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x66FFFFFF)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            sections.forEachIndexed { index, label ->
                val current = index == sectionIndex
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (current) colors.onPrimary else Color(0xFFE6EAEE),
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (current) colors.primary else Color(0x1AFFFFFF))
                        .clickable { onSelectSection(index) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
        // The list is capped at 40 % of the screen so the sheet never swallows the picture. That
        // matters in landscape, where 260 dp of options alone is most of the height.
        val listMax = (LocalConfiguration.current.screenHeightDp * 0.4f).dp.coerceAtMost(260.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = listMax)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (showCoffeeCard) {
                // No billing (Amazon builds, or Play unavailable): the QR is the whole section, so it
                // rides inside the sheet at full size instead of as a card on top of it.
                CoffeeCard(
                    showQr = true,
                    compact = false,
                    onOpenCoffee = onOpenCoffee,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                options.forEachIndexed { index, label ->
                    val selected = index == selectedIndex
                    Text(
                        text = (if (selected) "●  " else "○  ") + label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selected) colors.primary else Color(0xFFE6EAEE),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        // 48 dp-tall rows: the old 2 dp padding gave a target far under the minimum.
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectOption(index) }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}

/**
 * Horizontal swipe on the menu card itself, to change section.
 *
 * The full-screen gesture layer lives *below* the overlays in z-order, and Compose stops hit-testing
 * at the topmost sibling it hits, so a swipe that starts on the menu card never reached it: on a phone
 * the card covers the bottom-left corner and its option rows are `clickable`, which is exactly where
 * a thumb lands. The card therefore carries its own detector. It sits on the card (the parent), not on
 * the rows, so it still sees the drag after the rows decline it: `clickable` cancels its press on slop
 * without consuming the movement, and the Main pass travels child → parent.
 */
private fun Modifier.sectionSwipe(onStepSection: (Int) -> Unit): Modifier = this.pointerInput(Unit) {
    // PointerInputScope is a Density, so the threshold is resolved here without a composed{} wrapper.
    val threshold = 40.dp.toPx()
    var dx = 0f
    detectHorizontalDragGestures(
        onDragStart = { dx = 0f },
        onHorizontalDrag = { change, amount -> change.consume(); dx += amount },
        onDragEnd = { if (abs(dx) >= threshold) onStepSection(if (dx < 0) 1 else -1) },
    )
}

/**
 * "‹ Sección ›" header. The arrows are real tap targets on touch screens (they looked tappable and
 * were not, which is half of why the menu felt broken on a phone); on TV they stay decorative so they
 * never become focusable and steal the D-pad from the player's key handler.
 */
@Composable
private fun SectionHeader(section: String, onStepSection: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val touch = !isTv()
    val arrow: @Composable (String, Int) -> Unit = { glyph, step ->
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleMedium,
            color = colors.primary,
            modifier = Modifier
                .then(if (touch) Modifier.clickable { onStepSection(step) } else Modifier)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        arrow("‹", -1)
        Text(
            text = section,
            style = MaterialTheme.typography.labelMedium,
            color = colors.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        arrow("›", 1)
    }
}

@Composable
private fun OptionsMenu(
    section: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onStepSection: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .width(280.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE60A0E12))
            .sectionSwipe(onStepSection)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SectionHeader(section = section, onStepSection = onStepSection)
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
private fun CoffeeMenuPanel(
    section: String,
    onStepSection: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(280.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE60A0E12))
            .sectionSwipe(onStepSection)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SectionHeader(section = section, onStepSection = onStepSection)
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
private fun CoffeeCard(
    showQr: Boolean,
    compact: Boolean,
    onOpenCoffee: () -> Unit,
    modifier: Modifier = Modifier,
    // Phone reminder: the TV card scaled down to stay out of the way on a small screen — a 40 dp
    // cup, one label-sized line, no title and no thanks line (the Café section has them).
    small: Boolean = false,
) {
    val context = LocalContext.current
    val openSite = {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.support_site_url))),
            )
        }
        Unit
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE60A0E12)) // same opacity as the OK menu
            // With Play billing the whole card is the button (tap on a phone, OK on TV — see the key
            // handler). With the QR there is nothing to open: the QR keeps its own tap → Ko-fi.
            .then(if (showQr || !compact) Modifier else Modifier.clickable { onOpenCoffee() })
            .padding(if (small) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (small) 10.dp else 12.dp),
    ) {
        // The QR comes first: it is what the viewer is meant to scan with a phone. It opens the app's
        // own site (which invites to Ko-fi), never a payment page. On a phone you can't scan your own
        // screen, so it only shows where it is the only way to give, and a tap opens the site instead.
        if (showQr || isTv()) {
            Image(
                painter = painterResource(R.drawable.qr_site),
                contentDescription = stringResource(R.string.support_qr_desc),
                modifier = Modifier
                    .size(if (compact) 88.dp else 148.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { openSite() }
                    .background(Color.White)
                    .padding(5.dp),
            )
        }
        // The cup rides along only in the floating reminder, at the QR's size so neither dwarfs the
        // other. In the open Café section the prices are already on screen next to it.
        if (!showQr && compact) {
            Image(
                painter = painterResource(R.drawable.ic_coffee),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(if (small) 40.dp else 88.dp),
            )
        }
        val textStyle = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
        Column(
            modifier = if (small) Modifier.widthIn(max = 200.dp) else Modifier.widthIn(min = 160.dp, max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(if (small) 4.dp else 8.dp),
        ) {
            // The phone card keeps only the call to action: with the system font at its largest the
            // title alone doubled the card's height.
            if (!small) {
                Text(
                    text = stringResource(R.string.support_title),
                    style = textStyle,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(
                    when {
                        showQr -> R.string.support_entry
                        !compact -> R.string.support_entry // the prices are already on screen
                        isTv() -> R.string.coffee_bug_action_tv
                        else -> R.string.coffee_bug_action_touch
                    },
                ),
                style = textStyle,
                color = MaterialTheme.colorScheme.primary,
            )
            if (!small) {
                Text(
                    text = stringResource(R.string.coffee_thanks),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
