package com.footballxtream

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.footballxtream.ui.RootViewModel
import com.footballxtream.ui.StartState
import com.footballxtream.ui.channels.ChannelsScreen
import com.footballxtream.ui.player.PlayerScreen
import com.footballxtream.ui.profiles.AddProfileScreen
import com.footballxtream.ui.profiles.ProfilesScreen
import com.footballxtream.ui.theme.FootballXtreamTheme

object Routes {
    const val PROFILES = "profiles"
    const val ADD_PROFILE = "add_profile"
    const val EDIT_PROFILE = "edit_profile"
    const val CHANNELS = "channels"
    const val PLAYER = "player"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the device awake while the app is in the foreground (any screen) so it doesn't go
        // idle and put the TV into standby via HDMI-CEC. The flag only applies while visible, so it
        // releases automatically when the app is backgrounded.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            FootballXtreamTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val rootViewModel: RootViewModel = viewModel(factory = RootViewModel.Factory)
    val startState by rootViewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when (startState) {
            StartState.LOADING -> Text(stringResource(R.string.app_loading), color = MaterialTheme.colorScheme.onBackground)
            StartState.HAS_PROFILES -> AppNavigation(start = Routes.PROFILES)
            StartState.NO_PROFILES -> AppNavigation(start = Routes.ADD_PROFILE)
            // Last profile remembered: start on the picker but jump straight into its channels, so
            // pressing Back from channels still returns to the picker to switch profile.
            StartState.GO_CHANNELS -> AppNavigation(start = Routes.PROFILES, autoEnterChannels = true)
        }
    }
}

@Composable
private fun AppNavigation(start: String, autoEnterChannels: Boolean = false) {
    val navController = rememberNavController()

    // Auto-enter the remembered profile's channels, leaving the picker underneath for Back.
    if (autoEnterChannels) {
        LaunchedEffect(Unit) { navController.navigate(Routes.CHANNELS) }
    }

    NavHost(navController = navController, startDestination = start) {

        composable(Routes.PROFILES) {
            ProfilesScreen(
                onProfileSelected = { navController.navigate(Routes.CHANNELS) },
                onAddProfile = { navController.navigate(Routes.ADD_PROFILE) },
                onEditProfile = { id -> navController.navigate("${Routes.EDIT_PROFILE}/$id") },
            )
        }

        // Add a new profile, then jump straight into its channels.
        composable(Routes.ADD_PROFILE) {
            AddProfileScreen(
                onSaved = {
                    navController.navigate(Routes.CHANNELS) {
                        popUpTo(Routes.ADD_PROFILE) { inclusive = true }
                    }
                },
            )
        }

        // Edit an existing profile (prefilled), then return to the picker (showing the new name).
        composable(
            route = "${Routes.EDIT_PROFILE}/{profileId}",
            arguments = listOf(navArgument("profileId") { type = NavType.LongType }),
        ) { entry ->
            val profileId = entry.arguments?.getLong("profileId") ?: -1L
            AddProfileScreen(
                profileId = profileId,
                onSaved = { navController.popBackStack() },
            )
        }

        composable(Routes.CHANNELS) {
            ChannelsScreen(
                onPlay = { navController.navigate(Routes.PLAYER) },
            )
        }

        composable(Routes.PLAYER) {
            PlayerScreen(onBack = { navController.popBackStack() })
        }
    }
}
