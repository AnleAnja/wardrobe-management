package com.anleanja.wardrobe.composables

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anleanja.wardrobe.R
import com.anleanja.wardrobe.view_models.AddOutfitEvent
import com.anleanja.wardrobe.view_models.AddOutfitUiState
import com.anleanja.wardrobe.view_models.AddOutfitViewModel
import kotlinx.coroutines.launch

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

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(AddOutfitEvent.ImageUriChanged(uri))
        }
    }

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
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutfitCanvasEditor(
                            items = selectedItems,
                            graphicsLayer = graphicsLayer
                        )
                        Text(
                            text = stringResource(R.string.outfit_image_or_canvas_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                pickMedia.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddPhotoAlternate,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.upload_outfit_photo))
                        }
                    }
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
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_outfit_image))
        }
    }
}
