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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.ScheduledOutfit
import com.example.wardrobe.view_models.CalendarEvent
import com.example.wardrobe.view_models.CalendarUiState
import com.example.wardrobe.view_models.CalendarView
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.WeekDayPosition
import com.kizitonwose.calendar.core.atStartOfMonth
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.yearMonth
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun CalendarContent(
    uiState: CalendarUiState,
    onEvent: (CalendarEvent) -> Unit,
    allOutfits: List<Outfit>,
    onOutfitClick: (Int) -> Unit
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val coroutineScope = rememberCoroutineScope()
    val isPlanningMode = uiState.outfit != null

    val monthState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    val weekState = rememberWeekCalendarState(
        startDate = startMonth.atStartOfMonth(),
        endDate = endMonth.atEndOfMonth(),
        firstVisibleWeekDate = LocalDate.now(),
        firstDayOfWeek = firstDayOfWeek
    )

    LaunchedEffect(uiState.calendarView) {
        if (uiState.calendarView == CalendarView.MONTH) {
            monthState.animateScrollToMonth(uiState.selectedDate.yearMonth)
        } else {
            weekState.animateScrollToWeek(uiState.selectedDate)
        }
    }

    if (uiState.isOutfitSelectionDialogVisible) {
        OutfitSelectionDialog(
            outfits = allOutfits,
            onOutfitSelected = { outfit, temperature ->
                onEvent(CalendarEvent.ScheduleOutfitForDate(outfit, temperature))
            },
            onDismiss = {
                onEvent(CalendarEvent.DismissOutfitSelectionDialog)
            }
        )
    }

    Column {
        if (isPlanningMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Choose a date to plan outfit #${uiState.outfit.id}",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
        CalendarHeader(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            currentMonth = if (uiState.calendarView == CalendarView.MONTH) {
                monthState.firstVisibleMonth.yearMonth
            } else {
                weekState.firstVisibleWeek.days.first().date.yearMonth
            },
            goToPrevious = {
                coroutineScope.launch {
                    if (uiState.calendarView == CalendarView.MONTH) {
                        monthState.animateScrollToMonth(
                            monthState.firstVisibleMonth.yearMonth.minusMonths(
                                1
                            )
                        )
                    } else {
                        weekState.animateScrollToWeek(
                            weekState.firstVisibleWeek.days.first().date.minusWeeks(
                                1
                            )
                        )
                    }
                }
            },
            goToNext = {
                coroutineScope.launch {
                    if (uiState.calendarView == CalendarView.MONTH) {
                        monthState.animateScrollToMonth(
                            monthState.firstVisibleMonth.yearMonth.plusMonths(
                                1
                            )
                        )
                    } else {
                        weekState.animateScrollToWeek(
                            weekState.firstVisibleWeek.days.first().date.plusWeeks(
                                1
                            )
                        )
                    }
                }
            }
        )

        DaysOfWeekTitle(daysOfWeek = firstDayOfWeek.let {
            val days = DayOfWeek.entries.toTypedArray()
            val rotated =
                days.slice(it.value - 1 until days.size) + days.slice(0 until it.value - 1)
            rotated
        })

        if (uiState.calendarView == CalendarView.MONTH) {
            HorizontalCalendar(
                state = monthState,
                dayContent = { day ->
                    Day(
                        date = day.date,
                        isFromCurrentMonth = day.position == DayPosition.MonthDate,
                        isSelected = uiState.selectedDate == day.date,
                        hasEvent = day.date in uiState.eventDays
                    ) { date ->

                        onEvent(CalendarEvent.DateSelected(date))
                    }
                }
            )
        } else {
            WeekCalendar(
                state = weekState,
                dayContent = { day ->
                    Day(
                        date = day.date,
                        isFromCurrentMonth = day.position == WeekDayPosition.RangeDate,
                        isSelected = uiState.selectedDate == day.date,
                        hasEvent = day.date in uiState.eventDays
                    ) { date ->

                        onEvent(CalendarEvent.DateSelected(date))
                    }
                }
            )
        }

        HorizontalDivider()

        ScheduledOutfitsList(
            selectedDate = uiState.selectedDate,
            scheduledOutfits = uiState.outfitsForSelectedDate,
            onEvent,
            onOutfitClick
        )
    }
}

@Composable
private fun Day(
    date: LocalDate,
    isFromCurrentMonth: Boolean,
    isSelected: Boolean,
    hasEvent: Boolean,
    onClick: (LocalDate) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(
                enabled = isFromCurrentMonth,
                onClick = { onClick(date) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isFromCurrentMonth -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
            )
            if (hasEvent && isFromCurrentMonth) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun ScheduledOutfitsList(
    selectedDate: LocalDate,
    scheduledOutfits: List<Pair<ScheduledOutfit, Outfit>>,
    onEvent: (CalendarEvent) -> Unit,
    onOutfitClick: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        Text(
            text = "Outfits for ${selectedDate.format(formatter)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (scheduledOutfits.isEmpty()) {
            Text("No outfits planned for this date")
            Button(
                onClick = { onEvent(CalendarEvent.AddOutfitForDate(selectedDate)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(8.dp),
            ) { Text("Add Outfit", modifier = Modifier.padding(vertical = 8.dp)) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(scheduledOutfits, key = { it.first.id }) { (scheduledOutfit, outfit) ->
                    OutfitRow(
                        outfit = outfit,
                        isSelected = false,
                        onRowClick = { onOutfitClick(scheduledOutfit.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarHeader(
    modifier: Modifier = Modifier,
    currentMonth: YearMonth,
    goToPrevious: () -> Unit,
    goToNext: () -> Unit,
) {
    val formatter =
        remember { DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.getDefault()) }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(onClick = goToPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
        }
        Text(
            text = currentMonth.format(formatter)
                .replaceFirstChar { it.titlecase(java.util.Locale.getDefault()) },
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )
        IconButton(onClick = goToNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
        }
    }
}


@Composable
private fun DaysOfWeekTitle(daysOfWeek: List<DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayOfWeek.getDisplayName(
                    java.time.format.TextStyle.SHORT,
                    java.util.Locale.getDefault()
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OutfitRow(outfit: Outfit,
                      isSelected: Boolean,
                      onRowClick: (Outfit) -> Unit) {
    Row(Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp)) // Ecken abrunden für eine schönere Optik
        .background(
            // Hintergrundfarbe basierend auf dem Auswahlstatus ändern
            if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else Color.Transparent
        )
        .clickable { onRowClick(outfit) }
        .padding(16.dp), // Padding nach innen anwenden
        verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = outfit.imageUriTeaser,
            contentDescription = "Outfit Teaser",
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text("Outfit ID: ${outfit.id}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTopAppBar(currentView: CalendarView, title: String, onToggleView: () -> Unit) {
    TopAppBar(
        title = { Text(title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        actions = {
            IconButton(onClick = onToggleView) {
                Icon(
                    imageVector = if (currentView == CalendarView.MONTH) Icons.Default.CalendarViewWeek else Icons.Default.CalendarViewMonth,
                    contentDescription = "Toggle Calendar View"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun OutfitSelectionDialog(
    outfits: List<Outfit>,
    onOutfitSelected: (outfit: Outfit, temperature: Int?) -> Unit,
    onDismiss: () -> Unit
) {

    var selectedOutfit by remember { mutableStateOf<Outfit?>(null) }
    var temperatureInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (selectedOutfit == null) "Select an Outfit" else "Enter Temperature")
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AnimatedVisibility(visible = selectedOutfit == null) {
                    if (outfits.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No outfits available. Create an outfit first!",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(outfits, key = { it.id }) { outfit ->
                                OutfitRow(
                                    outfit = outfit,
                                    isSelected = false,
                                    onRowClick = {
                                        selectedOutfit = it
                                    }
                                )
                            }
                        }
                    }
                }
                AnimatedVisibility(visible = selectedOutfit != null) {
                    selectedOutfit?.let { outfit ->
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                "Selected:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutfitRow(
                                outfit = selectedOutfit!!,
                                isSelected = true,
                                onRowClick = {
                                    selectedOutfit = null
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedOutfit?.let {
                        val temp = temperatureInput.toIntOrNull()
                        onOutfitSelected(it, temp)
                    }
                },
                enabled = selectedOutfit != null
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