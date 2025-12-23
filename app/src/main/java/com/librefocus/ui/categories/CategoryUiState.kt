package com.librefocus.ui.categories

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * UI State for the Category Management Screen
 */
data class CategoryUiState(
    val categories: List<CategoryItem> = emptyList(),
    val selectedCategoryId: Int? = null,
    val appsInSelectedCategory: List<AppItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val dialogState: DialogState = DialogState.Hidden,
    val confirmationDialog: ConfirmationDialog? = null
)

/**
 * Category item with display information
 */
data class CategoryItem(
    val id: Int,
    val name: String,
    val icon: ImageVector,
    val isCustom: Boolean,
    val appCount: Int
)

/**
 * App item with display information
 */
data class AppItem(
    val id: Int,
    val packageName: String,
    val appName: String,
    val icon: Any? = null // Drawable or ImageVector
)

/**
 * Dialog state for Add/Edit category
 */
sealed class DialogState {
    object Hidden : DialogState()
    data class Add(
        val categoryName: String = "",
        val selectedIconName: String = "Category",
        val error: String? = null
    ) : DialogState()
    
    data class Edit(
        val categoryId: Int,
        val categoryName: String,
        val selectedIconName: String,
        val isCustom: Boolean,
        val error: String? = null
    ) : DialogState()
}

/**
 * Confirmation dialog state
 */
sealed class ConfirmationDialog {
    data class DeleteCategory(
        val categoryId: Int,
        val categoryName: String,
        val appCount: Int
    ) : ConfirmationDialog()
    
    data class RemoveApp(
        val appId: Int,
        val appName: String,
        val categoryId: Int
    ) : ConfirmationDialog()
}
