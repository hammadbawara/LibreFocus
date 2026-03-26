package com.librefocus.ui.appselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.AppRepository
import com.librefocus.data.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppSelectionViewModel(
    private val appRepository: AppRepository,
    private val categoryRepository: CategoryRepository,
    private val allowMultipleSelection: Boolean = true,
    private val preSelectedPackages: Set<String> = emptySet()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AppSelectionUiState(selectedPackages = preSelectedPackages)
    )
    val uiState: StateFlow<AppSelectionUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Fetch installed apps from repository
                val apps = appRepository.getInstalledApps()

                // Fetch categories from repository
                val categoryEntities = categoryRepository.getAllCategories().first()
                val categories = listOf("ALL") + categoryEntities.map { it.categoryName }.sorted()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        allApps = apps,
                        filteredApps = apps,
                        availableCategories = categories
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load apps"
                    )
                }
            }
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

    suspend fun isAppInstalled(packageName: String): Boolean {
        return appRepository.isAppInstalled(packageName)
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

