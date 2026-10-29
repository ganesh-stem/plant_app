package com.verdant.app.ui.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.verdant.app.ui.analysis.AnalysisScreen
import com.verdant.app.ui.camera.CameraScreen
import com.verdant.app.ui.history.HistoryScreen
import com.verdant.app.ui.home.HomeScreen
import com.verdant.app.ui.onboarding.OnboardingScreen
import com.verdant.app.ui.result.ResultScreen
import com.verdant.app.ui.settings.SettingsScreen

private const val ANIM_MS = 380

@Composable
fun VerdantNavHost(showOnboarding: Boolean) {
    val navController = rememberNavController()
    // Decided once so finishing onboarding doesn't rebuild the graph.
    val startDestination: Route = remember { if (showOnboarding) Route.Onboarding else Route.Home }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val selectedTab = when {
        destination?.hasRoute<Route.Home>() == true -> Tab.HOME
        destination?.hasRoute<Route.History>() == true -> Tab.HISTORY
        else -> null
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) navController.navigate(Route.Analysis(uri.toString()))
    }
    val openGallery = {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { fadeIn(tween(ANIM_MS)) + slideInHorizontally(tween(ANIM_MS)) { it / 8 } },
            exitTransition = { fadeOut(tween(ANIM_MS / 2)) },
            popEnterTransition = { fadeIn(tween(ANIM_MS)) },
            popExitTransition = { fadeOut(tween(ANIM_MS / 2)) + slideOutHorizontally(tween(ANIM_MS)) { it / 8 } },
        ) {
            composable<Route.Onboarding> {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Route.Home) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                )
            }
            composable<Route.Home>(
                enterTransition = { fadeIn(tween(ANIM_MS)) },
                popEnterTransition = { fadeIn(tween(ANIM_MS)) },
            ) {
                HomeScreen(
                    onScan = { navController.navigate(Route.Camera) },
                    onUpload = openGallery,
                    onOpenScan = { navController.navigate(Route.Result(it)) },
                    onSeeAll = { navController.switchTab(Tab.HISTORY) },
                    onSettings = { navController.navigate(Route.Settings) },
                )
            }
            composable<Route.History>(
                enterTransition = { fadeIn(tween(ANIM_MS)) },
                popEnterTransition = { fadeIn(tween(ANIM_MS)) },
            ) {
                HistoryScreen(
                    onOpenScan = { navController.navigate(Route.Result(it)) },
                    onScan = { navController.navigate(Route.Camera) },
                )
            }
            composable<Route.Settings> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onReplayOnboarding = { navController.navigate(Route.Onboarding) },
                )
            }
            composable<Route.Camera>(
                enterTransition = { fadeIn(tween(ANIM_MS)) + scaleIn(tween(ANIM_MS), initialScale = 0.94f) },
                popExitTransition = { fadeOut(tween(ANIM_MS)) + scaleOut(tween(ANIM_MS), targetScale = 0.94f) },
            ) {
                CameraScreen(
                    onClose = { navController.popBackStack() },
                    onImageCaptured = { uri -> navController.navigate(Route.Analysis(uri.toString())) },
                    onPickFromGallery = openGallery,
                )
            }
            composable<Route.Analysis>(
                enterTransition = { fadeIn(tween(ANIM_MS)) },
            ) { entry ->
                val route = entry.toRoute<Route.Analysis>()
                AnalysisScreen(
                    imageUri = route.imageUri,
                    onCancel = { navController.popBackStack() },
                    onNewPhoto = { navController.popBackStack() },
                    onComplete = { scanId ->
                        navController.navigate(Route.Result(scanId)) {
                            // Back from the result returns home, not to the camera or spinner.
                            popUpTo<Route.Home> { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<Route.Result>(
                enterTransition = { fadeIn(tween(ANIM_MS)) + slideInVertically(tween(ANIM_MS)) { it / 10 } },
                popExitTransition = { fadeOut(tween(ANIM_MS / 2)) + slideOutVertically(tween(ANIM_MS)) { it / 10 } },
            ) {
                ResultScreen(
                    onBack = { navController.popBackStack() },
                    onScanAnother = {
                        navController.navigate(Route.Camera) {
                            popUpTo<Route.Home> { inclusive = false }
                        }
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = selectedTab != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            VerdantBottomBar(
                selected = selectedTab ?: Tab.HOME,
                onSelect = { navController.switchTab(it) },
                onScan = { navController.navigate(Route.Camera) },
            )
        }
    }
}

enum class Tab { HOME, HISTORY }

private fun NavHostController.switchTab(tab: Tab) {
    val route: Route = when (tab) {
        Tab.HOME -> Route.Home
        Tab.HISTORY -> Route.History
    }
    navigate(route) {
        // Home is always the root of the back stack (onboarding pops itself).
        popUpTo<Route.Home> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
