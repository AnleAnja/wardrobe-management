package com.example.wardrobe.view_models

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardrobe.view_models.NavigationEvent.NavigateToAddItem
import com.example.wardrobe.view_models.NavigationEvent.NavigateToItemDetail
import com.example.wardrobe.database.WardrobeItemRepository
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.WardrobeItem
import com.example.wardrobe.filter_sort.WardrobeFilters
import com.example.wardrobe.filter_sort.WardrobeSortOption
import com.example.wardrobe.json_parser.WardrobeExporter
import com.example.wardrobe.json_parser.WardrobeImporter
import com.example.wardrobe.storage.ImageStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class WardrobeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val wardrobeItems: List<WardrobeItem> = emptyList(),
    val currentSortOption: WardrobeSortOption = WardrobeSortOption.RECENTLY_WORN,
    val currentFilters: WardrobeFilters = WardrobeFilters(),
    val availableCategories: Set<Pair<String, String>> = emptySet(),
    val availableSeasons: List<String> = listOf("Spring", "Summer", "Fall", "Winter")
)

sealed class WardrobeScreenEvent {
    // Pass the URI through; the VM reads the file off the main thread.
    data class ImportJson(val context: Context, val uri: Uri) : WardrobeScreenEvent()
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
    private val importer: WardrobeImporter,
    private val imageStorage: ImageStorage
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
                val availableCategories = allItems.mapNotNull { item ->
                    if (item.category != null && item.subcategory != null) {
                        Pair(item.category, item.subcategory)
                    } else {
                        null
                    }
                }.toSet()
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
                item.category != null && item.subcategory != null &&
                        Pair(item.category, item.subcategory) in filters.selectedCategories
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
            WardrobeSortOption.HIGHEST_RATING -> items.sortedByDescending { it.rating }
            null -> items
        }
    }

    fun onEvent(event: WardrobeScreenEvent) {
        when (event) {
            is WardrobeScreenEvent.ImportJson -> {
                viewModelScope.launch {
                    try {
                        val jsonContent = withContext(Dispatchers.IO) {
                            event.context.contentResolver.openInputStream(event.uri)?.use { input ->
                                input.bufferedReader().readText()
                            }
                        }
                        if (jsonContent.isNullOrBlank()) {
                            _uiState.update { it.copy(errorMessage = "Could not read file") }
                            return@launch
                        }
                        importer.importFromJson(jsonContent).onFailure { e ->
                            _uiState.update { it.copy(errorMessage = "Import failed: ${e.message}") }
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = "Import failed: ${e.message}") }
                    }
                }
            }

            is WardrobeScreenEvent.ExportJson -> {
                viewModelScope.launch {
                    val result =
                        exporter.exportToJson(event.context.applicationContext, event.uri)
                    result.onFailure { e ->
                        _uiState.update { it.copy(errorMessage = "Export failed: ${e.message}") }
                    }
                }
            }

            is WardrobeScreenEvent.AddItemClicked -> {
                _navigationEvent.value = NavigateToAddItem
            }

            is WardrobeScreenEvent.ItemClicked -> {
                _navigationEvent.value = NavigateToItemDetail(event.item)
            }

            is WardrobeScreenEvent.RefreshRequested -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
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
            val item = repository.getById(itemId).firstOrNull()
            repository.deleteItem(itemId)
            imageStorage.deleteImage(item?.imageUri)
        }
    }
}

// Define sealed classes for navigation events
sealed class NavigationEvent {
    data object NavigateToAddItem : NavigationEvent()
    data class NavigateToItemDetail(val item: WardrobeItem) : NavigationEvent()
    data object NavigateToAddOutfit : NavigationEvent()
    data class NavigateToOutfitDetail(val outfit: Outfit) : NavigationEvent()
    data class NavigateToScheduleOutfit(val outfit: Outfit) : NavigationEvent()
    data object NavigateBack: NavigationEvent()
}
