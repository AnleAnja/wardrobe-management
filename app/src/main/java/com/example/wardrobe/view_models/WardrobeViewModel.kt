package com.example.wardrobe.view_models

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardrobe.view_models.NavigationEvent.NavigateToAddItem
import com.example.wardrobe.view_models.NavigationEvent.NavigateToItemDetail
import com.example.wardrobe.database.WardrobeItemRepository
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.WardrobeItem
import com.example.wardrobe.json_parser.WardrobeExporter
import com.example.wardrobe.json_parser.WardrobeImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class WardrobeFilters(
    val selectedSeasons: List<String> = emptyList(),
    val selectedCategories: List<String> = emptyList()
)

enum class WardrobeSortOption(val displayName: String) {
    MOST_WORN("Most Worn"),
    LEAST_WORN("Least Worn"),
    RECENTLY_WORN("Recently Worn"),
    LEAST_RECENTLY_WORN("Least Recently Worn"),
    RECENTLY_PURCHASED("Recently Purchased"),
    LEAST_RECENTLY_PURCHASED("Least Recently Purchased")
}

data class WardrobeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val wardrobeItems: List<WardrobeItem> = emptyList(),
    val currentSortOption: WardrobeSortOption = WardrobeSortOption.RECENTLY_WORN,
    val currentFilters: WardrobeFilters = WardrobeFilters(),
    val availableCategories: List<String> = listOf("Tops", "Bottoms", "Shoes", "Accessories"),
    val availableSeasons: List<String> = listOf("Spring", "Summer", "Fall", "Winter")
)

sealed class WardrobeScreenEvent {
    data class ImportJson(val jsonContent: String) : WardrobeScreenEvent()
    data class ExportJson(val context: Context, val uri: Uri) : WardrobeScreenEvent()
    data object AddItemClicked : WardrobeScreenEvent()
    data class ItemClicked(val item: WardrobeItem) : WardrobeScreenEvent()
    data object RefreshRequested : WardrobeScreenEvent()
    data class ApplyFilters(val filters: WardrobeFilters) : WardrobeScreenEvent()
    data class ApplySortOption(val sortOption: WardrobeSortOption) : WardrobeScreenEvent()
    object ClearFilters : WardrobeScreenEvent()
}

