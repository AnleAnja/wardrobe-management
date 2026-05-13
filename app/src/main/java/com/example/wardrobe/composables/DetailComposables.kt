package com.example.wardrobe.composables

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.wardrobe.database.entities.WardrobeItem
import com.example.wardrobe.view_models.AddItemEvent
import com.example.wardrobe.view_models.AddOutfitEvent
import com.example.wardrobe.view_models.AddOutfitUiState
import com.example.wardrobe.view_models.AddOutfitViewModel
import com.example.wardrobe.view_models.CalendarEvent
import com.example.wardrobe.view_models.ItemDetailUiState
import com.example.wardrobe.view_models.OutfitDetailEvent
import com.example.wardrobe.view_models.OutfitDetailUiState
import com.example.wardrobe.view_models.WardrobeScreenEvent
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsTopAppBar(
    title: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onDeleteClick: (Int) -> Unit,
    id: Int?
) {
    var menuExpanded by remember { mutableStateOf(false) }
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
                Text("Wenn du das Item löschst, kannst du es nicht wiederherstellen.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        id?.let { id ->
                            onDeleteClick(id)
                        }
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
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back"
                )
            }
        },
        actions = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            id?.let { onNavigateToEdit(it) }
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showDialog = true
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun ItemDetailsContent(uiState: ItemDetailUiState) {
    when {
        uiState.isLoading -> ModernLoadingState()

        uiState.errorMessage != null -> ModernErrorState(message = uiState.errorMessage)

        else -> {
            uiState.item?.let { item ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ModernMediaCard(
                        imageUri = item.imageUri,
                        contentDescription = item.modernTitle(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f),
                        contentScale = ContentScale.Fit
                    )
                    ModernSectionCard(title = "Details") {
                        val categoryDescription = listOfNotNull(item.category, item.subcategory)
                            .filter { it.isNotBlank() }
                            .joinToString(" / ")
                        ModernDetailRow(label = "Category", value = categoryDescription)
                        ModernDetailRow(label = "Seasons", value = item.seasons)
                        ModernDetailRow(label = "Rating", value = item.rating?.takeIf { it > 0 }?.let { "$it/5" })
                        ModernDetailRow(label = "Price", value = item.price?.let { "$it €" })
                        ModernDetailRow(label = "Times worn", value = item.timesWorn.toString())
                        val daysText = when (uiState.daysSinceLastWear) {
                            null -> "Never worn"
                            0 -> "Today"
                            1 -> "Yesterday"
                            else -> "${uiState.daysSinceLastWear} days ago"
                        }
                        ModernDetailRow(label = "Last worn", value = daysText)
                        val formattedDate = item.purchaseDate?.let {
                            SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "Not set"
                        ModernDetailRow(label = "Purchase date", value = formattedDate)
                    }
                }
            } ?: ModernEmptyState("No item found", "This item is no longer available.")
        }
    }
}

