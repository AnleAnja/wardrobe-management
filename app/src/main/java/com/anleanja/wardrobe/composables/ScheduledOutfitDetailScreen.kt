package com.anleanja.wardrobe.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anleanja.wardrobe.database.entities.WardrobeItem
import com.anleanja.wardrobe.view_models.AddItemEvent
import com.anleanja.wardrobe.R
import com.anleanja.wardrobe.view_models.AddOutfitEvent
import com.anleanja.wardrobe.view_models.AddOutfitUiState
import com.anleanja.wardrobe.view_models.AddOutfitViewModel
import com.anleanja.wardrobe.view_models.CalendarEvent
import com.anleanja.wardrobe.view_models.ItemDetailUiState
import com.anleanja.wardrobe.view_models.OutfitDetailEvent
import com.anleanja.wardrobe.view_models.OutfitDetailUiState
import com.anleanja.wardrobe.view_models.WardrobeScreenEvent
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale


@Composable
fun ScheduledOutfitDetailsContent(
    uiState: OutfitDetailUiState,
    onNavigateToFullOutfitDetail: (Int) -> Unit,
    onItemClick: (Int) -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    val outfit = uiState.outfit
    val scheduledOutfit = uiState.scheduledOutfit
    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog && scheduledOutfit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(text = "Bist du sicher?")
            },
            text = {
                Text("Wenn du das geplante Outfit löschst, kannst du es nicht wiederherstellen.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(scheduledOutfit.id)
                    }
                ) {
                    Text("Ja")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Nein")
                }
            }
        )
    }
    when {
        uiState.isLoading -> ModernLoadingState()
        uiState.errorMessage != null -> ModernErrorState(message = uiState.errorMessage)
        outfit != null && scheduledOutfit != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                ModernSectionCard(title = "Planned outfit") {
                    val dateAndTempString = buildString {
                        scheduledOutfit.date?.let { dateMillis ->
                            val date = Instant.ofEpochMilli(dateMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            val formatter = DateTimeFormatter.ofPattern("dd/MM")
                            append(date.format(formatter))
                        } ?: append("No Date")
                        scheduledOutfit.temperature?.let { temp ->
                            append("  ($temp°C)")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            dateAndTempString,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onNavigateToEdit(scheduledOutfit.id) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit planned outfit"
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete planned outfit"
                            )
                        }
                    }
                }
                ModernSectionCard(title = "Items") {
                    Column {
                        uiState.itemsInOutfit.forEach { item ->
                            OutfitItemRow(
                                item = item,
                                onClick = { onItemClick(item.id) }
                            )
                        }
                    }
                }

                Button(
                    onClick = { onNavigateToFullOutfitDetail(outfit.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("View Full Outfit Details")
                }
            }
        }
        else -> ModernEmptyState("No scheduled outfit found", "This planned outfit is no longer available.")
    }
}

@Composable
fun AddScheduledItemScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel: AddOutfitViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
            viewModel.onEvent(AddOutfitEvent.ClearSuccess)
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ItemSelector(
                itemsByCategory = uiState.itemsByCategory,
                selectedItemIds = uiState.selectedItemIds,
                lockedItemIds = uiState.lockedItemIds,
                onItemToggle = { viewModel.onEvent(AddOutfitEvent.ItemsChanged(it)) }
            )
        }

        uiState.errorMessage?.let { error ->
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }

        Button(
            onClick = { viewModel.onEvent(AddOutfitEvent.SaveOutfit()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = uiState.selectedItemIds.isNotEmpty() && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Save Outfit", modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}