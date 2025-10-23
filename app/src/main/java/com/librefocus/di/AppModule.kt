package com.librefocus.di

import com.librefocus.ui.MainViewModel
import com.librefocus.ui.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import android.app.usage.UsageStatsManager
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.librefocus.data.repository.UsageRepository
import com.librefocus.ui.home.HomeViewModel

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
