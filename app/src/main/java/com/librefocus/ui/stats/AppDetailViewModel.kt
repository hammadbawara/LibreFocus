package com.librefocus.ui.stats

import com.librefocus.data.repository.UsageTrackingRepository
import com.librefocus.utils.DateTimeFormatterManager

/**
 * ViewModel for App Detail Screen.
 * Extends StatsContentViewModel to reuse all stats functionality,
 * but filters data to show only a single app's usage.
 *
 * @param packageName The package name of the app to display details for
 * @param usageRepository Repository for usage tracking data
 * @param dateTimeFormatterManager Manager for date/time formatting
 */
class AppDetailViewModel(
    packageName: String,
    usageRepository: UsageTrackingRepository,
    dateTimeFormatterManager: DateTimeFormatterManager
) : StatsContentViewModel(
    usageRepository = usageRepository,
    dateTimeFormatterManager = dateTimeFormatterManager,
    packageName = packageName
)
