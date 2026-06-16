package com.footballxtream.ui.channels

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed as lazyItemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.footballxtream.R
import com.footballxtream.model.ChannelFolder
import com.footballxtream.model.ChannelGroup
import com.footballxtream.model.Quality
import com.footballxtream.model.QualityMode
import com.footballxtream.ui.components.TvTextField

@Composable
fun ChannelsScreen(
    onPlay: () -> Unit,
    viewModel: ChannelsViewModel = viewModel(factory = ChannelsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteNames by viewModel.favoriteNames.collectAsStateWithLifecycle()
    val favoriteChannelKeys by viewModel.favoriteChannelKeys.collectAsStateWithLifecycle()
    val openedFolder by viewModel.openedFolder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        val folder = openedFolder
        when {
            folder != null -> {
                BackHandler(onBack = viewModel::closeFolder)
                FolderDetail(
                    folder = folder,
                    favoriteChannelKeys = favoriteChannelKeys,
                    onChannelSelected = { index -> viewModel.play(folder, index, onPlay) },
                    onChannelLongClick = viewModel::toggleFavoriteChannel,
                )
            }

            state is ChannelsUiState.Loading -> LoadingSkeleton()

            state is ChannelsUiState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource((state as ChannelsUiState.Error).messageRes),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Button(onClick = viewModel::reload) { Text(stringResource(R.string.action_retry)) }
            }

            state is ChannelsUiState.Content -> FolderGrid(
                content = state as ChannelsUiState.Content,
                title = viewModel.activeProfileName
                    ?.let { stringResource(R.string.live_sports_of, it) }
                    ?: stringResource(R.string.live_sports),
                favoriteNames = favoriteNames,
                favoriteChannelKeys = favoriteChannelKeys,
                query = searchQuery,
                onQueryChange = viewModel::setQuery,
                onQualitySelected = viewModel::selectQuality,
                onReload = viewModel::reload,
                onResume = { key -> viewModel.resume(key, onPlay) },
                onFolderClick = { f ->
                    if (f.isSingle) viewModel.play(f, 0, onPlay) else viewModel.openFolder(f)
                },
                onFolderLongClick = viewModel::toggleFavorite,
                onChannelLongClick = viewModel::toggleFavoriteChannel,
            )
        }
    }
}

