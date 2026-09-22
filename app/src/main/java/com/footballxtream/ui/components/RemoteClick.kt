package com.footballxtream.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/** Whether the OK/Enter press in flight already fired the long click (so its release doesn't click). */
private class RemotePress(var longPressFired: Boolean = false)

/**
 * [combinedClickable] that also long-presses from a remote or keyboard. Foundation's only detects a
 * long press from touch: a held OK reaches it as key repeats and it clicks on release, which silently
 * lost every long-press action on TV (reorder favorites, channel, folder and profile menus) when the
 * tv.material3 Card was dropped for touch support. So the select keys are handled here: the first
 * repeat is the long press, and a release that didn't long-press is the click. Touch still goes
 * through [combinedClickable] untouched.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.remoteCombinedClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
): Modifier = composed {
    val press = remember { RemotePress() }
    this
        .onPreviewKeyEvent { e ->
            val select = e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter
            if (!select) return@onPreviewKeyEvent false
            when (e.type) {
                KeyEventType.KeyDown -> {
                    if (e.nativeKeyEvent.repeatCount == 0) {
                        press.longPressFired = false
                    } else if (!press.longPressFired && onLongClick != null) {
                        press.longPressFired = true
                        onLongClick()
                    }
                    true
                }
                KeyEventType.KeyUp -> {
                    if (!press.longPressFired) onClick()
                    press.longPressFired = false
                    true
                }
                else -> false
            }
        }
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
}
