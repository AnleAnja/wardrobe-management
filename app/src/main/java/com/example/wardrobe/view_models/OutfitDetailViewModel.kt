package com.example.wardrobe.view_models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardrobe.database.OutfitRepository
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.ScheduledOutfit
import com.example.wardrobe.database.entities.WardrobeItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.emptyList

data class OutfitDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val outfit: Outfit? = null,
    val scheduledOutfit: ScheduledOutfit? = null,
    val daysSinceLastWear: Int? = null,
    val itemsInOutfit: List<WardrobeItem> = emptyList(),
    val minTemp: Int? = null,
    val maxTemp: Int? = null,
    val isCalendarDialogVisible: Boolean = false
)

sealed class OutfitDetailEvent {
    data object AddScheduledOutfit : OutfitDetailEvent()
    data object DismissCalendarDialog : OutfitDetailEvent()
    data class ScheduleOutfitForDate(val date: LocalDate, val temperature: Int?) : OutfitDetailEvent()
}

@HiltViewModel
class OutfitDetailViewModel @Inject constructor(
    private val repository: OutfitRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val outfitId = savedStateHandle.get<Int?>("outfitId")
    private val scheduledOutfitId = savedStateHandle.get<Int?>("scheduledOutfitId")

    private val _uiState = MutableStateFlow(OutfitDetailUiState())

    private val effectiveOutfitIdFlow = when {
        outfitId != null -> repository.getById(outfitId)
        scheduledOutfitId != null -> repository.getOutfitByScheduledId(scheduledOutfitId)
        else -> throw IllegalStateException("Keine ID für OutfitDetailViewModel vorhanden")
    }.map { it?.id ?: -1 }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val outfitFlow = effectiveOutfitIdFlow.flatMapLatest { id ->
        if (id != -1) repository.getById(id) else flowOf(null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val itemsForOutfitFlow = effectiveOutfitIdFlow.flatMapLatest { id ->
        if (id == -1) flowOf(emptyList())
        else {
            val outfitItemsFlow = repository.getItemsForOutfit(id)
            val scheduledOutfitItemsFlow = if (scheduledOutfitId != null) repository.getItemsForScheduledOutfit(scheduledOutfitId) else flowOf(emptyList())
            combine(outfitItemsFlow, scheduledOutfitItemsFlow) { outfitItems, scheduledOutfitItems ->
                (outfitItems + scheduledOutfitItems).distinctBy { it.id }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allScheduledForOutfitFlow = effectiveOutfitIdFlow.flatMapLatest { id ->
        if (id != -1) repository.getAllScheduledForOutfit(id) else flowOf(emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scheduledOutfitFlow = if (scheduledOutfitId != null) {
        repository.getScheduledById(scheduledOutfitId)
    } else {
        flowOf(null)
    }

    val uiState: StateFlow<OutfitDetailUiState> =
        combine(outfitFlow, itemsForOutfitFlow, allScheduledForOutfitFlow, scheduledOutfitFlow, _uiState) { outfit, items, allScheduled, scheduledOutfit, uiState ->
            val daysSinceLastWear = outfit?.lastWorn?.let {
                val diffInMillis = System.currentTimeMillis() - it
                TimeUnit.MILLISECONDS.toDays(diffInMillis)
            }

            val temperatures = allScheduled.mapNotNull { it.temperature }
            val minTemp = temperatures.minOrNull()
            val maxTemp = temperatures.maxOrNull()

            OutfitDetailUiState(
                isLoading = false,
                outfit = outfit,
                scheduledOutfit = scheduledOutfit,
                daysSinceLastWear = daysSinceLastWear?.toInt(),
                itemsInOutfit = items,
                minTemp = minTemp,
                maxTemp = maxTemp,
                isCalendarDialogVisible = uiState.isCalendarDialogVisible
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = OutfitDetailUiState(isLoading = true)
            )

    fun onEvent(event: OutfitDetailEvent) {
        when (event) {
            is OutfitDetailEvent.AddScheduledOutfit -> {
                _uiState.update { it.copy(isCalendarDialogVisible = true) }
            }
            is OutfitDetailEvent.DismissCalendarDialog -> {
                _uiState.update { it.copy(isCalendarDialogVisible = false) }
            }
            is OutfitDetailEvent.ScheduleOutfitForDate -> {
                viewModelScope.launch {
                    val idToSchedule = outfitId ?: return@launch
                    val scheduledOutfit = ScheduledOutfit(
                        outfitId = idToSchedule,
                        date = event.date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                            .toEpochMilli(),
                        temperature = event.temperature
                    )
                    repository.insertScheduledOutfit(scheduledOutfit)
                    _uiState.update { it.copy(isCalendarDialogVisible = false) }
                }
            }
        }
    }

    fun deleteOutfit(outfitId: Int) {
        viewModelScope.launch {
            repository.deleteOutfit(outfitId)
        }
    }

}