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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anleanja.wardrobe.database.entities.WardrobeItem
import com.anleanja.wardrobe.view_models.AddItemEvent
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        ModernMediaCard(
                            imageUri = outfit.displayImageUri(),
                            contentDescription = "Outfit image",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit
                        )
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
internal fun OutfitItemRow(
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

