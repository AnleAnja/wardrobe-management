package com.example.wardrobe.composables

import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.wardrobe.CategoryHierarchy
import com.example.wardrobe.database.entities.WardrobeItem
import com.example.wardrobe.view_models.AddItemEvent
import com.example.wardrobe.view_models.AddItemUiState
import com.example.wardrobe.view_models.AddItemViewModel
import com.example.wardrobe.view_models.AddOutfitEvent
import com.example.wardrobe.view_models.AddOutfitUiState
import com.example.wardrobe.view_models.AddOutfitViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddItemScreen(onNavigateBack: () -> Unit) {
    val viewModel: AddItemViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
            viewModel.onEvent(AddItemEvent.ClearSuccess)
        }
    }

    AddItemForm(uiState, viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTopAppBar(
    onNavigateBack: () -> Unit,
    isEditMode: Boolean,
    isOutfit: Boolean
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            title = {
                Text(text = "Bist du sicher?")
            },
            text = {
                Text("Wenn du zurückgehst, werden deine Eingaben nicht gespeichert.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Ja")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                    }
                ) {
                    Text("Nein")
                }
            }
        )
    }
    TopAppBar(
        title = {
            Text(
                text = (if (isEditMode) "Edit " else "Add ") + (if (isOutfit) "Outfit" else "Item"),
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Close, contentDescription = "Close screen")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemForm(uiState: AddItemUiState, viewModel: AddItemViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var isCategoryMenuExpanded by remember { mutableStateOf(false) }
    var tempSelectedSuperCategory by remember { mutableStateOf<String?>(null) }
    var isSeasonsMenuExpanded by remember { mutableStateOf(false) }

    val selectedSeasonsList = remember(uiState.seasons) {
        uiState.seasons.split(", ").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    }

    val categoryDisplayValue = when {
        uiState.category.isNotEmpty() && uiState.subcategory.isNotEmpty() -> "${uiState.category} > ${uiState.subcategory}"
        uiState.category.isNotEmpty() -> uiState.category
        else -> ""
    }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(AddItemEvent.ImageUriChanged(uri))
        }
    }

    if (showDatePicker) {
        val datePickerState =
            rememberDatePickerState(initialSelectedDateMillis = uiState.purchaseDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.onEvent(AddItemEvent.PurchaseDateChanged(it))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            ImagePickerCard(
                imageUri = uiState.imageUri,
                onPickClick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onClearClick = {
                    viewModel.onEvent(AddItemEvent.ImageUriChanged(null))
                },
                label = "PNG/JPG"
            )
            Text(
                "Purchased Date",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
            OutlinedTextField(
                value = uiState.purchaseDate?.let {
                    SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                } ?: "Select a date",
                onValueChange = {},
                placeholder = { Text("Select Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                readOnly = true,
                shape = MaterialTheme.shapes.medium
            )
            Text(
                "Category",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
            ExposedDropdownMenuBox(
                expanded = isCategoryMenuExpanded,
                onExpandedChange = { isExpanded ->
                    isCategoryMenuExpanded = isExpanded
                    if (!isExpanded) {
                        tempSelectedSuperCategory = null
                    }
                }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    value = categoryDisplayValue,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Select Category") },
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.category.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.onEvent(AddItemEvent.ClearCategory)
                                tempSelectedSuperCategory = null
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                            }
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryMenuExpanded)
                        }
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    ),
                )
                ExposedDropdownMenu(
                    expanded = isCategoryMenuExpanded,
                    onDismissRequest = {
                        isCategoryMenuExpanded = false
                        tempSelectedSuperCategory = null
                    }
                ) {
                    if (tempSelectedSuperCategory == null) {
                        CategoryHierarchy.superCategories.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    tempSelectedSuperCategory = selectionOption
                                }
                            )
                        }
                    } else {
                        val superCategory = tempSelectedSuperCategory!!
                        DropdownMenuItem(
                            text = { Text("< Back to main categories") },
                            onClick = { tempSelectedSuperCategory = null }
                        )
                        HorizontalDivider()
                        val subCategories =
                            CategoryHierarchy.hierarchy[superCategory] ?: emptyList()
                        subCategories.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    viewModel.onEvent(
                                        AddItemEvent.CategoryChanged(
                                            superCategory,
                                            selectionOption
                                        )
                                    )
                                    isCategoryMenuExpanded = false
                                    tempSelectedSuperCategory = null
                                }
                            )
                        }
                    }
                }
            }
            Text(
                "Rating",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
            ModernRatingSelector(
                rating = uiState.rating,
                onRatingSelected = { rating -> viewModel.onEvent(AddItemEvent.RatingChanged(rating)) }
            )
            Text(
                "Price",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
            OutlinedTextField(
                value = uiState.price,
                onValueChange = { viewModel.onEvent(AddItemEvent.PriceChanged(it)) },
                placeholder = { Text("Select Price (€)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(8.dp)
            )
            Text(
                "Seasons",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
            ExposedDropdownMenuBox(
                expanded = isSeasonsMenuExpanded,
                onExpandedChange = { isSeasonsMenuExpanded = it }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(), // Required
                    readOnly = true, // We will display the comma-separated string
                    value = uiState.seasons.ifEmpty { "Select seasons" }, // Display selected seasons or placeholder
                    onValueChange = {},
                    placeholder = { Text("Select Seasons") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSeasonsMenuExpanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    ),
                )
                ExposedDropdownMenu(
                    expanded = isSeasonsMenuExpanded,
                    onDismissRequest = { isSeasonsMenuExpanded = false }
                ) {
                    viewModel.seasons.forEach { season ->
                        val isSelected = selectedSeasonsList.contains(season)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null // Null because onClick of DropdownMenuItem handles it
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(season)
                                }
                            },
                            onClick = {
                                if (isSelected) {
                                    selectedSeasonsList.remove(season)
                                } else {
                                    selectedSeasonsList.add(season)
                                }
                                viewModel.onEvent(
                                    AddItemEvent.SeasonsChanged(
                                        selectedSeasonsList.joinToString(
                                            ", "
                                        )
                                    )
                                )
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = { viewModel.onEvent(AddItemEvent.SaveItem) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !uiState.isLoading

            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Item", modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiImagePicker(
    imageUris: List<String?>,
    onPickClick: (index: Int) -> Unit,
    onClearClick: (index: Int) -> Unit,
    labels: List<String>,
    enabled: Boolean = true
) {
    val pagerState = rememberPagerState(pageCount = { imageUris.size })

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { pageIndex ->
            ImagePickerCard(
                imageUri = imageUris[pageIndex],
                onPickClick = { onPickClick(pageIndex) },
                onClearClick = { onClearClick(pageIndex) },
                label = labels[pageIndex],
                enabled = enabled
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color =
                    if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.2f
                    )
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}

@Composable
private fun ImagePickerCard(
    imageUri: String?,
    onPickClick: () -> Unit,
    onClearClick: () -> Unit,
    label: String,
    enabled: Boolean = true
) {
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    val outlineColor = MaterialTheme.colorScheme.outline
    if (imageUri == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .drawBehind {
                    drawRoundRect(
                        color = outlineColor,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = dashEffect
                        ),
                        cornerRadius = CornerRadius(20.dp.toPx())
                    )
                }
                .clickable(
                    onClick = onPickClick,
                    enabled = enabled
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddPhotoAlternate,
                    contentDescription = "Upload Photo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Upload a photo",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Selected image",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )
            IconButton(
                onClick = onClearClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = CircleShape
                    ),
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove"
                )
            }
            IconButton(
                onClick = onPickClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit"
                )
            }
        }
    }
}

@Composable
fun AddOutfitScreen(
    onNavigateBack: () -> Unit,
    isScheduledOutfit: Boolean = false
) {
    val viewModel: AddOutfitViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
            viewModel.onEvent(AddOutfitEvent.ClearSuccess)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            AddOutfitForm(uiState, viewModel, isScheduledOutfit)
        }
        Button(
            onClick = { viewModel.onEvent(AddOutfitEvent.SaveOutfit) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = uiState.selectedItemIds.isNotEmpty() && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Save Outfit")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOutfitForm(
    uiState: AddOutfitUiState,
    viewModel: AddOutfitViewModel,
    isScheduledOutfit: Boolean
) {
    var isSeasonsMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val selectedSeasonsList = remember(uiState.seasons) {
        uiState.seasons.split(", ").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    }

    val pickTeaserMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                viewModel.onEvent(AddOutfitEvent.ImageUriTeaserChanged(uri))
            }
        }
    val pickCombinedMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                viewModel.onEvent(AddOutfitEvent.ImageUriCombinedChanged(uri))
            }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            MultiImagePicker(
                imageUris = listOf(uiState.imageUriTeaser, uiState.imageUriCombined),
                labels = listOf("Add Outfit Image", "Add Combined Image"),
                onPickClick = { index ->
                    val request =
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    if (index == 0) {
                        pickTeaserMedia.launch(request)
                    } else {
                        pickCombinedMedia.launch(request)
                    }
                },
                onClearClick = { index ->
                    if (index == 0) {
                        viewModel.onEvent(AddOutfitEvent.ImageUriTeaserChanged(null))
                    } else {
                        viewModel.onEvent(AddOutfitEvent.ImageUriCombinedChanged(null))
                    }
                },
                enabled = !isScheduledOutfit
            )
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Rating",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
            ModernRatingSelector(
                rating = uiState.rating,
                onRatingSelected = { rating -> viewModel.onEvent(AddOutfitEvent.RatingChanged(rating)) },
                enabled = !isScheduledOutfit
            )
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Seasons",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
            ExposedDropdownMenuBox(
                expanded = isSeasonsMenuExpanded,
                onExpandedChange = { isSeasonsMenuExpanded = it }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(), // Required
                    readOnly = true, // We will display the comma-separated string
                    value = uiState.seasons.ifEmpty { "Select seasons" }, // Display selected seasons or placeholder
                    onValueChange = {},
                    placeholder = { Text("Select Seasons") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSeasonsMenuExpanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    ),
                    enabled = !isScheduledOutfit
                )
                ExposedDropdownMenu(
                    expanded = isSeasonsMenuExpanded,
                    onDismissRequest = { isSeasonsMenuExpanded = false }
                ) {
                    viewModel.seasons.forEach { season ->
                        val isSelected = selectedSeasonsList.contains(season)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null // Null because onClick of DropdownMenuItem handles it
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(season)
                                }
                            },
                            onClick = {
                                if (isSelected) {
                                    selectedSeasonsList.remove(season)
                                } else {
                                    selectedSeasonsList.add(season)
                                }
                                viewModel.onEvent(
                                    AddOutfitEvent.SeasonsChanged(
                                        selectedSeasonsList.joinToString(
                                            ", "
                                        )
                                    )
                                )
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            enabled = !isScheduledOutfit
                        )
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Items",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            ItemSelector(
                uiState.itemsByCategory,
                uiState.selectedItemIds,
                uiState.lockedItemIds,
                onItemToggle = { viewModel.onEvent(AddOutfitEvent.ItemsChanged(it)) })
        }
    }
}

