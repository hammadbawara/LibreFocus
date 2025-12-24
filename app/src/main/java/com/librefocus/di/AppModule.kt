package com.librefocus.di

import com.librefocus.ui.MainViewModel
import com.librefocus.ui.appselection.AppSelectionViewModel
import com.librefocus.ui.categories.CategoryViewModel
import com.librefocus.ui.home.HomeViewModel
import com.librefocus.ui.onboarding.OnboardingViewModel
import com.librefocus.ui.settings.SettingsViewModel
import com.librefocus.ui.stats.AppDetailViewModel
import com.librefocus.ui.stats.StatsContentViewModel
import com.librefocus.ui.stats.StatsViewModel
import org.koin.core.module.dsl.viewModel
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

val statsModule = module {
    viewModelOf(::StatsViewModel)
    // StatsContentViewModel without packageName (for global stats)
    viewModelOf(::StatsContentViewModel)
    // StatsContentViewModel with packageName parameter (for app detail)
    viewModel { params ->
        StatsContentViewModel(
            usageRepository = get(),
            dateTimeFormatterManager = get(),
            packageName = params.getOrNull()
        )
    }
}

val settingsModule = module {
    viewModelOf(::SettingsViewModel)
}

val appDetailModule = module {
    factory { (packageName: String) ->
        AppDetailViewModel(
            packageName = packageName,
            usageRepository = get(),
            dateTimeFormatterManager = get()
        )
    }
}

val categoryModule = module {
    viewModelOf(::CategoryViewModel)
}

val appSelectionModule = module {
    viewModel { params ->
        AppSelectionViewModel(
            context = get(),
            allowMultipleSelection = params.get(),
            preSelectedPackages = params.get()
        )
    }
}
