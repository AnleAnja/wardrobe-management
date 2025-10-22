package com.example.wardrobe.view_models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardrobe.database.OutfitRepository
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.WardrobeItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class OutfitDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val outfit: Outfit? = null,
    val daysSinceLastWear: Int? = null,
    val itemsInOutfit: List<WardrobeItem> = emptyList(),
    val minTemp: Int? = null,
    val maxTemp: Int? = null
)
@HiltViewModel
class OutfitDetailViewModel @Inject constructor(
    private val repository: OutfitRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val outfitId = checkNotNull(savedStateHandle.get<Int>("outfitId"))

    private val outfitFlow = repository.getById(outfitId)
    private val mostRecentScheduledFlow = repository.getMostRecentScheduledOutfit(outfitId)
    private val itemsForOutfitFlow = repository.getItemsForOutfit(outfitId)
    private val allScheduledForOutfitFlow = repository.getAllScheduledForOutfit(outfitId)

    val uiState: StateFlow<OutfitDetailUiState> =
        combine(outfitFlow, mostRecentScheduledFlow, itemsForOutfitFlow, allScheduledForOutfitFlow) { outfit, mostRecentScheduled, items, allScheduled ->
            val daysSinceLastWear = mostRecentScheduled?.date?.let { lastWornDate ->
                val diffInMillis = System.currentTimeMillis() - lastWornDate
                TimeUnit.MILLISECONDS.toDays(diffInMillis)
            }

            val temperatures = allScheduled.mapNotNull { it.temperature }
            val minTemp = temperatures.minOrNull()
            val maxTemp = temperatures.maxOrNull()

            OutfitDetailUiState(
                isLoading = false,
                outfit = outfit,
                daysSinceLastWear = daysSinceLastWear?.toInt(),
                itemsInOutfit = items,
                minTemp = minTemp,
                maxTemp = maxTemp
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = OutfitDetailUiState(isLoading = true)
            )

    fun deleteOutfit(outfitId: Int) {
        viewModelScope.launch {
            repository.deleteOutfit(outfitId)
        }
    }

}