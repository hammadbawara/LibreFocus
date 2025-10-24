package com.librefocus.di

import com.librefocus.ui.MainViewModel
import com.librefocus.ui.home.HomeViewModel
import com.librefocus.ui.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingModule = module {
    viewModelOf(::OnboardingViewModel)
}

val mainModule = module {
    viewModelOf (::MainViewModel)
}

val homeModule = module {
    // HomeViewModel now uses UsageTrackingRepository from databaseModule
    viewModelOf(::HomeViewModel)
}