@HiltViewModel
class WardrobeViewModel @Inject constructor(
    private val repository: WardrobeItemRepository,
    private val exporter: WardrobeExporter,
    private val importer: WardrobeImporter
) : ViewModel() {
    private val _uiState = MutableStateFlow(WardrobeUiState())
    val uiState: StateFlow<WardrobeUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    private val allItemsFlow = repository.getAll()

    init {
        viewModelScope.launch {
            combine(allItemsFlow, _uiState) { allItems, state ->
                val filteredItems = applyFilters(allItems, state.currentFilters)
                val sortedItems = applySorting(filteredItems, state.currentSortOption)
                val availableCategories = allItems.mapNotNull { it.category }.distinct().sorted()
                state.copy(
                    wardrobeItems = sortedItems,
                    availableCategories = availableCategories
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

    private fun applyFilters(
        items: List<WardrobeItem>,
        filters: WardrobeFilters
    ): List<WardrobeItem> {
        var filteredList = items

        if (filters.selectedSeasons.isNotEmpty()) {
            filteredList = filteredList.filter { item ->
                val itemSeasons = item.seasons?.split(',')?.map { it.trim() } ?: emptyList()
                filters.selectedSeasons.any { selectedSeason -> itemSeasons.contains(selectedSeason) }
            }
        }

        if (filters.selectedCategories.isNotEmpty()) {
            filteredList = filteredList.filter { item ->
                item.category != null && filters.selectedCategories.contains(item.category)
            }
        }

        return filteredList
    }

    private fun applySorting(
        items: List<WardrobeItem>,
        sortOption: WardrobeSortOption?
    ): List<WardrobeItem> {
        return when (sortOption) {
            WardrobeSortOption.MOST_WORN -> items.sortedByDescending { it.timesWorn }
            WardrobeSortOption.LEAST_WORN -> items.sortedBy { it.timesWorn }
            WardrobeSortOption.RECENTLY_WORN -> items.sortedByDescending { it.lastWorn }
            WardrobeSortOption.LEAST_RECENTLY_WORN -> items.sortedBy { it.lastWorn }
            WardrobeSortOption.RECENTLY_PURCHASED -> items.sortedByDescending { it.purchaseDate }
            WardrobeSortOption.LEAST_RECENTLY_PURCHASED -> items.sortedBy { it.purchaseDate }
            null -> items
        }
    }

    private fun loadWardrobeItems() {
        Log.d("WardrobeViewModel", "Loading wardrobe items")
        viewModelScope.launch {
            _uiState.value = WardrobeUiState(isLoading = true)
            repository.getAll()
                .catch { e ->
                    _uiState.value = WardrobeUiState(
                        isLoading = false,
                        errorMessage = "Failed to load items: ${e.message}",
                        wardrobeItems = emptyList()
                    )
                }
                .collect { items ->
                    _uiState.value = WardrobeUiState(
                        isLoading = false,
                        wardrobeItems = items,
                        errorMessage = null
                    )
                }
        }
    }

    fun onEvent(event: WardrobeScreenEvent) {
        when (event) {
            is WardrobeScreenEvent.ImportJson -> {
                viewModelScope.launch {
                    val result = importer.importFromJson(event.jsonContent)
                    Log.d("WardrobeViewModel", "Import result: $result")
                    result.onFailure { e ->
                        Log.e("WardrobeViewModel", "Error importing JSON", e)
                        _uiState.update { it.copy(errorMessage = "Import failed: ${e.message}") }
                    }
                }
            }

            is WardrobeScreenEvent.ExportJson -> {
                try {
                    viewModelScope.launch {
                        val result =
                            exporter.exportToJson(event.context.applicationContext, event.uri)

                        result.onSuccess { message ->
                            Log.d("Export", message)
                        }.onFailure { e ->
                            Log.e("Export", "Error: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Export", "Error: ${e.message}")
                }
            }

            is WardrobeScreenEvent.AddItemClicked -> {
                _navigationEvent.value = NavigateToAddItem
            }

            is WardrobeScreenEvent.ItemClicked -> {
                _navigationEvent.value = NavigateToItemDetail(event.item)
            }

            is WardrobeScreenEvent.RefreshRequested -> {}
            is WardrobeScreenEvent.ApplyFilters -> {
                _uiState.update { it.copy(currentFilters = event.filters) }
            }

            is WardrobeScreenEvent.ApplySortOption -> {
                _uiState.update { it.copy(currentSortOption = event.sortOption) }
            }

            WardrobeScreenEvent.ClearFilters -> {
                _uiState.update { it.copy(currentFilters = WardrobeFilters()) }
            }
        }
    }

    // Reset the navigation event after it's consumed by the UI
    fun navigationEventConsumed() {
        _navigationEvent.value = null
    }

    fun deleteItem(itemId: Int) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
        }
    }

    // TODO: SortOption Typ nutzen (09.10.)

    fun sortItemsBy(sortOption: String) {
        val currentList = _uiState.value.wardrobeItems
        val sortedList = when (sortOption) {
            "Most Worn" -> currentList.sortedByDescending { it.timesWorn }
            "Least Worn" -> currentList.sortedBy { it.timesWorn }
            "Recently Worn" -> currentList.sortedByDescending { it.lastWorn }
            "Least Recently Worn" -> currentList.sortedBy { it.lastWorn }
            "Recently Purchased" -> currentList.sortedByDescending { it.purchaseDate }
            "Least Recently Purchased" -> currentList.sortedBy { it.purchaseDate }
            else -> currentList
        }
        _uiState.update { currentState ->
            currentState.copy(wardrobeItems = sortedList)
        }
    }
}

// Define sealed classes for navigation events
sealed class NavigationEvent {
    data object NavigateToAddItem : NavigationEvent()
    data class NavigateToItemDetail(val item: WardrobeItem) : NavigationEvent()
    data object NavigateToAddOutfit : NavigationEvent()
    data class NavigateToOutfitDetail(val outfit: Outfit) : NavigationEvent()
}