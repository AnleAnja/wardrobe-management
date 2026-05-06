package com.example.wardrobe.view_models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardrobe.database.WardrobeItemRepository
import com.example.wardrobe.database.entities.WardrobeItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ItemDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val item: WardrobeItem? = null,
    val daysSinceLastWear: Int? = null
)
@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    wardrobeItemRepository: WardrobeItemRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val itemId = checkNotNull(savedStateHandle.get<Int>("itemId"))
    val uiState: StateFlow<ItemDetailUiState> =
        wardrobeItemRepository.getById(itemId)
            .filterNotNull()
            .map { item ->
                ItemDetailUiState(isLoading = false, item = item)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = ItemDetailUiState(isLoading = true)
            )
}