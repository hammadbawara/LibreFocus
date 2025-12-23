package com.librefocus.ui.categories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.librefocus.ui.common.AppScaffold
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Category Management Screen with Material 3 List-Detail pane layout
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun CategoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = rememberListDetailPaneScaffoldNavigator<Int>()
    val coroutineScope = rememberCoroutineScope()
    
    // Synchronize selected category with navigator
    LaunchedEffect(uiState.selectedCategoryId) {
        uiState.selectedCategoryId?.let { categoryId ->
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, categoryId)
        }
    }
    
    BackHandler(navigator.canNavigateBack()) {
        coroutineScope.launch {
            navigator.navigateBack()
        }
    }
    
    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                CategoryListPane(
                    uiState = uiState,
                    onNavigateBack = onNavigateBack,
                    onCategoryClick = { viewModel.selectCategory(it) },
                    onAddCategory = { viewModel.showAddCategoryDialog() },
                    onEditCategory = { viewModel.showEditCategoryDialog(it) },
                    onDeleteCategory = { viewModel.showDeleteConfirmation(it) },
                    onClearError = { viewModel.clearError() }
                )
            }
        },
        detailPane = {
            AnimatedPane {
                uiState.selectedCategoryId?.let { categoryId ->
                    val category = uiState.categories.find { it.id == categoryId }
                    if (category != null) {
                        CategoryDetailPane(
                            category = category,
                            apps = uiState.appsInSelectedCategory,
                            onRemoveApp = { viewModel.showRemoveAppConfirmation(it) },
                            onAddApp = { viewModel.showAddAppBottomSheet() },
                            onBack = {
                                coroutineScope.launch {
                                    navigator.navigateBack()
                                }
                            }
                        )
                    }
                }
            }
        }
    )
    
    // Dialogs and bottom sheets
    CategoryDialogsAndSheets(
        uiState = uiState,
        viewModel = viewModel
    )
}

@Composable
private fun CategoryDialogsAndSheets(
    uiState: CategoryUiState,
    viewModel: CategoryViewModel
) {
    // Add/Edit dialogs
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
        
        else -> {}
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
                    TextButton(onClick = { viewModel.deleteCategory(confirmation.categoryId) }) {
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
                    TextButton(onClick = { viewModel.removeAppFromCategory(confirmation.appId) }) {
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
        
        is ConfirmationDialog.MoveApp -> {
            AlertDialog(
                onDismissRequest = { viewModel.hideDialog() },
                title = { Text("Move App?") },
                text = { 
                    Text("Move \"${confirmation.appName}\" from \"${confirmation.fromCategoryName}\" to \"${confirmation.toCategoryName}\"?")
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.moveAppToCategory(confirmation.appId, confirmation.toCategoryId) }) {
                        Text("Move")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        null -> {}
    }
    
    // Add App Bottom Sheet
    if (uiState.showAddAppBottomSheet) {
        AddAppBottomSheet(
            availableApps = uiState.availableAppsToAdd,
            onAppSelect = { viewModel.showMoveAppConfirmation(it) },
            onDismiss = { viewModel.hideAddAppBottomSheet() }
        )
    }
}

/**
 * Category List Pane with ExtendedFAB
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListPane(
    uiState: CategoryUiState,
    onNavigateBack: () -> Unit,
    onCategoryClick: (Int) -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (Int) -> Unit,
    onDeleteCategory: (Int) -> Unit,
    onClearError: () -> Unit
) {
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val fabExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }
    
    AppScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = { Text("App Categories") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCategory,
                expanded = fabExpanded,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Category") }
            )
        },
        snackbarHost = {
            uiState.error?.let { errorMessage ->
                LaunchedEffect(errorMessage) {
                    onClearError()
                }
                SnackbarHost(
                    hostState = remember { SnackbarHostState() }
                ) {
                    Snackbar { Text(errorMessage) }
                }
            }
        },
        topAppBarScrollBehavior = scrollBehavior,
        showBottomBar = false
    ) { paddingValues, scrollModifier ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.categories.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
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
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .then(scrollModifier)
                ) {
                    items(uiState.categories, key = { it.id }) { category ->
                        CategoryListItem(
                            category = category,
                            isSelected = category.id == uiState.selectedCategoryId,
                            onClick = { onCategoryClick(category.id) },
                            onEdit = { onEditCategory(category.id) },
                            onDelete = { onDeleteCategory(category.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/**
 * Category list item
 */
@Composable
private fun CategoryListItem(
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
 * Category Detail Pane with apps list and ExtendedFAB
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailPane(
    category: CategoryItem,
    apps: List<AppItem>,
    onRemoveApp: (Int) -> Unit,
    onAddApp: () -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val fabExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }
    
    AppScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = { Text(category.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddApp,
                expanded = fabExpanded,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add App") }
            )
        },
        topAppBarScrollBehavior = scrollBehavior,
        showBottomBar = false
    ) { paddingValues, scrollModifier ->
        when {
            apps.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
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
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .then(scrollModifier)
                ) {
                    items(apps, key = { it.id }) { app ->
                        AppListItem(
                            app = app,
                            onRemove = { onRemoveApp(app.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/**
 * App list item
 */
@Composable
private fun AppListItem(
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

/**
 * Bottom sheet for adding apps to category
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppBottomSheet(
    availableApps: List<AppItemWithCategory>,
    onAppSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Add App to Category",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            HorizontalDivider()
            
            when {
                availableApps.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No apps available to add",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableApps, key = { it.id }) { app ->
                            AddAppListItem(
                                app = app,
                                onClick = { onAppSelect(app.id) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

/**
 * App item in Add App bottom sheet
 */
@Composable
private fun AddAppListItem(
    app: AppItemWithCategory,
    onClick: () -> Unit
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
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = app.categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
