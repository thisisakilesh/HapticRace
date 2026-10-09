package com.hapticrace.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hapticrace.app.data.TimelineRepository
import com.hapticrace.app.haptic.HapticManager
import com.hapticrace.app.ui.screens.HapticEditorScreen
import com.hapticrace.app.ui.screens.HomeScreen
import com.hapticrace.app.ui.screens.PlaybackScreen
import com.hapticrace.app.ui.screens.VideoImportScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Import : Screen("import")
    object Editor : Screen("editor/{videoId}") {
        fun createRoute(videoId: String) = "editor/$videoId"
    }
    object Playback : Screen("playback/{videoId}") {
        fun createRoute(videoId: String) = "playback/$videoId"
    }
}

@Composable
fun HapticRaceNavHost(
    repository: TimelineRepository,
    hapticManager: HapticManager,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                repository = repository,
                hapticManager = hapticManager,
                onCreateClick = {
                    navController.navigate(Screen.Import.route)
                },
                onEditVideo = { videoId ->
                    navController.navigate(Screen.Editor.createRoute(videoId))
                },
                onPlayVideo = { videoId ->
                    navController.navigate(Screen.Playback.createRoute(videoId))
                }
            )
        }

        composable(Screen.Import.route) {
            VideoImportScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onVideoReady = { videoId ->
                    // Navigate directly to editor for the newly imported track
                    navController.navigate(Screen.Editor.createRoute(videoId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("videoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
            HapticEditorScreen(
                videoId = videoId,
                repository = repository,
                hapticManager = hapticManager,
                onBack = { navController.popBackStack() },
                onLaunchPlayback = { targetVideoId ->
                    navController.navigate(Screen.Playback.createRoute(targetVideoId))
                }
            )
        }

        composable(
            route = Screen.Playback.route,
            arguments = listOf(navArgument("videoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
            PlaybackScreen(
                videoId = videoId,
                repository = repository,
                hapticManager = hapticManager,
                onExit = { navController.popBackStack() }
            )
        }
    }
}
