package com.example.wardrobe.view_models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardrobe.database.OutfitRepository
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.ScheduledOutfit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

enum class CalendarView {
    WEEK, MONTH
}

data class CalendarUiState(
    val isLoading: Boolean = true,
    val calendarView: CalendarView = CalendarView.MONTH,
    val eventDays: Set<LocalDate> = emptySet(),
    val selectedDate: LocalDate = LocalDate.now(),
    val outfitsForSelectedDate: List<Pair<ScheduledOutfit, Outfit>> = emptyList(),
    val scheduledOutfits: List<ScheduledOutfit> = emptyList(),
    val outfit: Outfit? = null,
    val isOutfitSelectionDialogVisible: Boolean = false
)

sealed class CalendarEvent {
    data object ToggleView : CalendarEvent()
    data class DateSelected(val date: LocalDate) : CalendarEvent()
    data class ScheduleOutfitForDate(val outfit: Outfit, val temperature: Int?) : CalendarEvent()
    data class AddOutfitForDate(val date: LocalDate): CalendarEvent()
    data object DismissOutfitSelectionDialog : CalendarEvent()
    data class AddItemForScheduledOutfit(val scheduledOutfitId: Int) : CalendarEvent()
}

sealed class CalendarNavigationEvent {
    data class NavigateToEditScheduledOutfit(val scheduledOutfitId: Int) : CalendarNavigationEvent()
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val outfitRepository: OutfitRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _navigationEvent = MutableStateFlow<CalendarNavigationEvent?>(null)
    val navigationEvent: StateFlow<CalendarNavigationEvent?> = _navigationEvent.asStateFlow()

    private val outfitId: Int? = savedStateHandle.get<Int>("outfitId")
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    val allOutfitsFlow = outfitRepository.getAll()

    // Cache the latest scheduled list so date changes don't need to start a new collector.
    private var allScheduled: List<ScheduledOutfit> = emptyList()
    private var outfitsForDateJob: Job? = null

    init {
        viewModelScope.launch {
            outfitRepository.getAllScheduled().collect { scheduledOutfits ->
                allScheduled = scheduledOutfits
                val eventDaysSet = scheduledOutfits.mapNotNull { scheduled ->
                    scheduled.date?.let { dateInMillis ->
                        Date(dateInMillis).toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                }.toSet()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        eventDays = eventDaysSet,
                        scheduledOutfits = scheduledOutfits
                    )
                }
                updateOutfitsForSelectedDate(uiState.value.selectedDate)
            }
        }
        if (outfitId != null && outfitId != -1) {
            viewModelScope.launch {
                outfitRepository.getById(outfitId).collect { outfit ->
                    _uiState.update {
                        it.copy(outfit = outfit)
                    }
                }
            }
        }
    }

    private fun updateOutfitsForSelectedDate(date: LocalDate) {
        outfitsForDateJob?.cancel()
        val scheduledForDate = allScheduled.filter { scheduled ->
            scheduled.date?.let {
                Date(it).toInstant().atZone(ZoneId.systemDefault())
                    .toLocalDate() == date
            } ?: false
        }
        if (scheduledForDate.isEmpty()) {
            _uiState.update { it.copy(outfitsForSelectedDate = emptyList()) }
            return
        }
        val outfitIdsForDate = scheduledForDate.map { it.outfitId }
        outfitsForDateJob = viewModelScope.launch {
            outfitRepository.getOutfitsByIds(outfitIdsForDate).collect { outfits ->
                val scheduledOutfitPairs = scheduledForDate.mapNotNull { scheduled ->
                    outfits.find { it.id == scheduled.outfitId }?.let { outfit ->
                        scheduled to outfit
                    }
                }
                _uiState.update { it.copy(outfitsForSelectedDate = scheduledOutfitPairs) }
            }
        }
    }

    fun onEvent(event: CalendarEvent) {
        when (event) {
            is CalendarEvent.DateSelected -> {
                _uiState.update { it.copy(selectedDate = event.date) }
                updateOutfitsForSelectedDate(event.date)
            }
            is CalendarEvent.ScheduleOutfitForDate -> {
                viewModelScope.launch {
                    val scheduledOutfit = ScheduledOutfit(
                        outfitId = event.outfit.id,
                        date = uiState.value.selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        temperature = event.temperature
                    )
                    outfitRepository.insertScheduledOutfit(scheduledOutfit)
                    _uiState.update { it.copy(isOutfitSelectionDialogVisible = false) }
                    // No reload needed: the getAllScheduled() flow above re-emits automatically.
                }
            }
            is CalendarEvent.AddOutfitForDate -> {
                _uiState.update { it.copy(isOutfitSelectionDialogVisible = true) }
            }
            is CalendarEvent.AddItemForScheduledOutfit -> {
                _navigationEvent.value =
                    CalendarNavigationEvent.NavigateToEditScheduledOutfit(event.scheduledOutfitId)
            }
            is CalendarEvent.DismissOutfitSelectionDialog -> {
                _uiState.update { it.copy(isOutfitSelectionDialogVisible = false) }
            }
            CalendarEvent.ToggleView -> {
                _uiState.update {
                    val newView = if (it.calendarView == CalendarView.MONTH) CalendarView.WEEK else CalendarView.MONTH
                    it.copy(calendarView = newView)
                }
            }
        }
    }

    fun onNavigationEventConsumed() {
        _navigationEvent.value = null
    }
}
