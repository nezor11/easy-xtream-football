package com.footballxtream.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.footballxtream.LocaleHelper
import com.footballxtream.R

/** Display name of each shipped language, written in that language (autonym). */
private fun autonym(tag: String): String = when (tag) {
    "en" -> "English"
    "es" -> "Español"
    "ca" -> "Català"
    "eu" -> "Euskara"
    "gl" -> "Galego"
    "pt" -> "Português"
    "fr" -> "Français"
    "it" -> "Italiano"
    else -> tag
}

/** Walks the ContextWrapper chain to find the hosting Activity (for finish/restart from Compose). */
internal fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** Small globe button that opens the language picker; lives in a screen corner. */
@Composable
fun LanguageButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant)
            .border(2.dp, if (focused) colors.primary else Color.Transparent, CircleShape)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_language),
            contentDescription = stringResource(R.string.settings_language),
            colorFilter = ColorFilter.tint(colors.primary),
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Small gear button that opens the Settings screen; mirrors [LanguageButton] in the other corner. */
@Composable
fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant)
            .border(2.dp, if (focused) colors.primary else Color.Transparent, CircleShape)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = stringResource(R.string.settings_title),
            colorFilter = ColorFilter.tint(colors.onSurface),
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * Language picker: "Automatic (device)" across the top, then the shipped languages in a 2-column
 * grid. The active language is filled green with a check; the focused one gets a border. Picking
 * one persists it and relaunches the app in that language.
 */
@Composable
fun LanguageDialog(onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val activity = context.findActivity()
    val current = LocaleHelper.persistedTag(context)
    val selectedFocus = remember { FocusRequester() }
    val languages = remember { LocaleHelper.supportedTags.filter { it.isNotEmpty() } }

    BackHandler(enabled = true) { onDismiss() }
    LaunchedEffect(Unit) { runCatching { selectedFocus.requestFocus() } }

    fun choose(tag: String) {
        activity?.let { LocaleHelper.applyAndRestart(it, tag) }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            // "Automatic (device)" spans the full width — it's the default mode.
            LanguageOption(
                label = stringResource(R.string.language_system),
                selected = current.isEmpty(),
                onClick = { choose("") },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (current.isEmpty()) Modifier.focusRequester(selectedFocus) else Modifier),
            )
            // The shipped languages in a 2-column grid.
            languages.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { tag ->
                        LanguageOption(
                            label = autonym(tag),
                            selected = tag == current,
                            onClick = { choose(tag) },
                            modifier = Modifier
                                .weight(1f)
                                .then(if (tag == current) Modifier.focusRequester(selectedFocus) else Modifier),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val borderColor = when {
        !focused -> Color.Transparent
        selected -> colors.onPrimary
        else -> colors.primary
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.primary else colors.surfaceVariant)
            .border(2.dp, borderColor, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(vertical = 11.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (selected) "✓  $label" else label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) colors.onPrimary else colors.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