fun <T> LazyListScope.gridItems(
    data: List<T>,
    columnCount: Int,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    itemContent: @Composable (T) -> Unit
) {
    val size = data.count()
    val rows = if (size == 0) 0 else 1 + (size - 1) / columnCount
    items(rows, key = { it }) { rowIndex ->
        Row(
            modifier = modifier,
            horizontalArrangement = horizontalArrangement
        ) {
            for (columnIndex in 0 until columnCount) {
                val itemIndex = rowIndex * columnCount + columnIndex
                if (itemIndex < size) {
                    Box(
                        modifier = Modifier.weight(1F, fill = true),
                        propagateMinConstraints = true
                    ) {
                        itemContent(data[itemIndex])
                    }
                } else {
                    Spacer(Modifier.weight(1F, fill = true))
                }
            }
        }
    }
}

@Composable
private fun SeasonSelector(
    availableSeasons: List<String>,
    selectedSeasons: List<String>,
    onSeasonToggle: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Select Seasons",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            availableSeasons.forEach { season ->
                FilterChip(
                    selected = season in selectedSeasons,
                    onClick = { onSeasonToggle(season) },
                    label = { Text(season) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemSelector(
    itemsByCategory: Map<String, List<WardrobeItem>>,
    selectedItemIds: Set<Int>,
    lockedItemIds: Set<Int>?,
    onItemToggle: (Int) -> Unit
) {
    val categories = itemsByCategory.keys.toList()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    if (categories.isEmpty()) {
        Box(Modifier
            .fillMaxWidth()
            .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No items in your wardrobe yet.")
        }
        return
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .height(500.dp)
    ) {
        ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(category) }
                )
            }
        }
        val itemsInSelectedCategory = itemsByCategory.get(categories.getOrNull(selectedTabIndex)) ?: emptyList()

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(itemsInSelectedCategory, key = { it.id }) { item ->
                val isSelected = item.id in selectedItemIds
                val isLocked = lockedItemIds?.contains(item.id) == true

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .border(
                            width = 3.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = MaterialTheme.shapes.medium
                        )
                        .clickable { onItemToggle(item.id) }
                ) {
                    ModernMediaCard(
                        imageUri = item.imageUri,
                        contentDescription = item.modernTitle(),
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        if (isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked Item",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f), CircleShape)
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}