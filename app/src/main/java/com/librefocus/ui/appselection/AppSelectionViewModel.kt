package com.librefocus.ui.appselection

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.models.AppCategory
import com.librefocus.models.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSelectionViewModel(
    private val context: Context,
    private val allowMultipleSelection: Boolean = true,
    private val preSelectedPackages: Set<String> = emptySet()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AppSelectionUiState(selectedPackages = preSelectedPackages)
    )
    val uiState: StateFlow<AppSelectionUiState> = _uiState.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val apps = withContext(Dispatchers.IO) {
                fetchInstalledApps()
            }

            val categories = listOf("ALL") + apps
                .map { it.category }
                .distinct()
                .sorted()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    allApps = apps,
                    filteredApps = apps,
                    availableCategories = categories
                )
            }
        }
    }

    private fun fetchInstalledApps(): List<InstalledApp> {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        return packages
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .mapNotNull { appInfo ->
                try {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)
                    val category = getCategoryForPackage(appInfo.packageName)

                    InstalledApp(
                        packageName = appInfo.packageName,
                        appName = appName,
                        icon = icon,
                        category = category
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.appName.lowercase() }
    }

    private fun getCategoryForPackage(packageName: String): String {
        return when {
            packageName.contains("facebook") || packageName.contains("instagram") ||
                    packageName.contains("twitter") || packageName.contains("snapchat") ||
                    packageName.contains("tiktok") || packageName.contains("reddit") -> AppCategory.SOCIAL.displayName

            packageName.contains("whatsapp") || packageName.contains("telegram") ||
                    packageName.contains("messenger") || packageName.contains("discord") ||
                    packageName.contains("skype") -> AppCategory.COMMUNICATION.displayName

            packageName.contains("youtube") || packageName.contains("netflix") ||
                    packageName.contains("spotify") || packageName.contains("twitch") ||
                    packageName.contains("prime") -> AppCategory.ENTERTAINMENT.displayName

            packageName.contains("game") || packageName.contains("pubg") ||
                    packageName.contains("clash") || packageName.contains("candy") -> AppCategory.GAMES.displayName

            packageName.contains("office") || packageName.contains("docs") ||
                    packageName.contains("sheets") || packageName.contains("notion") ||
                    packageName.contains("evernote") -> AppCategory.PRODUCTIVITY.displayName

            packageName.contains("amazon") || packageName.contains("ebay") ||
                    packageName.contains("shop") || packageName.contains("store") -> AppCategory.SHOPPING.displayName

            packageName.contains("news") || packageName.contains("medium") ||
                    packageName.contains("flipboard") -> AppCategory.NEWS.displayName

            else -> AppCategory.OTHER.displayName
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { currentState ->
            currentState.copy(searchQuery = query)
        }
        filterApps()
    }

    fun onCategorySelected(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        filterApps()
    }

    fun onAppToggle(packageName: String) {
        _uiState.update { currentState ->
            val currentSelection = currentState.selectedPackages

            val newSelection = if (allowMultipleSelection) {
                if (packageName in currentSelection) {
                    currentSelection - packageName
                } else {
                    currentSelection + packageName
                }
            } else {
                if (packageName in currentSelection) {
                    emptySet()
                } else {
                    setOf(packageName)
                }
            }

            currentState.copy(selectedPackages = newSelection)
        }
    }

    fun clearSearch() {
        onSearchQueryChange("")
    }

    private fun filterApps() {
        val currentState = _uiState.value
        val query = currentState.searchQuery.lowercase().trim()
        val category = currentState.selectedCategory

        val filtered = currentState.allApps.filter { app ->
            val matchesSearch = query.isEmpty() ||
                    app.appName.lowercase().contains(query) ||
                    app.packageName.lowercase().contains(query) ||
                    app.category.lowercase().contains(query)

            val matchesCategory = category == "ALL" || app.category == category

            matchesSearch && matchesCategory
        }

        _uiState.update { it.copy(filteredApps = filtered) }
    }
}
