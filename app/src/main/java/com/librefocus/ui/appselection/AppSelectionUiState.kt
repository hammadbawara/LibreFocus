package com.librefocus.ui.appselection

import com.librefocus.models.InstalledApp

data class AppSelectionUiState(
    val isLoading: Boolean = true,
    val allApps: List<InstalledApp> = emptyList(),
    val filteredApps: List<InstalledApp> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedCategory: String = "ALL",
    val availableCategories: List<String> = emptyList()
)
