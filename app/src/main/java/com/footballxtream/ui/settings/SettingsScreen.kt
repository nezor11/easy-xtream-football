package com.footballxtream.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.footballxtream.BuildConfig
import com.footballxtream.R

/**
 * A small, focused Settings screen: clear the channel/guide cache, plus an "About" block with the
 * app version, license and the open-source attribution required by FFmpeg & co.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val clearFocus = remember { FocusRequester() }
    var showSupport by remember { mutableStateOf(false) }
    val coffeeDismissed by viewModel.coffeeReminderDismissed.collectAsStateWithLifecycle()

    // Back closes the support overlay first; otherwise it leaves the screen.
    BackHandler(enabled = showSupport) { showSupport = false }
    BackHandler(enabled = !showSupport, onBack = onBack)
    LaunchedEffect(Unit) { runCatching { clearFocus.requestFocus() } }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 48.dp, end = 48.dp, top = 40.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
        )
        Text(
            text = "Easy Xtream Football  ·  v${BuildConfig.VERSION_NAME}  ·  GPL-3.0",
            style = MaterialTheme.typography.titleSmall,
            color = colors.primary,
        )

        val cacheCleared = stringResource(R.string.settings_cache_cleared)
        SettingsAction(
            label = stringResource(R.string.settings_clear_cache),
            modifier = Modifier.focusRequester(clearFocus),
            onClick = {
                viewModel.clearCache {
                    Toast.makeText(context, cacheCleared, Toast.LENGTH_SHORT).show()
                }
            },
        )

        SettingsAction(
            label = stringResource(R.string.support_entry),
            onClick = { showSupport = true },
        )

        Text(
            text = stringResource(R.string.settings_open_source),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.settings_licenses_body),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.folder_back_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
        if (showSupport) {
            SupportOverlay(
                reminderDismissed = coffeeDismissed,
                onToggleReminder = { viewModel.setCoffeeReminderDismissed(!coffeeDismissed) },
                onDismiss = { showSupport = false },
            )
        }
    }
}

/** "Buy me a coffee" panel: a QR to the Ko-fi page that the user scans with a phone (TVs have no
 *  browser), plus the handle as text. Dismissed with Back (handled by the caller). */
@Composable
private fun SupportOverlay(
    reminderDismissed: Boolean,
    onToggleReminder: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val toggleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { toggleFocus.requestFocus() } }
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.support_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
            )
            Text(
                text = stringResource(R.string.support_message),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Image(
                painter = painterResource(R.drawable.qr_kofi),
                contentDescription = stringResource(R.string.support_qr_desc),
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(10.dp),
            )
            Text(
                text = stringResource(R.string.support_kofi_handle),
                style = MaterialTheme.typography.titleMedium,
                color = colors.primary,
            )
            SettingsAction(
                label = stringResource(
                    if (reminderDismissed) R.string.coffee_reenable else R.string.coffee_dismiss,
                ),
                modifier = Modifier.focusRequester(toggleFocus),
                onClick = onToggleReminder,
            )
            Text(
                text = stringResource(R.string.folder_back_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** A focusable, full-width-ish action row used for the cache button. */
@Composable
private fun SettingsAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(if (focused) colors.primary else colors.surfaceVariant)
            .border(2.dp, if (focused) colors.onBackground else Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 18.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (focused) colors.onPrimary else colors.onSurface,
        )
    }
}
