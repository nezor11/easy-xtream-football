package com.footballxtream.ui.components

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * Text field usable with a D-pad.
 *
 * On TV the field has two focus levels: the D-pad lands on the *container* (highlighted border,
 * no keyboard). Only an explicit OK moves focus into the text field, which is what makes the IME
 * appear. This is deliberate: if the keyboard popped up as soon as the D-pad reached the field,
 * the user's next OK would be delivered to the IME and would type its highlighted key (a stray
 * "q" on Gboard) instead of "entering" the field, silently corrupting the credentials.
 * Up/down from the editing field leave it and move to the neighbouring container/button.
 *
 * On touch devices the text field is focused/tapped directly, as usual.
 */
@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    focusRequester: FocusRequester? = null,
    helper: String? = null,
) {
    val isTv = LocalContext.current.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    var focused by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(!isTv) }
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val innerRequester = remember { FocusRequester() }
    val borderColor = if (focused) colors.primary else colors.surfaceVariant
    val shape = RoundedCornerShape(10.dp)

    LaunchedEffect(editing) {
        if (isTv && editing) runCatching { innerRequester.requestFocus() }
    }

    val containerModifier = if (isTv) {
        Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onPreviewKeyEvent { event ->
                when (event.key) {
                    // Enter the field on key UP: the IME must not be visible yet when the
                    // release of the same OK press arrives, or it will "click" a key.
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (event.type == KeyEventType.KeyUp && !editing) editing = true
                        !editing
                    }
                    else -> false
                }
            }
            .onFocusChanged { focused = it.hasFocus }
            .focusable()
    } else {
        Modifier
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
        )
        Box(modifier = containerModifier) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.onSurface, fontSize = 18.sp),
                cursorBrush = SolidColor(colors.primary),
                visualTransformation = if (isPassword) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                // URLs and credentials must never be "corrected" by the TV keyboard's suggestions.
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, autoCorrectEnabled = false),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .then(
                        if (isTv) {
                            Modifier
                                .focusRequester(innerRequester)
                                .focusProperties { canFocus = editing }
                        } else if (focusRequester != null) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        },
                    )
                    // Single-line TV fields must release focus on up/down so the D-pad can move
                    // between fields and reach the button (otherwise the field swallows the keys).
                    .onPreviewKeyEvent { event ->
                        when (event.key) {
                            Key.DirectionDown, Key.DirectionUp -> {
                                event.type == KeyEventType.KeyDown && focusManager.moveFocus(
                                    if (event.key == Key.DirectionDown) FocusDirection.Down else FocusDirection.Up,
                                )
                            }
                            // OK while editing with the keyboard closed (after Back) reopens it.
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                if (event.type == KeyEventType.KeyUp) keyboard?.show()
                                true
                            }
                            else -> false
                        }
                    }
                    .onFocusChanged {
                        if (isTv) {
                            if (!it.isFocused) editing = false
                        } else {
                            focused = it.isFocused
                        }
                    }
                    .background(colors.surface, shape)
                    .border(2.dp, borderColor, shape)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
        if (helper != null) {
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            )
        }
    }
}
