package com.librefocus.di

import com.librefocus.ui.MainViewModel
import com.librefocus.ui.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingModule = module {
    viewModelOf(::OnboardingViewModel)
}

val mainModule = module {
    viewModelOf (::MainViewModel)
}
