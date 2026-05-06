package com.example.wardrobe.view_models

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardrobe.database.OutfitRepository
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.ScheduledOutfit
import com.example.wardrobe.filter_sort.OutfitFilters
import com.example.wardrobe.filter_sort.OutfitSortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class OutfitUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val outfits: List<Outfit> = emptyList(),
    val currentSortOption: OutfitSortOption = OutfitSortOption.RECENTLY_WORN,
    val currentFilters: OutfitFilters = OutfitFilters(),
    val availableSeasons: List<String> = listOf("Spring", "Summer", "Fall", "Winter"),
    val availableTemperature: Int? = null,
    val isSelectionMode: Boolean = false,
    val dateToSchedule: LocalDate? = null
)

sealed class OutfitScreenEvent {
    data object AddOutfitClicked : OutfitScreenEvent()
    data class OutfitClicked(val outfit: Outfit) : OutfitScreenEvent()
    data object RefreshRequested : OutfitScreenEvent()
    data class ApplyFilters(val filters: OutfitFilters) : OutfitScreenEvent()
    data class ApplySortOption(val sortOption: OutfitSortOption) : OutfitScreenEvent()
    data class OutfitSelectedForDate(val outfit: Outfit) : OutfitScreenEvent()
    object ClearFilters : OutfitScreenEvent()
}

@HiltViewModel
class OutfitsViewModel @Inject constructor(
    private val repository: OutfitRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val dateToScheduleEpochDay: Long? = savedStateHandle.get<Long>("date")
    private val dateToSchedule: LocalDate? = dateToScheduleEpochDay?.takeIf { it != -1L }?.let { LocalDate.ofEpochDay(it) }

    private val _uiState = MutableStateFlow(OutfitUiState())
    val uiState: StateFlow<OutfitUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    private val allOutfitsFlow = repository.getAll()
    private val allScheduledOutfitsFlow = repository.getAllScheduled()

    init {
        if (dateToSchedule != null) {
            _uiState.update { it.copy(
                isSelectionMode = true,
                dateToSchedule = dateToSchedule
            ) }
        }

        viewModelScope.launch {
            viewModelScope.launch {
                combine(allOutfitsFlow, allScheduledOutfitsFlow, _uiState) { allOutfits, allScheduled, state ->
                    val filteredByTemp = applyTemperatureFilter(allOutfits, allScheduled, state.currentFilters.temperature)
                    val filteredBySeason = applySeasonFilter(filteredByTemp, state.currentFilters)
                    val sortedOutfits = applySorting(filteredBySeason, state.currentSortOption)
                    state.copy(
                        outfits = sortedOutfits
                    )
                }.catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load items: ${e.message}"
                        )
                    }
                }.collect { newState ->
                    _uiState.update {
                        newState.copy(isLoading = false, errorMessage = null)
                    }
                }
            }
        }
    }

    private fun applyTemperatureFilter(
        outfits: List<Outfit>,
        scheduledOutfits: List<ScheduledOutfit>,
        temperature: Int?
    ): List<Outfit> {
        if (temperature == null) return outfits
        val outfitTempRanges = scheduledOutfits
            .groupBy { it.outfitId }
            .mapValues { (_, scheduledList) ->
                val temps = scheduledList.mapNotNull { it.temperature }
                if (temps.isEmpty()) null else temps.minOrNull() to temps.maxOrNull()
            }

        return outfits.filter { outfit ->
            val tempRange = outfitTempRanges[outfit.id]
            if (tempRange != null && tempRange.first != null && tempRange.second != null) {
                temperature in tempRange.first!!..tempRange.second!!
            } else {
                false
            }
        }
    }

    private fun applySeasonFilter(
        outfits: List<Outfit>,
        filters: OutfitFilters
    ): List<Outfit> {
        var filteredList = outfits

        if (filters.selectedSeasons.isNotEmpty()) {
            filteredList = filteredList.filter { outfit ->
                val outfitsSeasons = outfit.seasons?.split(',')?.map { it.trim() } ?: emptyList()
                filters.selectedSeasons.any { selectedSeason -> outfitsSeasons.contains(selectedSeason) }
            }
        }

        return filteredList
    }

    private fun applySorting(
        outfits: List<Outfit>,
        sortOption: OutfitSortOption?
    ): List<Outfit> {
        return when (sortOption) {
            OutfitSortOption.MOST_WORN -> outfits.sortedByDescending { it.timesWorn }
            OutfitSortOption.LEAST_WORN -> outfits.sortedBy { it.timesWorn }
            OutfitSortOption.RECENTLY_WORN -> outfits.sortedByDescending { it.lastWorn }
            OutfitSortOption.LEAST_RECENTLY_WORN -> outfits.sortedBy { it.lastWorn }
            OutfitSortOption.HIGHEST_RATING -> outfits.sortedByDescending { it.rating }
            null -> outfits
        }
    }

    fun onEvent(event: OutfitScreenEvent) {
        when(event) {
            OutfitScreenEvent.AddOutfitClicked -> {
                _navigationEvent.value = NavigationEvent.NavigateToAddOutfit
            }
            is OutfitScreenEvent.ApplyFilters -> {
                _uiState.update { it.copy(currentFilters = event.filters) }
            }
            is OutfitScreenEvent.ApplySortOption -> {
                _uiState.update { it.copy(currentSortOption = event.sortOption) }
            }
            is OutfitScreenEvent.ClearFilters -> {
                _uiState.update { it.copy(currentFilters = OutfitFilters()) }
            }
            is OutfitScreenEvent.OutfitClicked -> {
                _navigationEvent.value = NavigationEvent.NavigateToOutfitDetail(event.outfit)
            }
            is OutfitScreenEvent.OutfitSelectedForDate -> {
                viewModelScope.launch {
                    val date = _uiState.value.dateToSchedule
                    if (date != null) {
                        val scheduledOutfit = ScheduledOutfit(
                            outfitId = event.outfit.id,
                            date = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                                .toEpochMilli(),
                            temperature = null
                        )
                        Log.d("OutfitViewModel", "Inserting scheduled outfit: $scheduledOutfit at Date $date")
                        repository.insertScheduledOutfit(scheduledOutfit)
                        _navigationEvent.value = NavigationEvent.NavigateBack
                    }
                }
            }
            OutfitScreenEvent.RefreshRequested -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun navigationEventConsumed() {
        _navigationEvent.value = null
    }

}
