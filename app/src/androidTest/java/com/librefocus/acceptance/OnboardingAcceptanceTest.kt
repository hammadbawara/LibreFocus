package com.librefocus.acceptance

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.librefocus.ui.onboarding.AppIntroScreen
import com.librefocus.ui.onboarding.OnboardingViewModel
import com.librefocus.ui.onboarding.PermissionScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class OnboardingAcceptanceTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun introScreen_displaysCoreBrandingAndPrimaryAction() {
        composeRule.setContent {
            AppIntroScreen(onNext = {})
        }

        composeRule.onNodeWithText("LibreFocus").assertIsDisplayed()
        composeRule.onNodeWithText("Reclaim your focus").assertIsDisplayed()
        composeRule.onNodeWithText("Get Started").assertIsDisplayed()
    }

    @Test
    fun introPrimaryAction_navigatesToPermissionStep() {
        val onboardingViewModel = OnboardingViewModel()

        composeRule.setContent {
            OnboardingAcceptanceHarness(onboardingViewModel = onboardingViewModel)
        }

        composeRule.onNodeWithText("Get Started").performClick()

        composeRule.onNodeWithText("App Permissions").assertIsDisplayed()
        composeRule.onNodeWithText("Next").assertIsNotEnabled()
    }

    @Test
    fun permissionStep_requiresMandatoryPermissionBeforeCompletion() {
        val onboardingViewModel = OnboardingViewModel()
        var onboardingCompleted = false

        composeRule.setContent {
            PermissionScreen(
                viewModel = onboardingViewModel,
                onNext = { onboardingCompleted = true }
            )
        }

        composeRule.onNodeWithText("Next").assertIsNotEnabled()

        composeRule.runOnIdle {
            onboardingViewModel.updatePermissionStatus(
                permission = "android.permission.PACKAGE_USAGE_STATS",
                granted = true
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Next").assertIsEnabled()
        composeRule.onNodeWithText("Next").performClick()

        composeRule.runOnIdle {
            assertTrue("Onboarding should complete when mandatory permissions are granted", onboardingCompleted)
        }
    }
}

@Composable
private fun OnboardingAcceptanceHarness(onboardingViewModel: OnboardingViewModel) {
    var step by remember { mutableStateOf(0) }

    when (step) {
        0 -> AppIntroScreen(onNext = { step = 1 })
        else -> PermissionScreen(
            viewModel = onboardingViewModel,
            onNext = {}
        )
    }
}
