package com.example.wardrobe.view_models

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardrobe.database.OutfitRepository
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.ScheduledOutfit
import com.example.wardrobe.filter_sort.OutfitFilters
import com.example.wardrobe.filter_sort.OutfitSortOption
import com.example.wardrobe.filter_sort.filterOutfitsBySeason
import com.example.wardrobe.filter_sort.filterOutfitsByTemperature
import com.example.wardrobe.filter_sort.sortOutfits
import com.example.wardrobe.R
import com.example.wardrobe.navigation.NavigationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val appContext: Context,
    private val repository: OutfitRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val dateToScheduleEpochDay: Long? = savedStateHandle.get<Long>("date")
    private val dateToSchedule: LocalDate? = dateToScheduleEpochDay?.takeIf { it != -1L }?.let { LocalDate.ofEpochDay(it) }

    private val _uiState = MutableStateFlow(OutfitUiState())
    val uiState: StateFlow<OutfitUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    private val _sortOption = MutableStateFlow(OutfitSortOption.RECENTLY_WORN)
    private val _filters = MutableStateFlow(OutfitFilters())

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
            combine(allOutfitsFlow, allScheduledOutfitsFlow, _sortOption, _filters) { allOutfits, allScheduled, sort, filters ->
                val filteredByTemp = filterOutfitsByTemperature(allOutfits, allScheduled, filters.temperature)
                val filteredBySeason = filterOutfitsBySeason(filteredByTemp, filters)
                sortOutfits(filteredBySeason, sort) to (sort to filters)
            }.catch { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = appContext.getString(R.string.error_load_items, e.message ?: "")
                    )
                }
            }.collect { (sortedOutfits, sortAndFilters) ->
                val (sort, filters) = sortAndFilters
                _uiState.update {
                    it.copy(
                        outfits = sortedOutfits,
                        currentSortOption = sort,
                        currentFilters = filters,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun onEvent(event: OutfitScreenEvent) {
        when(event) {
            OutfitScreenEvent.AddOutfitClicked -> {
                _navigationEvent.value = NavigationEvent.NavigateToAddOutfit
            }
            is OutfitScreenEvent.ApplyFilters -> {
                _filters.update { event.filters }
            }
            is OutfitScreenEvent.ApplySortOption -> {
                _sortOption.update { event.sortOption }
            }
            is OutfitScreenEvent.ClearFilters -> {
                _filters.update { OutfitFilters() }
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
