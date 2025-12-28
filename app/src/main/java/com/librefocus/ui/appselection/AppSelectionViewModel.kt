package com.librefocus.ui.appselection

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.local.AppInfoProvider
import com.librefocus.data.repository.CategoryRepository
import com.librefocus.models.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSelectionViewModel(
    private val context: Context,
    private val appInfoProvider: AppInfoProvider,
    private val categoryRepository: CategoryRepository,
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

            val categories = withContext(Dispatchers.IO) {
                fetchCategories()
            }

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

    private suspend fun fetchInstalledApps(): List<InstalledApp> {
        val packageManager = context.packageManager
        val installedApps = appInfoProvider.getInstalledApps()

        return installedApps.mapNotNull { appInfo ->
            try {
                val icon = packageManager.getApplicationIcon(appInfo.packageName)

                InstalledApp(
                    packageName = appInfo.packageName,
                    appName = appInfo.appName,
                    icon = icon,
                    category = appInfo.category
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun fetchCategories(): List<String> {
        return try {
            val categoryEntities = categoryRepository.getAllCategories().first()
            val categoryNames = categoryEntities.map { it.categoryName }.sorted()
            listOf("ALL") + categoryNames
        } catch (e: Exception) {
            listOf("ALL")
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
