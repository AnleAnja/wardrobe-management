package com.anleanja.wardrobe.composables

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
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.anleanja.wardrobe.CategoryHierarchy
import com.anleanja.wardrobe.database.entities.WardrobeItem
import com.anleanja.wardrobe.view_models.AddItemEvent
import com.anleanja.wardrobe.view_models.AddItemUiState
import com.anleanja.wardrobe.view_models.AddItemViewModel
import com.anleanja.wardrobe.view_models.AddOutfitEvent
import com.anleanja.wardrobe.view_models.AddOutfitUiState
import com.anleanja.wardrobe.view_models.AddOutfitViewModel
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
                "Purchased",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.purchaseDate?.let {
                        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "",
                    onValueChange = {},
                    placeholder = { Text("Select a date") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Select date"
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }
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