@Composable
private fun FolderGrid(
    content: ChannelsUiState.Content,
    title: String,
    favoriteNames: Set<String>,
    favoriteChannelKeys: Set<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    onQualitySelected: (QualityMode) -> Unit,
    onReload: () -> Unit,
    onResume: (String) -> Unit,
    onFolderClick: (ChannelFolder) -> Unit,
    onFolderLongClick: (ChannelFolder) -> Unit,
    onChannelLongClick: (ChannelGroup) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(top = 28.dp)) {
        // Filters (quality chips, search, reload) are tucked away and only shown on demand, to keep
        // the header clean and give the channels more room.
        var filtersOpen by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Chip(
                label = stringResource(R.string.action_filters),
                selected = filtersOpen,
                onClick = { filtersOpen = !filtersOpen },
            )
        }
        // Count up from 0 to the real total, so the user gets a feel for how big the list is.
        var countStarted by remember { mutableStateOf(false) }
        LaunchedEffect(content.totalChannels) { countStarted = true }
        val animatedCount by animateIntAsState(
            targetValue = if (countStarted) content.totalChannels else 0,
            animationSpec = tween(durationMillis = 700),
            label = "channelCount",
        )
        Text(
            text = pluralStringResource(R.plurals.channels_count, animatedCount, animatedCount),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 48.dp, bottom = 14.dp),
        )
        val searchFocus = remember { FocusRequester() }
        var searchOpen by remember { mutableStateOf(query.isNotBlank()) }

        if (filtersOpen) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 48.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QualityMode.entries.forEach { mode ->
                    Chip(
                        label = mode.label,
                        selected = mode == content.qualityMode,
                        onClick = { onQualitySelected(mode) },
                    )
                }
                Chip(
                    label = stringResource(R.string.action_search),
                    selected = searchOpen || query.isNotBlank(),
                    onClick = {
                        searchOpen = !searchOpen
                        if (!searchOpen) onQueryChange("")
                    },
                )
                Chip(label = stringResource(R.string.action_reload), selected = false, onClick = onReload)
            }

            if (searchOpen) {
                LaunchedEffect(Unit) { runCatching { searchFocus.requestFocus() } }
                TvTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = stringResource(R.string.search_field_label),
                    modifier = Modifier.padding(start = 48.dp, bottom = 18.dp).width(520.dp),
                    focusRequester = searchFocus,
                )
            }
        }

        if (content.rows.isEmpty()) {
            Text(
                text = if (query.isBlank()) {
                    stringResource(R.string.no_sports_channels)
                } else {
                    stringResource(R.string.no_results, query)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 48.dp, top = 24.dp),
            )
            return@Column
        }

        // First content row that will render, to land the focus on it: favorites > recents > rows.
        val firstSection = when {
            content.favoriteChannels.isNotEmpty() -> 0
            content.recent.isNotEmpty() -> 1
            else -> 2
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (content.favoriteChannels.isNotEmpty()) {
                item {
                    ChannelRowSection(title = stringResource(R.string.section_favorite_channels)) {
                        WrappingRow(content.favoriteChannels, autoFocus = firstSection == 0) { group, cardModifier ->
                            ChannelCard(
                                group = group,
                                onClick = { onResume(group.key) },
                                modifier = cardModifier,
                                isFavorite = true,
                                onLongClick = { onChannelLongClick(group) },
                            )
                        }
                    }
                }
            }
            if (content.recent.isNotEmpty()) {
                item {
                    ChannelRowSection(title = stringResource(R.string.section_recent)) {
                        WrappingRow(content.recent, autoFocus = firstSection == 1) { group, cardModifier ->
                            ChannelCard(
                                group = group,
                                onClick = { onResume(group.key) },
                                modifier = cardModifier,
                                isFavorite = favoriteChannelKeys.contains(group.key),
                                onLongClick = { onChannelLongClick(group) },
                            )
                        }
                    }
                }
            }
            lazyItemsIndexed(content.rows) { index, row ->
                ChannelRowSection(title = stringResource(row.titleRes)) {
                    WrappingRow(row.folders, autoFocus = firstSection == 2 && index == 0) { folder, cardModifier ->
                        FolderCard(
                            folder = folder,
                            isFavorite = favoriteNames.contains(folder.name),
                            onClick = { onFolderClick(folder) },
                            onLongClick = { onFolderLongClick(folder) },
                            modifier = cardModifier,
                        )
                    }
                }
            }
        }
    }
}

/** Simple, clear loading state: a spinner and a short message, centered. */
@Composable
private fun LoadingSkeleton() {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "loading")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "spin",
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Canvas(modifier = Modifier.size(46.dp)) {
            drawArc(
                color = colors.primary,
                startAngle = angle,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Text(
            text = stringResource(R.string.loading_channels),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

/** A titled section: the row label plus its horizontal strip of cards. */
@Composable
private fun ChannelRowSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 48.dp),
        )
        content()
    }
}

/**
 * A horizontal row of cards whose focus wraps around: pressing left on the first card jumps to the
 * last, and right on the last jumps to the first — so it reads the same forwards and backwards. The
 * vertical content padding leaves room for the focus zoom so the grown card is not clipped.
 */
@Composable
private fun <T> WrappingRow(
    items: List<T>,
    autoFocus: Boolean = false,
    itemContent: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val firstFocus = remember { FocusRequester() }
    val lastFocus = remember { FocusRequester() }
    var focusedIndex by remember { mutableStateOf(-1) }
    val lastIndex = items.lastIndex

    // On the first content row, land the focus on its first card when the grid opens.
    if (autoFocus) {
        LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown || items.size < 2) return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionRight -> if (focusedIndex == lastIndex) {
                    scope.launch {
                        listState.scrollToItem(0)
                        runCatching { firstFocus.requestFocus() }
                    }
                    true
                } else {
                    false
                }

                Key.DirectionLeft -> if (focusedIndex == 0) {
                    scope.launch {
                        listState.scrollToItem(lastIndex)
                        runCatching { lastFocus.requestFocus() }
                    }
                    true
                } else {
                    false
                }

                else -> false
            }
        },
    ) {
        lazyItemsIndexed(items) { index, item ->
            val cardModifier = when (index) {
                0 -> Modifier.focusRequester(firstFocus)
                lastIndex -> Modifier.focusRequester(lastFocus)
                else -> Modifier
            }.onFocusChanged { if (it.isFocused) focusedIndex = index }
            itemContent(item, cardModifier)
        }
    }
}

