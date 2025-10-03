package com.example.wardrobe

import androidx.compose.animation.core.copy
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardrobe.NavigationEvent.*
import com.example.wardrobe.database.entities.WardrobeItem
import com.example.wardrobe.database.WardrobeItemRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class SortOption(val displayName: String) {
    NONE("None"),
    MOST_WORN("Most Worn"),
    LEAST_WORN("Least Worn"),
    RECENTLY_WORN("Recently Worn"),
    LEAST_RECENTLY_WORN("Least Recently Worn"),
    RECENTLY_ADDED("Recently Added (Newest First)"),
    LEAST_RECENTLY_ADDED("Least Recently Added (Oldest First)"),
    RECENTLY_PURCHASED("Recently Purchased"),
    LEAST_RECENTLY_PURCHASED("Least Recently Purchased"),
    PRICE_HIGH_TO_LOW("Price: High to Low"),
    PRICE_LOW_TO_HIGH("Price: Low to High")
}

data class WardrobeFilters(
    val selectedSeasons: List<String> = emptyList(),
    val selectedCategories: List<String> = emptyList()
)

data class WardrobeGalleryUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val wardrobeItems: List<WardrobeItem> = emptyList(),
    /*val allWardrobeItems: List<WardrobeItem> = emptyList(),
    val displayedWardrobeItems: List<WardrobeItem> = emptyList(),
    val currentSortOption: SortOption = SortOption.RECENTLY_ADDED,
    val currentFilters: WardrobeFilters = WardrobeFilters(),
    val availableCategories: List<String> = emptyList(),
    val availableSeasons: List<String> = listOf("Spring", "Summer", "Fall", "Winter")*/
)

sealed class WardrobeScreenEvent {
    data object AddItemClicked : WardrobeScreenEvent()
    data class ItemClicked(val item: WardrobeItem) : WardrobeScreenEvent()
    data object RefreshRequested : WardrobeScreenEvent()
    /*data class ApplyFilters(val filters: WardrobeFilters) : WardrobeScreenEvent()
    data class ApplySortOption(val sortOption: SortOption) : WardrobeScreenEvent()
    object OpenFilterDialog : WardrobeScreenEvent()
    object OpenSortDialog : WardrobeScreenEvent()
    object ClearFilters : WardrobeScreenEvent()*/
}

@HiltViewModel
class WardrobeViewModel @Inject constructor(
    private val wardrobeItemRepository: WardrobeItemRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WardrobeGalleryUiState())
    val uiState: StateFlow<WardrobeGalleryUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadWardrobeItems()
        /*viewModelScope.launch {
            val categories = wardrobeItemRepository.getAllCategories().firstOrNull() ?: emptyList()
            _uiState.update { it.copy(availableCategories = categories) }
        }*/
    }

    private fun loadWardrobeItems() {
        viewModelScope.launch {
            _uiState.value = WardrobeGalleryUiState(isLoading = true) // Show loading
            wardrobeItemRepository.getAll() // Assuming this method returns Flow<List<WardrobeItem>>
                .catch { e ->
                    // Handle potential errors during data fetching
                    _uiState.value = WardrobeGalleryUiState(
                        isLoading = false,
                        errorMessage = "Failed to load items: ${e.message}",
                        wardrobeItems = emptyList()
                    )
                }
                .collect { items ->
                    // Update the UI state with the fetched items
                    _uiState.value = WardrobeGalleryUiState(
                        isLoading = false,
                        wardrobeItems = items,
                        errorMessage = null // Clear any previous error
                    )
                }
        }
    }

    fun onEvent(event: WardrobeScreenEvent) {
        when (event) {
            WardrobeScreenEvent.AddItemClicked -> {
                _navigationEvent.value = NavigateToAddItem
            }
            is WardrobeScreenEvent.ItemClicked -> {
                // Trigger navigation to the item detail screen
                _navigationEvent.value = NavigateToItemDetail(event.item)
            }
            WardrobeScreenEvent.RefreshRequested -> {
                // Reload items (if your repo supports manual refresh, otherwise re-collecting Flow is often enough)
                loadWardrobeItems()
            }

            /*is WardrobeScreenEvent.ApplyFilters ->
            is WardrobeScreenEvent.ApplySortOption ->
            WardrobeScreenEvent.ClearFilters ->
            WardrobeScreenEvent.OpenFilterDialog ->
            WardrobeScreenEvent.OpenSortDialog ->*/
        }
    }

    // Reset the navigation event after it's consumed by the UI
    fun navigationEventConsumed() {
        _navigationEvent.value = null
    }
}

// Define sealed classes for navigation events
sealed class NavigationEvent {
    data object NavigateToAddItem : NavigationEvent()
    data class NavigateToItemDetail(val item: WardrobeItem) : NavigationEvent()
}