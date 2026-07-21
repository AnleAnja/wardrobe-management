package com.anleanja.wardrobe.composables

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.anleanja.wardrobe.R
import com.anleanja.wardrobe.filter_sort.FilterDialog
import com.anleanja.wardrobe.filter_sort.OutfitFilters
import com.anleanja.wardrobe.filter_sort.OutfitSortOption
import com.anleanja.wardrobe.filter_sort.SortDialog
import com.anleanja.wardrobe.filter_sort.WardrobeFilters
import com.anleanja.wardrobe.filter_sort.WardrobeSortOption
import com.anleanja.wardrobe.view_models.OutfitScreenEvent
import com.anleanja.wardrobe.view_models.OutfitsViewModel
import com.anleanja.wardrobe.view_models.WardrobeScreenEvent
import com.anleanja.wardrobe.view_models.WardrobeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryTopAppBar(
    viewModel: WardrobeViewModel,
    title: String,
    onAddClick: () -> Unit,
    onAboutClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { fileUri ->
                viewModel.onEvent(WardrobeScreenEvent.ImportJson(context, fileUri))
            }
        }
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onEvent(WardrobeScreenEvent.ExportJson(context, it))
        }
    }

    TopAppBar(
        title = { Text(title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        navigationIcon = {
            Row {
                IconButton(onClick = {
                    filePickerLauncher.launch("application/json")
                }) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = stringResource(R.string.action_import_json)
                    )
                }
                IconButton(onClick = {
                    exportLauncher.launch("wardrobe_export.json")
                }) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = stringResource(R.string.action_export_json)
                    )
                }
            }
        },
        actions = {
            if (onAboutClick != null) {
                IconButton(onClick = onAboutClick) {
                    Icon(Icons.Default.Info, stringResource(R.string.action_about))
                }
            }
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, stringResource(R.string.action_add))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun WardrobeGalleryScreen(viewModel: WardrobeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSortDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.snackbarMessageConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    if (showSortDialog) {
        SortDialog(
            sortOptions = WardrobeSortOption.entries,
            currentSortOption = uiState.currentSortOption,
            onApplySort = { sortOption ->
                viewModel.onEvent(WardrobeScreenEvent.ApplySortOption(sortOption as WardrobeSortOption))
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }

    if (showFilterDialog) {
        FilterDialog(
            availableCategories = uiState.availableCategories,
            availableSeasons = uiState.availableSeasons,
            availableTemperature = false,
            selectedCategories = uiState.currentFilters.selectedCategories,
            selectedSeasons = uiState.currentFilters.selectedSeasons,
            selectedTemperature = 999,
            onApplyFilters = { categories, seasons, _ ->
                viewModel.onEvent(
                    WardrobeScreenEvent.ApplyFilters(
                        WardrobeFilters(
                            seasons,
                            categories
                        )
                    )
                )
                showFilterDialog = false
            },
            onClearFilters = {
                viewModel.onEvent(WardrobeScreenEvent.ClearFilters)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    WardrobeGalleryModernContent(
        uiState = uiState,
        onSortClick = { showSortDialog = true },
        onFilterClick = { showFilterDialog = true },
        onRefreshClick = { viewModel.onEvent(WardrobeScreenEvent.RefreshRequested) },
        onItemClick = { item -> viewModel.onEvent(WardrobeScreenEvent.ItemClicked(item)) }
    )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
fun OutfitGalleryScreen(viewModel: OutfitsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    if (showSortDialog) {
        SortDialog(
            sortOptions = OutfitSortOption.entries,
            currentSortOption = uiState.currentSortOption,
            onApplySort = { sortOption ->
                viewModel.onEvent(OutfitScreenEvent.ApplySortOption(sortOption as OutfitSortOption))
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }

    if (showFilterDialog) {
        FilterDialog(
            availableCategories = null,
            availableSeasons = uiState.availableSeasons,
            availableTemperature = true,
            selectedCategories = emptySet(),
            selectedSeasons = uiState.currentFilters.selectedSeasons,
            selectedTemperature = uiState.currentFilters.temperature,
            onApplyFilters = { _, seasons, temp ->
                viewModel.onEvent(
                    OutfitScreenEvent.ApplyFilters(
                        OutfitFilters(
                            seasons,
                            temp
                        )
                    )
                )
                showFilterDialog = false
            },
            onClearFilters = {
                viewModel.onEvent(OutfitScreenEvent.ClearFilters)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    OutfitGalleryModernContent(
        uiState = uiState,
        onSortClick = { showSortDialog = true },
        onFilterClick = { showFilterDialog = true },
        onRefreshClick = { viewModel.onEvent(OutfitScreenEvent.RefreshRequested) },
        onOutfitClick = { outfit ->
            if (uiState.isSelectionMode) {
                viewModel.onEvent(OutfitScreenEvent.OutfitSelectedForDate(outfit))
            } else {
                viewModel.onEvent(OutfitScreenEvent.OutfitClicked(outfit))
            }
        }
    )
}

@Composable
fun <T> GalleryGrid(
    items: List<T>,
    imageUriProvider: (T) -> String?,
    contentDescriptionProvider: (T) -> String?,
    onItemClick: (T) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(items) { item ->
            GalleryCard(
                item,
                imageUriProvider,
                contentDescriptionProvider,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
fun <T> GalleryCard(
    item: T,
    imageUriProvider: (T) -> String?,
    contentDescriptionProvider: (T) -> String?,
    onClick: () -> Unit
) {
    val imageUri = imageUriProvider(item)
    val description = contentDescriptionProvider(item)
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(true) }
    LaunchedEffect(key1 = imageUri) {
        if (imageUri != null && imageUri.startsWith("content://")) {
            val uri = imageUri.toUri()
            val persistedUris = context.contentResolver.persistedUriPermissions
            val hasPersistedPermission = persistedUris.any { it.uri == uri && it.isReadPermission }

            if (!hasPersistedPermission) {
                hasPermission = false
            }
        }
    }
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .aspectRatio(0.75f),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (hasPermission) {
            AsyncImage(
                model = imageUri,
                contentDescription = description,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Permission denied",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
