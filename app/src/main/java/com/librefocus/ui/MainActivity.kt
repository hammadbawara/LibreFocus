package com.librefocus.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.librefocus.ui.navigation.NavGraph
import com.librefocus.ui.onboarding.OnboardingNavGraph
import com.librefocus.ui.theme.LibreFocusTheme
import com.librefocus.utils.UsageSyncScheduler
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity() : ComponentActivity() {
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val splashScreen = installSplashScreen()
        // Keep the splash screen showing until everything loaded
        splashScreen.setKeepOnScreenCondition {
            false
        }

        // Schedule periodic usage sync when app starts
        UsageSyncScheduler.schedulePeriodicSync(this)

        setContent {
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val darkTheme = when (appTheme) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            LibreFocusTheme(darkTheme = darkTheme) {
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
                        NavGraph()
                    }
                }
            }
        }
    }
}