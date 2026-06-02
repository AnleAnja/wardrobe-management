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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.rememberGraphicsLayer
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
fun AddOutfitScreen(
    onNavigateBack: () -> Unit,
    isScheduledOutfit: Boolean = false
) {
    val viewModel: AddOutfitViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
            viewModel.onEvent(AddOutfitEvent.ClearSuccess)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            AddOutfitForm(uiState, viewModel, isScheduledOutfit, graphicsLayer)
        }
        Button(
            onClick = {
                if (uiState.imageUri == null && !isScheduledOutfit) {
                    scope.launch {
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        viewModel.onEvent(AddOutfitEvent.SaveOutfit(bitmap))
                    }
                } else {
                    viewModel.onEvent(AddOutfitEvent.SaveOutfit())
                }
            },
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
    isScheduledOutfit: Boolean,
    graphicsLayer: androidx.compose.ui.graphics.layer.GraphicsLayer
) {
    var isSeasonsMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val selectedSeasonsList = remember(uiState.seasons) {
        uiState.seasons.split(", ").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    }

    val selectedItems = remember(uiState.itemsByCategory, uiState.selectedItemIds) {
        uiState.itemsByCategory.values.flatten().filter { it.id in uiState.selectedItemIds }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isScheduledOutfit) {
            item {
                Spacer(Modifier.height(8.dp))
                if (uiState.imageUri == null) {
                    OutfitCanvasEditor(
                        items = selectedItems,
                        graphicsLayer = graphicsLayer
                    )
                } else {
                    OutfitImagePreview(
                        imageUri = uiState.imageUri,
                        onRemove = { viewModel.onEvent(AddOutfitEvent.RemoveImage) }
                    )
                }
            }
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

@Composable
private fun OutfitImagePreview(
    imageUri: String?,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Outfit image",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    CircleShape
                )
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove image")
        }
    }
}
