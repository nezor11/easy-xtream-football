package com.footballxtream.ui.components

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** True on Android TV / Google TV / Fire TV (leanback), false on phones and tablets. */
@Composable
fun isTv(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
}

/**
 * Extra inset on TV so content stays inside the overscan-safe area Google recommends for TV apps
 * (48 dp on the sides, 27 dp top/bottom). The screens already keep about 20 dp from the edges, so
 * this adds the difference. No-op on phones, where the system bars padding already applies.
 */
@Composable
fun Modifier.tvSafeArea(horizontal: Dp = 28.dp, vertical: Dp = 0.dp): Modifier =
    if (isTv()) this.padding(horizontal = horizontal, vertical = vertical) else this