@Composable
private fun FolderDetail(
    folder: ChannelFolder,
    favoriteChannelKeys: Set<String>,
    onChannelSelected: (Int) -> Unit,
    onChannelLongClick: (ChannelGroup) -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(folder.name) {
        runCatching { firstFocus.requestFocus() }
    }
    Column(modifier = Modifier.fillMaxSize().padding(top = 28.dp)) {
        Text(
            text = folder.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
        )
        Text(
            text = stringResource(R.string.folder_back_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 48.dp, bottom = 16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(156.dp),
            // Vertical padding so the focus zoom on the top/bottom rows is not clipped.
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(folder.channels) { index, channel ->
                ChannelCard(
                    group = channel,
                    onClick = { onChannelSelected(index) },
                    modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                    isFavorite = favoriteChannelKeys.contains(channel.key),
                    onLongClick = { onChannelLongClick(channel) },
                )
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) colors.primary else colors.surfaceVariant)
            .border(2.dp, if (focused) colors.onBackground else Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onPrimary else colors.onSurface,
        )
    }
}

@Composable
private fun FolderCard(
    folder: ChannelFolder,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle = if (folder.isSingle) {
        qualityLabels(folder.single)
    } else {
        pluralStringResource(R.plurals.channels_count, folder.channels.size, folder.channels.size)
    }
    ImageCard(
        title = folder.name,
        subtitle = subtitle,
        iconUrl = folder.iconUrl,
        showFavorite = isFavorite,
        showFolderHint = !folder.isSingle,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        countryCode = folder.country,
        geoBlocked = folder.geoBlocked,
    )
}

@Composable
private fun ChannelCard(
    group: ChannelGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    ImageCard(
        title = group.displayName,
        subtitle = qualityLabels(group),
        iconUrl = group.iconUrl,
        showFavorite = isFavorite,
        showFolderHint = false,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        countryCode = group.country,
        geoBlocked = group.geoBlocked,
    )
}

@Composable
private fun ImageCard(
    title: String,
    subtitle: String,
    iconUrl: String?,
    showFavorite: Boolean,
    showFolderHint: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    countryCode: String? = null,
    geoBlocked: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.width(148.dp),
        scale = CardDefaults.scale(focusedScale = 1.06f),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(3.dp, colors.primary),
                shape = RoundedCornerShape(12.dp),
            ),
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(84.dp).background(colors.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            // Fall back to the initials when there's no logo, or when the logo URL fails to load
            // (many playlist logos 404). Keyed on the URL so it resets when the card is recycled.
            var imageFailed by remember(iconUrl) { mutableStateOf(false) }
            if (iconUrl != null && !imageFailed) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    onError = { imageFailed = true },
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                )
            } else {
                Text(
                    text = title.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (showFavorite) {
                Text(
                    text = "★",
                    color = colors.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                )
            }
            if (showFolderHint) {
                Text(
                    text = "▸",
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                )
            }
            if (countryCode != null) {
                // Origin country of the channel (read from the M3U tvg-id). A hint of where it's
                // meant to air — not a guarantee it plays from there.
                Text(
                    text = countryCode.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurface,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
            if (geoBlocked) {
                // The playlist already flags this channel as region-locked: warn before entering.
                Text(
                    text = "GEO",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.primary.copy(alpha = 0.9f))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun qualityLabels(group: ChannelGroup): String =
    Quality.tiers.filter { group.availableQualities.contains(it) }.joinToString(" · ") { it.label }
