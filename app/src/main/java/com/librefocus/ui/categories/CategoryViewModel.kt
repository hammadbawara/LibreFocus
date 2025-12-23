package com.librefocus.ui.categories

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.local.database.entity.AppCategoryEntity
import com.librefocus.data.repository.CategoryAlreadyExistsException
import com.librefocus.data.repository.CategoryRepository
import com.librefocus.utils.CategoryIconMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Category Management screen.
 * Manages category CRUD operations, app assignments, and UI state.
 */
class CategoryViewModel(
    private val categoryRepository: CategoryRepository,
    private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()
    
    private var uncategorizedCategoryId: Int? = null
    
    init {
        loadCategories()
    }
    
    /**
     * Load all categories and their app counts
     */
    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                categoryRepository.getAllCategories()
                    .catch { e ->
                        _uiState.update { 
                            it.copy(isLoading = false, error = "Failed to load categories: ${e.message}")
                        }
                    }
                    .collect { categories ->
                        // Find uncategorized category ID
                        uncategorizedCategoryId = categories.find { 
                            it.categoryName.equals("Undefined", ignoreCase = true) 
                        }?.id
                        
                        // Map to CategoryItem with app counts
                        val categoryItems = categories.map { category ->
                            val appCount = categoryRepository.getAppCountInCategory(category.id)
                            CategoryItem(
                                id = category.id,
                                name = category.categoryName,
                                icon = CategoryIconMapper.getIconForCategory(category.categoryName, category.isCustom),
                                isCustom = category.isCustom,
                                appCount = appCount
                            )
                        }
                        
                        _uiState.update { 
                            it.copy(
                                categories = categoryItems,
                                isLoading = false,
                                error = null
                            )
                        }
                        
                        // Reload apps if a category is selected
                        _uiState.value.selectedCategoryId?.let { selectedId ->
                            loadAppsForCategory(selectedId)
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isLoading = false, error = "Failed to load categories: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Select a category to view its apps
     */
    fun selectCategory(categoryId: Int) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        loadAppsForCategory(categoryId)
    }
    
    /**
     * Load apps for the selected category
     */
    private fun loadAppsForCategory(categoryId: Int) {
        viewModelScope.launch {
            try {
                categoryRepository.getAppsByCategory(categoryId)
                    .catch { e ->
                        _uiState.update { 
                            it.copy(error = "Failed to load apps: ${e.message}")
                        }
                    }
                    .collect { apps ->
                        val packageManager = context.packageManager
                        val appItems = apps.map { app ->
                            // Try to get app icon from package manager
                            val icon = try {
                                packageManager.getApplicationIcon(app.packageName)
                            } catch (e: PackageManager.NameNotFoundException) {
                                null
                            }
                            
                            AppItem(
                                id = app.id,
                                packageName = app.packageName,
                                appName = app.appName,
                                icon = icon
                            )
                        }
                        
                        _uiState.update { 
                            it.copy(appsInSelectedCategory = appItems)
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to load apps: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Show add category dialog
     */
    fun showAddCategoryDialog() {
        _uiState.update { 
            it.copy(dialogState = DialogState.Add())
        }
    }
    
    /**
     * Show edit category dialog
     */
    fun showEditCategoryDialog(categoryId: Int) {
        viewModelScope.launch {
            val category = categoryRepository.getCategoryById(categoryId)
            if (category != null) {
                // Determine the icon name
                val iconName = if (category.isCustom) {
                    // For custom categories, try to find matching icon or default
                    CategoryIconMapper.getAvailableIcons()
                        .find { it.icon == CategoryIconMapper.getIconForCategory(category.categoryName, true) }
                        ?.name ?: "Category"
                } else {
                    // For system categories, use category name as icon identifier
                    category.categoryName
                }
                
                _uiState.update { 
                    it.copy(
                        dialogState = DialogState.Edit(
                            categoryId = category.id,
                            categoryName = category.categoryName,
                            selectedIconName = iconName,
                            isCustom = category.isCustom
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Hide dialogs
     */
    fun hideDialog() {
        _uiState.update { 
            it.copy(dialogState = DialogState.Hidden, confirmationDialog = null)
        }
    }
    
    /**
     * Update category name in dialog
     */
    fun updateDialogCategoryName(name: String) {
        _uiState.update { state ->
            state.copy(
                dialogState = when (val dialog = state.dialogState) {
                    is DialogState.Add -> dialog.copy(categoryName = name, error = null)
                    is DialogState.Edit -> dialog.copy(categoryName = name, error = null)
                    else -> dialog
                }
            )
        }
    }
    
    /**
     * Update selected icon in dialog
     */
    fun updateDialogIconName(iconName: String) {
        _uiState.update { state ->
            state.copy(
                dialogState = when (val dialog = state.dialogState) {
                    is DialogState.Add -> dialog.copy(selectedIconName = iconName)
                    is DialogState.Edit -> dialog.copy(selectedIconName = iconName)
                    else -> dialog
                }
            )
        }
    }
    
    /**
     * Save category (add or edit)
     */
    fun saveCategory() {
        viewModelScope.launch {
            val dialogState = _uiState.value.dialogState
            
            when (dialogState) {
                is DialogState.Add -> addCategory(dialogState)
                is DialogState.Edit -> editCategory(dialogState)
                else -> return@launch
            }
        }
    }
    
    /**
     * Add a new category
     */
    private suspend fun addCategory(dialogState: DialogState.Add) {
        val categoryName = dialogState.categoryName.trim()
        
        // Validation
        if (categoryName.isEmpty()) {
            _uiState.update { state ->
                state.copy(
                    dialogState = dialogState.copy(error = "Category name cannot be empty")
                )
            }
            return
        }
        
        if (categoryName.length > 50) {
            _uiState.update { state ->
                state.copy(
                    dialogState = dialogState.copy(error = "Category name too long (max 50 characters)")
                )
            }
            return
        }
        
        try {
            val newCategory = AppCategoryEntity(
                categoryName = categoryName,
                isCustom = true,
                addedAtUtc = System.currentTimeMillis()
            )
            
            categoryRepository.insertCategory(newCategory)
            
            // Hide dialog and refresh
            _uiState.update { it.copy(dialogState = DialogState.Hidden) }
            loadCategories()
            
        } catch (e: CategoryAlreadyExistsException) {
            _uiState.update { state ->
                state.copy(
                    dialogState = dialogState.copy(error = "A category with this name already exists")
                )
            }
        } catch (e: Exception) {
            _uiState.update { state ->
                state.copy(
                    dialogState = dialogState.copy(error = "Failed to add category: ${e.message}")
                )
            }
        }
    }
    
    /**
     * Edit an existing category
     */
    private suspend fun editCategory(dialogState: DialogState.Edit) {
        val categoryName = dialogState.categoryName.trim()
        
        // Validation
        if (categoryName.isEmpty()) {
            _uiState.update { state ->
                state.copy(
                    dialogState = dialogState.copy(error = "Category name cannot be empty")
                )
            }
            return
        }
        
        if (categoryName.length > 50) {
            _uiState.update { state ->
                state.copy(
                    dialogState = dialogState.copy(error = "Category name too long (max 50 characters)")
                )
            }
            return
        }
        
        // System categories cannot be renamed
        if (!dialogState.isCustom) {
            _uiState.update { state ->
                state.copy(
                    dialogState = dialogState.copy(error = "System categories cannot be renamed")
                )
            }
            return
        }
        
        try {
            val category = categoryRepository.getCategoryById(dialogState.categoryId)
            if (category != null) {
                val updatedCategory = category.copy(categoryName = categoryName)
                categoryRepository.updateCategory(updatedCategory)
                
                // Hide dialog and refresh
                _uiState.update { it.copy(dialogState = DialogState.Hidden) }
                loadCategories()
            }
            
        } catch (e: CategoryAlreadyExistsException) {
            _uiState.update { state ->
                state.copy(
                    dialogState = dialogState.copy(error = "A category with this name already exists")
                )
            }
        } catch (e: Exception) {
            _uiState.update { state ->
                state.copy(
                    dialogState = dialogState.copy(error = "Failed to update category: ${e.message}")
                )
            }
        }
    }
    
    /**
     * Show delete category confirmation
     */
    fun showDeleteConfirmation(categoryId: Int) {
        val category = _uiState.value.categories.find { it.id == categoryId }
        if (category != null) {
            _uiState.update { 
                it.copy(
                    confirmationDialog = ConfirmationDialog.DeleteCategory(
                        categoryId = categoryId,
                        categoryName = category.name,
                        appCount = category.appCount
                    )
                )
            }
        }
    }
    
    /**
     * Delete a category
     */
    fun deleteCategory(categoryId: Int) {
        viewModelScope.launch {
            try {
                val category = categoryRepository.getCategoryById(categoryId)
                
                // Prevent deleting system categories
                if (category != null && !category.isCustom) {
                    _uiState.update { 
                        it.copy(
                            confirmationDialog = null,
                            error = "System categories cannot be deleted"
                        )
                    }
                    return@launch
                }
                
                // Delete category (apps will be moved to uncategorized)
                categoryRepository.deleteCategory(categoryId, uncategorizedCategoryId)
                
                // Hide confirmation and refresh
                _uiState.update { 
                    it.copy(
                        confirmationDialog = null,
                        selectedCategoryId = if (it.selectedCategoryId == categoryId) null else it.selectedCategoryId
                    )
                }
                loadCategories()
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        confirmationDialog = null,
                        error = "Failed to delete category: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Show remove app confirmation
     */
    fun showRemoveAppConfirmation(appId: Int) {
        val app = _uiState.value.appsInSelectedCategory.find { it.id == appId }
        val categoryId = _uiState.value.selectedCategoryId
        
        if (app != null && categoryId != null) {
            _uiState.update { 
                it.copy(
                    confirmationDialog = ConfirmationDialog.RemoveApp(
                        appId = appId,
                        appName = app.appName,
                        categoryId = categoryId
                    )
                )
            }
        }
    }
    
    /**
     * Remove an app from the current category
     */
    fun removeAppFromCategory(appId: Int) {
        viewModelScope.launch {
            try {
                if (uncategorizedCategoryId == null) {
                    _uiState.update { 
                        it.copy(
                            confirmationDialog = null,
                            error = "Uncategorized category not found"
                        )
                    }
                    return@launch
                }
                
                // Move app to uncategorized
                categoryRepository.removeAppFromCategory(appId, uncategorizedCategoryId!!)
                
                // Hide confirmation and refresh
                _uiState.update { it.copy(confirmationDialog = null) }
                loadCategories()
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        confirmationDialog = null,
                        error = "Failed to remove app: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Show add app bottom sheet
     */
    fun showAddAppBottomSheet() {
        loadAvailableAppsToAdd()
        _uiState.update { it.copy(showAddAppBottomSheet = true) }
    }
    
    /**
     * Hide add app bottom sheet
     */
    fun hideAddAppBottomSheet() {
        _uiState.update { it.copy(showAddAppBottomSheet = false) }
    }
    
    /**
     * Load apps from other categories that can be added
     */
    private fun loadAvailableAppsToAdd() {
        viewModelScope.launch {
            val selectedCategoryId = _uiState.value.selectedCategoryId ?: return@launch
            
            try {
                val allCategories = categoryRepository.getAllCategories().first()
                val packageManager = context.packageManager
                val availableApps = mutableListOf<AppItemWithCategory>()
                
                // Get apps from all categories except the selected one
                allCategories.forEach { category ->
                    if (category.id != selectedCategoryId) {
                        val apps = categoryRepository.getAppsByCategory(category.id).first()
                        apps.forEach { app ->
                            // Try to get app icon from package manager
                            val icon = try {
                                packageManager.getApplicationIcon(app.packageName)
                            } catch (e: PackageManager.NameNotFoundException) {
                                null
                            }
                            
                            availableApps.add(
                                AppItemWithCategory(
                                    id = app.id,
                                    packageName = app.packageName,
                                    appName = app.appName,
                                    icon = icon,
                                    categoryId = category.id,
                                    categoryName = category.categoryName
                                )
                            )
                        }
                    }
                }
                
                _uiState.update { 
                    it.copy(availableAppsToAdd = availableApps.sortedBy { app -> app.appName })
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to load available apps: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Show move app confirmation
     */
    fun showMoveAppConfirmation(appId: Int) {
        val app = _uiState.value.availableAppsToAdd.find { it.id == appId }
        val selectedCategoryId = _uiState.value.selectedCategoryId
        val toCategoryName = _uiState.value.categories.find { it.id == selectedCategoryId }?.name
        
        if (app != null && selectedCategoryId != null && toCategoryName != null) {
            _uiState.update { 
                it.copy(
                    confirmationDialog = ConfirmationDialog.MoveApp(
                        appId = appId,
                        appName = app.appName,
                        fromCategoryName = app.categoryName,
                        toCategoryId = selectedCategoryId,
                        toCategoryName = toCategoryName
                    ),
                    showAddAppBottomSheet = false
                )
            }
        }
    }
    
    /**
     * Move an app to the selected category
     */
    fun moveAppToCategory(appId: Int, toCategoryId: Int) {
        viewModelScope.launch {
            try {
                categoryRepository.updateAppCategory(appId, toCategoryId)
                
                // Hide confirmation and refresh
                _uiState.update { it.copy(confirmationDialog = null) }
                loadCategories()
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        confirmationDialog = null,
                        error = "Failed to move app: ${e.message}"
                    )
                }
            }
        }
    }
}
