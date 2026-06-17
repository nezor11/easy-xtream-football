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
import androidx.compose.ui.unit.dp
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

    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { runCatching { clearFocus.requestFocus() } }

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