@Composable
fun OutfitDetailsContent(
    uiState: OutfitDetailUiState,
    onEvent: (OutfitDetailEvent) -> Unit,
    onItemClick: (Int) -> Unit
) {
    when {
        uiState.isLoading -> ModernLoadingState()

        uiState.errorMessage != null -> ModernErrorState(message = uiState.errorMessage)

        uiState.isCalendarDialogVisible -> {
            ScheduleOutfitDialog(
                onDateSelected = { date, temperature ->
                    onEvent(OutfitDetailEvent.ScheduleOutfitForDate(date, temperature))
                },
                onDismiss = {
                    onEvent(OutfitDetailEvent.DismissCalendarDialog)
                }
            )
        }

        else -> {
            uiState.outfit?.let { outfit ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val imageUris = listOf(outfit.imageUriCombined, outfit.imageUriTeaser)
                    val pagerState = rememberPagerState(pageCount = { imageUris.size })
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        ) { page ->
                            ModernMediaCard(
                                imageUri = imageUris[page],
                                contentDescription = "Outfit image ${page + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(pagerState.pageCount) { iteration ->
                                val color = if (pagerState.currentPage == iteration) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
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
                    ModernSectionCard(
                        title = "Items",
                        subtitle = "${uiState.itemsInOutfit.size} wardrobe pieces"
                    ) {
                        Column {
                            uiState.itemsInOutfit.forEach { item ->
                                OutfitItemRow(
                                    item = item,
                                    onClick = { onItemClick(item.id) }
                                )
                            }
                        }
                    }
                    ModernSectionCard(title = "Details") {
                    val rating = outfit.rating?.takeIf { it > 0 }?.let { "$it/5" }
                    ModernDetailRow(label = "Rating", value = rating)
                    ModernDetailRow(label = "Seasons", value = outfit.seasons)
                    val temperatureText = if (uiState.minTemp != null && uiState.maxTemp != null) {
                        if (uiState.minTemp == uiState.maxTemp) {
                            "${uiState.minTemp}°C"
                        } else {
                            "${uiState.minTemp}°C - ${uiState.maxTemp}°C"
                        }
                    } else {
                        "Not worn yet"
                    }
                    ModernDetailRow(
                        label = "Temperature",
                        value = temperatureText
                    )
                    }
                    ModernSectionCard(title = "Stats") {
                    ModernDetailRow(label = "Times worn", value = outfit.timesWorn.toString())
                    val daysText = when (uiState.daysSinceLastWear) {
                        null -> "Never worn"
                        0 -> "Today"
                        1 -> "Yesterday"
                        else -> "${uiState.daysSinceLastWear} days ago"
                    }
                    ModernDetailRow(label = "Last worn", value = daysText)
                    }

                    Button(
                        onClick = { onEvent(OutfitDetailEvent.AddScheduledOutfit) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = MaterialTheme.shapes.large,
                    ) { Text("Add to OOTD", modifier = Modifier.padding(vertical = 8.dp)) }
                }
            } ?: ModernEmptyState("No outfit found", "This outfit is no longer available.")
        }
    }
}

@Composable
private fun OutfitItemRow(
    item: WardrobeItem,
    onClick: () -> Unit
) {
    ModernWardrobeItemListItem(item = item, onClick = onClick)
}

@Composable
fun ScheduleOutfitDialog(
    onDateSelected: (date: LocalDate, temperature: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(24) }
    val endMonth = remember { currentMonth.plusMonths(24) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val coroutineScope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var temperatureInput by remember { mutableStateOf("") }

    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (selectedDate == null) "Plan Outfit" else "Enter Temperature")
        },
        text = {
            Column(modifier = Modifier.animateContentSize()) {
                AnimatedVisibility(visible = selectedDate == null) {
                    Column {
                        val monthTitle =
                            calendarState.firstVisibleMonth.yearMonth.month.getDisplayName(
                                TextStyle.FULL, Locale.getDefault()
                            ) + " " + calendarState.firstVisibleMonth.yearMonth.year
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    calendarState.animateScrollToMonth(
                                        calendarState.firstVisibleMonth.yearMonth.minusMonths(
                                            1
                                        )
                                    )
                                }
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Month"
                                )
                            }
                            Text(text = monthTitle, fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    calendarState.animateScrollToMonth(
                                        calendarState.firstVisibleMonth.yearMonth.plusMonths(
                                            1
                                        )
                                    )
                                }
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Month"
                                )
                            }
                        }
                        // Tage der Woche
                        Row(modifier = Modifier.fillMaxWidth()) {
                            DayOfWeek.entries.forEach { dayOfWeek ->
                                Text(
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    text = dayOfWeek.getDisplayName(
                                        TextStyle.SHORT,
                                        Locale.getDefault()
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Der Kalender selbst
                        HorizontalCalendar(
                            state = calendarState,
                            dayContent = { day ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .clickable(
                                            enabled = day.position == DayPosition.MonthDate,
                                            onClick = { selectedDate = day.date }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.date.dayOfMonth.toString(),
                                        color = when {
                                            day.position == DayPosition.MonthDate -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
                AnimatedVisibility(visible = selectedDate != null) {
                    Column {
                        if (selectedDate != null) {
                            Text(
                                "Selected Date:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // Zeige das Datum als Vorschau an
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { selectedDate = null } // Auswahl rückgängig machen
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedDate!!.format(
                                        DateTimeFormatter.ofLocalizedDate(
                                            FormatStyle.MEDIUM
                                        )
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        OutlinedTextField(
                            value = temperatureInput,
                            onValueChange = { temperatureInput = it },
                            label = { Text("Temperature (°C)") },
                            placeholder = { Text("e.g., 15") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedDate?.let {
                        val temp = temperatureInput.toIntOrNull()
                        onDateSelected(it, temp)
                    }
                },
                enabled = selectedDate != null // Button ist nur aktiv, wenn ein Datum ausgewählt ist
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ScheduledOutfitDetailsContent(
    uiState: OutfitDetailUiState,
    onNavigateToFullOutfitDetail: (Int) -> Unit,
    onItemClick: (Int) -> Unit,
    onNavigateToEdit: (Int) -> Unit
) {
    val outfit = uiState.outfit
    val scheduledOutfit = uiState.scheduledOutfit
    when {
        uiState.isLoading -> ModernLoadingState()
        uiState.errorMessage != null -> ModernErrorState(message = uiState.errorMessage)
        outfit != null && scheduledOutfit != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            dateAndTempString,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton({ onNavigateToEdit(scheduledOutfit.id) }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit planned outfit")
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

                Spacer(Modifier.weight(1f))

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
                uiState.errorMessage ?: "An unknown error occurred.",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }

        Button(
            onClick = { viewModel.onEvent(AddOutfitEvent.SaveOutfit) },
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