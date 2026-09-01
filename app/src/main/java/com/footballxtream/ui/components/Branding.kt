package com.footballxtream.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.footballxtream.R

/**
 * App wordmark used on the start screens: "Easy"/"Football" in the foreground colour and "Xtream"
 * in the green accent, with the localized tagline below. [compact] shrinks it and hides the tagline
 * for the profile picker header. The brand name itself is not translated.
 */
@Composable
fun BrandHeader(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val wordmarkStyle =
        if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall
    // One Text (not three in a Row) so it wraps at word boundaries and stays centred on a narrow phone
    // ("Easy Xtream" / "Football") instead of breaking mid-word.
    val wordmark = buildAnnotatedString {
        withStyle(SpanStyle(color = colors.onBackground)) { append("Easy ") }
        withStyle(SpanStyle(color = colors.primary)) { append("Xtream") }
        withStyle(SpanStyle(color = colors.onBackground)) { append(" Football") }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = wordmark,
            style = wordmarkStyle,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (!compact) {
            Text(
                text = stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
