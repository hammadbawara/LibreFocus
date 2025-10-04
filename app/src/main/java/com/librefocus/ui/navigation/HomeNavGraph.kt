package com.librefocus.ui.home

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.librefocus.ui.onboarding.AppIntroScreen
import com.librefocus.ui.onboarding.OnboardingNavGraph

@Composable
fun HomeNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            OnboardingNavGraph {

            }
        }
    }
}
