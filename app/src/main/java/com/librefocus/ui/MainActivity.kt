package com.librefocus.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.librefocus.ui.home.HomeNavGraph
import com.librefocus.ui.onboarding.OnboardingNavGraph
import com.librefocus.ui.theme.LibreFocusTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep the splash screen showing until preferences are loaded
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            true
        }

        setContent {
            LibreFocusTheme {
                val onboardingShown by viewModel.onboardingShown.collectAsStateWithLifecycle()

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = if (onboardingShown) "home" else "onboarding"
                ) {
                    composable("onboarding") {
                        OnboardingNavGraph(
                            onOnboardingComplete = {
                                viewModel.setOnboardingShown(true)
                                navController.navigate("home") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("home") {
                        HomeNavGraph()
                    }
                }
            }
        }
    }
}