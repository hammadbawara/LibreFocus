package com.librefocus.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingNavGraph(
    navController: NavHostController = androidx.navigation.compose.rememberNavController(),
    onOnboardingComplete: () -> Unit
) {
    val viewModel: OnboardingViewModel = koinViewModel()

    NavHost(
        navController = navController,
        startDestination = "intro"
    ) {
        composable("intro") {
            AppIntroScreen(
                onNext = { navController.navigate("permissions") }
            )
        }

        composable("permissions") {
            PermissionScreen(
                viewModel = viewModel,
                onNext = {
                    onOnboardingComplete()
                }
            )
        }
    }
}
