package com.librefocus.di

import com.librefocus.ui.onboarding.OnboardingViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingModule = module {
    viewModelOf(::OnboardingViewModel)
}
