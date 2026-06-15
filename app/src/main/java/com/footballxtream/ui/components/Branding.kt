package com.footballxtream.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * App wordmark used on the start screens. "Football" in the foreground colour and "Xtream" in the
 * green accent, with an optional tagline. [compact] shrinks it for the profile picker header.
 */
@Composable
fun BrandHeader(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    tagline: String? = "Solo deporte en directo",
) {
    val colors = MaterialTheme.colorScheme
    val wordmarkStyle =
        if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row {
            Text(text = "Football ", style = wordmarkStyle, fontWeight = FontWeight.Bold, color = colors.onBackground)
            Text(text = "Xtream", style = wordmarkStyle, fontWeight = FontWeight.Bold, color = colors.primary)
        }
        if (!compact && tagline != null) {
            Text(text = tagline, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}
