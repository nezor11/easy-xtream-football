package com.footballxtream.ui.profiles

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import android.widget.Toast
import com.footballxtream.R
import com.footballxtream.data.local.ProfileEntity
import com.footballxtream.ui.components.BrandHeader
import com.footballxtream.ui.components.LanguageButton
import com.footballxtream.ui.components.LanguageDialog
import com.footballxtream.ui.components.findActivity

@Composable
fun ProfilesScreen(
    onProfileSelected: () -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: (Long) -> Unit,
    viewModel: ProfilesViewModel = viewModel(factory = ProfilesViewModel.Factory),
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    var menuProfile by remember { mutableStateOf<ProfileEntity?>(null) }
    var showLanguage by remember { mutableStateOf(false) }

    // Profiles is the root screen: Back here would exit the app. Require a quick double-press (with
    // an on-screen hint) so a single stray Back/Exit doesn't drop the user out by accident.
    val context = LocalContext.current
    var lastBackAt by remember { mutableStateOf(0L) }
    BackHandler(enabled = menuProfile == null && !showLanguage) {
        val now = System.currentTimeMillis()
        if (now - lastBackAt < 2500L) {
            context.findActivity()?.finishAffinity()
        } else {
            lastBackAt = now
            Toast.makeText(context, R.string.press_back_to_exit, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            // The brand is the heading now (replaces the Netflix-style "¿Quién está viendo?").
            BrandHeader()

            val menuOpen = menuProfile != null || showLanguage
            // Auto-focus the first profile on entry, so the remote works immediately without a
            // first "blind" press to grab focus.
            val firstFocus = remember { FocusRequester() }
            // Only focus the first profile on entry — never the Add button, so it doesn't flash
            // green while the profile list is still loading (it starts out momentarily empty).
            LaunchedEffect(profiles.isNotEmpty(), menuOpen) {
                if (profiles.isNotEmpty() && !menuOpen) runCatching { firstFocus.requestFocus() }
            }
            // A plain centered Row (not a LazyRow) keeps the cards centered and, since it does not
            // clip, the focus zoom never gets cut off. Fine for the handful of profiles a user has.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            ) {
                profiles.forEachIndexed { index, profile ->
                    ProfileCard(
                        profile = profile,
                        focusable = !menuOpen,
                        onClick = { viewModel.select(profile, onProfileSelected) },
                        onLongClick = { menuProfile = profile },
                        modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                    )
                }
            }
            // Add a new profile: a wide button below the cards (was a "+" tile at the end of the row).
            AddProfileButton(
                focusable = !menuOpen,
                onClick = onAddProfile,
            )
            if (profiles.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.profiles_longpress_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        menuProfile?.let { profile ->
            ProfileActionMenu(
                profile = profile,
                onEdit = {
                    menuProfile = null
                    onEditProfile(profile.id)
                },
                onDelete = {
                    menuProfile = null
                    viewModel.delete(profile)
                },
                onDismiss = { menuProfile = null },
            )
        }

        // Language picker: a discreet globe in the corner, hidden while another overlay is open.
        if (menuProfile == null && !showLanguage) {
            LanguageButton(
                onClick = { showLanguage = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(24.dp),
            )
        }
        if (showLanguage) {
            LanguageDialog(onDismiss = { showLanguage = false })
        }
    }
}

/** Long-press menu: edit or delete a profile (replaces the old instant, unconfirmed delete). */
@Composable
private fun ProfileActionMenu(
    profile: ProfileEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val firstButton = remember { FocusRequester() }
    // The long-press that opens this menu ends with a key-up (the OK release) that Compose would
    // otherwise deliver to the freshly focused "Editar" button, triggering it instantly. That stray
    // release is a KeyUp with no matching KeyDown inside the menu, so swallow select-key KeyUps
    // until we've seen a real KeyDown here.
    var sawKeyDown by remember { mutableStateOf(false) }

    BackHandler(enabled = true) { onDismiss() }
    LaunchedEffect(Unit) { runCatching { firstButton.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .onPreviewKeyEvent { event ->
                val isSelect = event.key == Key.DirectionCenter ||
                    event.key == Key.Enter ||
                    event.key == Key.NumPadEnter
                when {
                    event.type == KeyEventType.KeyDown -> {
                        sawKeyDown = true
                        false
                    }
                    event.type == KeyEventType.KeyUp && isSelect && !sawKeyDown -> true
                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = profile.name.ifBlank { profile.username },
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().focusRequester(firstButton),
            ) {
                Text(text = stringResource(R.string.action_edit), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            Button(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.action_delete), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.action_cancel), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

// Shared card geometry so profile cards and the "add" card are exactly the same size: a fixed avatar
// area on top and a fixed label area below, regardless of how many lines the label has.
private val CardWidth = 150.dp
private val AvatarAreaHeight = 92.dp
private val LabelAreaHeight = 64.dp

/** Tinted circle with a single letter (or "+"), shared by both card types for an identical top area. */
@Composable
private fun CardAvatar(letter: String) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxWidth().height(AvatarAreaHeight),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ProfileEntity,
    focusable: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.width(CardWidth).focusProperties { canFocus = focusable },
        scale = CardDefaults.scale(focusedScale = 1.06f),
    ) {
        CardAvatar(letter = profile.name.take(1).uppercase().ifBlank { "?" })
        Box(
            modifier = Modifier.fillMaxWidth().height(LabelAreaHeight),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = profile.name.ifBlank { profile.username },
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                )
                TypeBadge(
                    label = stringResource(
                        when {
                            profile.isDirect -> R.string.profile_type_direct
                            profile.isM3u -> R.string.profile_type_m3u
                            else -> R.string.profile_type_xtream
                        },
                    ),
                )
            }
        }
    }
}

/** Small pill that tells the profile source apart (Xtream / M3U / direct) at a glance. */
@Composable
private fun TypeBadge(label: String) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = colors.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Wide "Add profile" button below the cards. Muted by default so it doesn't compete with the
 *  profiles; only turns green and prominent when focused (i.e. when you actually go to add one). */
@Composable
private fun AddProfileButton(
    focusable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(if (focused) colors.primary else Color.Transparent)
            .border(1.dp, if (focused) colors.primary else colors.surfaceVariant, shape)
            .focusProperties { canFocus = focusable }
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+  " + stringResource(R.string.add_profile),
            style = MaterialTheme.typography.bodySmall,
            color = if (focused) colors.onPrimary else colors.onSurfaceVariant,
        )
    }
}
