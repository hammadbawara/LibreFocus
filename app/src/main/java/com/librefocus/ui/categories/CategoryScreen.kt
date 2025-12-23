package com.librefocus.ui.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

/**
 * Category Management Screen with Material 3 List-Detail pane layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Categories") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddCategoryDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        
        // Show error snackbar if needed
        uiState.error?.let { errorMessage ->
            LaunchedEffect(errorMessage) {
                // Error shown, clear it after display
                viewModel.clearError()
            }
            
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(errorMessage)
            }
        }
        
        // Adaptive layout: Single pane on phones, dual pane on tablets
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Left Pane: Category List
            CategoryListPane(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                isLoading = uiState.isLoading,
                onCategoryClick = { viewModel.selectCategory(it) },
                onEditCategory = { viewModel.showEditCategoryDialog(it) },
                onDeleteCategory = { viewModel.showDeleteConfirmation(it) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            
            // Right Pane: App List (only on tablets or when category is selected)
            if (uiState.selectedCategoryId != null) {
                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )
                
                AppListPane(
                    apps = uiState.appsInSelectedCategory,
                    categoryName = uiState.categories.find { 
                        it.id == uiState.selectedCategoryId 
                    }?.name ?: "",
                    onRemoveApp = { viewModel.showRemoveAppConfirmation(it) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
    
    // Dialogs
    when (val dialogState = uiState.dialogState) {
        is DialogState.Add -> {
            AddEditCategoryDialog(
                title = "Add Category",
                categoryName = dialogState.categoryName,
                selectedIconName = dialogState.selectedIconName,
                error = dialogState.error,
                onCategoryNameChange = { viewModel.updateDialogCategoryName(it) },
                onIconSelect = { viewModel.updateDialogIconName(it) },
                onConfirm = { viewModel.saveCategory() },
                onDismiss = { viewModel.hideDialog() }
            )
        }
        
        is DialogState.Edit -> {
            AddEditCategoryDialog(
                title = "Edit Category",
                categoryName = dialogState.categoryName,
                selectedIconName = dialogState.selectedIconName,
                error = dialogState.error,
                isSystemCategory = !dialogState.isCustom,
                onCategoryNameChange = { viewModel.updateDialogCategoryName(it) },
                onIconSelect = { viewModel.updateDialogIconName(it) },
                onConfirm = { viewModel.saveCategory() },
                onDismiss = { viewModel.hideDialog() }
            )
        }
        
        else -> { /* No dialog */ }
    }
    
    // Confirmation dialogs
    when (val confirmation = uiState.confirmationDialog) {
        is ConfirmationDialog.DeleteCategory -> {
            AlertDialog(
                onDismissRequest = { viewModel.hideDialog() },
                title = { Text("Delete Category?") },
                text = { 
                    Text(
                        if (confirmation.appCount > 0) {
                            "Delete \"${confirmation.categoryName}\"? ${confirmation.appCount} app(s) will be moved to Uncategorized."
                        } else {
                            "Delete \"${confirmation.categoryName}\"?"
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.deleteCategory(confirmation.categoryId) }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        is ConfirmationDialog.RemoveApp -> {
            AlertDialog(
                onDismissRequest = { viewModel.hideDialog() },
                title = { Text("Remove App?") },
                text = { 
                    Text("Remove \"${confirmation.appName}\" from this category? It will be moved to Uncategorized.")
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.removeAppFromCategory(confirmation.appId) }
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        null -> { /* No confirmation */ }
    }
}

/**
 * Category List Pane (Left/Primary pane)
 */
@Composable
fun CategoryListPane(
    categories: List<CategoryItem>,
    selectedCategoryId: Int?,
    isLoading: Boolean,
    onCategoryClick: (Int) -> Unit,
    onEditCategory: (Int) -> Unit,
    onDeleteCategory: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            categories.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No categories yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(categories, key = { it.id }) { category ->
                        CategoryListItem(
                            category = category,
                            isSelected = category.id == selectedCategoryId,
                            onClick = { onCategoryClick(category.id) },
                            onEdit = { onEditCategory(category.id) },
                            onDelete = { onDeleteCategory(category.id) }
                        )
                        
                        Divider()
                    }
                }
            }
        }
    }
}

/**
 * Individual category list item
 */
@Composable
fun CategoryListItem(
    category: CategoryItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    ListItem(
        headlineContent = { 
            Text(
                category.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = { 
            Text("${category.appCount} app${if (category.appCount != 1) "s" else ""}")
        },
        leadingContent = {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    
                    if (category.isCustom) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/**
 * App List Pane (Right/Secondary pane)
 */
@Composable
fun AppListPane(
    apps: List<AppItem>,
    categoryName: String,
    onRemoveApp: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            
            Divider()
            
            // App list
            when {
                apps.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No apps in this category",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(apps, key = { it.id }) { app ->
                            AppListItem(
                                app = app,
                                onRemove = { onRemoveApp(app.id) }
                            )
                            
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual app list item
 */
@Composable
fun AppListItem(
    app: AppItem,
    onRemove: () -> Unit
) {
    ListItem(
        headlineContent = { 
            Text(
                app.appName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                app.packageName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            // Display app icon if available
            app.icon?.let { icon ->
                if (icon is android.graphics.drawable.Drawable) {
                    val bitmap = remember(icon) { icon.toBitmap().asImageBitmap() }
                    androidx.compose.foundation.Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove app",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}
