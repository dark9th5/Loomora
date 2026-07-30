package com.loomora.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.loomora.feature.editor.EditorRoute
import com.loomora.feature.home.HomeRoute
import com.loomora.feature.library.LibraryRoute
import com.loomora.feature.onboarding.OnboardingRoute
import com.loomora.feature.onboarding.OnboardingMode
import com.loomora.feature.recorder.RecorderRoute
import com.loomora.feature.recordingdetail.RecordingDetailRoute
import com.loomora.feature.settings.SettingsRoute
import com.loomora.feature.subscription.SubscriptionRoute

@Composable
fun LoomoraNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = listOf(
        Screen.Home.route,
        Screen.Library.route,
        Screen.Settings.route
    )

    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                LoomoraBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToRoute = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingRoute(
                    onCompleteOnboarding = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Tutorial.route) {
                OnboardingRoute(
                    mode = OnboardingMode.TUTORIAL,
                    onCompleteOnboarding = { navController.popBackStack() }
                )
            }
            composable(Screen.Home.route) {
                HomeRoute(
                    onNavigateToRecorder = { mode -> navController.navigate(Screen.Recorder.createRoute(mode?.name)) },
                    onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToDetail = { recordingId ->
                        navController.navigate(Screen.RecordingDetail.createRoute(recordingId))
                    }
                )
            }
            composable(Screen.Library.route) {
                LibraryRoute(
                    onNavigateToDetail = { recordingId ->
                        navController.navigate(Screen.RecordingDetail.createRoute(recordingId))
                    }
                )
            }
            composable(
                route = Screen.RecordingDetail.route,
                arguments = listOf(navArgument("recordingId") { type = NavType.StringType })
            ) {
                RecordingDetailRoute(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.Editor.route,
                arguments = listOf(navArgument("recordingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val recordingId = backStackEntry.arguments?.getString("recordingId") ?: ""
                EditorRoute(
                    recordingId = recordingId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsRoute(
                    onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) },
                    onNavigateToTutorial = { navController.navigate(Screen.Tutorial.route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Subscription.route) {
                SubscriptionRoute(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.Recorder.route,
                arguments = listOf(navArgument("mode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                RecorderRoute(
                    initialMode = backStackEntry.arguments?.getString("mode"),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
