package com.example.wardrobe.view_models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardrobe.database.OutfitRepository
import com.example.wardrobe.database.entities.Outfit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OutfitFilters(
    val selectedSeasons: List<String> = emptyList()
)

enum class OutfitSortOption(val displayName: String) {
    MOST_WORN("Most Worn"),
    LEAST_WORN("Least Worn"),
    RECENTLY_WORN("Recently Worn"),
    LEAST_RECENTLY_WORN("Least Recently Worn")
}

data class OutfitUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val outfits: List<Outfit> = emptyList(),
    val currentSortOption: OutfitSortOption = OutfitSortOption.RECENTLY_WORN,
    val currentFilters: OutfitFilters = OutfitFilters(),
    val availableSeasons: List<String> = listOf("Spring", "Summer", "Fall", "Winter")
)

sealed class OutfitScreenEvent {
    data object AddOutfitClicked : OutfitScreenEvent()
    data class OutfitClicked(val outfit: Outfit) : OutfitScreenEvent()
    data object RefreshRequested : OutfitScreenEvent()
    data class ApplyFilters(val filters: OutfitFilters) : OutfitScreenEvent()
    data class ApplySortOption(val sortOption: OutfitSortOption) : OutfitScreenEvent()
    object ClearFilters : OutfitScreenEvent()
}

@HiltViewModel
class OutfitsViewModel @Inject constructor(
    private val repository: OutfitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OutfitUiState())
    val uiState: StateFlow<OutfitUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    private val allOutfitsFlow = repository.getAll()

    init {
        viewModelScope.launch {
            viewModelScope.launch {
                combine(allOutfitsFlow, _uiState) { allOutfits, state ->
                    val filteredOutfits = applyFilters(allOutfits, state.currentFilters)
                    val sortedOutfits = applySorting(filteredOutfits, state.currentSortOption)
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

    private fun applyFilters(
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
            null -> outfits
        }
    }

    fun onEvent(event: OutfitScreenEvent) {
        when(event) {
            OutfitScreenEvent.AddOutfitClicked -> {
                _navigationEvent.value = NavigationEvent.NavigateToAddOutfit
            }

            is OutfitScreenEvent.ApplyFilters -> TODO()
            is OutfitScreenEvent.ApplySortOption -> TODO()
            OutfitScreenEvent.ClearFilters -> TODO()
            is OutfitScreenEvent.OutfitClicked -> {
                _navigationEvent.value = NavigationEvent.NavigateToOutfitDetail(event.outfit)
            }
            OutfitScreenEvent.RefreshRequested -> TODO()
        }
    }

    fun navigationEventConsumed() {
        _navigationEvent.value = null
    }
}
