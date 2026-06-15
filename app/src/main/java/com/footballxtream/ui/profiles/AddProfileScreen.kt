package com.footballxtream.ui.profiles

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.footballxtream.R
import com.footballxtream.data.local.ProfileType
import com.footballxtream.ui.components.BrandHeader
import com.footballxtream.ui.components.TvTextField

@Composable
fun AddProfileScreen(
    onSaved: () -> Unit,
    profileId: Long = -1L,
    viewModel: AddProfileViewModel = viewModel(factory = AddProfileViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val firstField = remember { FocusRequester() }
    LaunchedEffect(profileId) { viewModel.load(profileId) }
    LaunchedEffect(state.mode) { runCatching { firstField.requestFocus() } }

    val fieldModifier = Modifier.fillMaxWidth()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        BrandHeader()

        // Card-like container so the form reads as one block instead of text floating on black.
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface)
                .padding(horizontal = 32.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(
                    if (state.isEditing) R.string.add_edit_profile else R.string.add_connect_list,
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeChip(stringResource(R.string.profile_type_xtream), state.mode == ProfileType.XTREAM) {
                    viewModel.onModeChange(ProfileType.XTREAM)
                }
                ModeChip(stringResource(R.string.profile_type_m3u), state.mode == ProfileType.M3U) {
                    viewModel.onModeChange(ProfileType.M3U)
                }
                ModeChip(stringResource(R.string.profile_type_direct), state.mode == ProfileType.DIRECT) {
                    viewModel.onModeChange(ProfileType.DIRECT)
                }
            }

            Text(
                text = stringResource(
                    when {
                        state.isDirect -> R.string.add_desc_direct
                        state.isM3u -> R.string.add_desc_m3u
                        else -> R.string.add_desc_xtream
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )

            if (state.usesUrlField) {
                TvTextField(
                    value = state.m3uUrl,
                    onValueChange = viewModel::onM3uUrlChange,
                    label = stringResource(
                        if (state.isDirect) R.string.field_stream_url else R.string.field_m3u_url,
                    ),
                    modifier = fieldModifier,
                    keyboardType = KeyboardType.Uri,
                    focusRequester = firstField,
                    helper = stringResource(
                        if (state.isDirect) R.string.helper_direct else R.string.helper_m3u,
                    ),
                )
                TvTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = stringResource(R.string.field_profile_name),
                    modifier = fieldModifier,
                )
            } else {
                TvTextField(
                    value = state.server,
                    onValueChange = viewModel::onServerChange,
                    label = stringResource(R.string.field_server_url),
                    modifier = fieldModifier,
                    keyboardType = KeyboardType.Uri,
                    focusRequester = firstField,
                    helper = stringResource(R.string.helper_server),
                )
                TvTextField(
                    value = state.username,
                    onValueChange = viewModel::onUsernameChange,
                    label = stringResource(R.string.field_username),
                    modifier = fieldModifier,
                )
                TvTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = stringResource(R.string.field_password),
                    modifier = fieldModifier,
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                )
                TvTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = stringResource(R.string.field_profile_name),
                    modifier = fieldModifier,
                )
            }

            if (state.error != null) {
                Text(
                    text = state.error.orEmpty(),
                    color = colors.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(
                        when {
                            state.isConnecting -> R.string.btn_connecting
                            state.isEditing -> R.string.btn_save_changes
                            else -> R.string.btn_save_enter
                        },
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    val border = when {
        focused -> colors.primary
        selected -> Color.Transparent
        else -> colors.surfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) colors.primary else colors.surfaceVariant)
            .border(2.dp, border, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onPrimary else colors.onSurface,
        )
    }
}
