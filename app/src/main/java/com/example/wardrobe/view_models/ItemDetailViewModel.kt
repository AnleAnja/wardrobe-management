package com.example.wardrobe.view_models

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardrobe.database.OutfitRepository
import com.example.wardrobe.database.WardrobeItemRepository
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.WardrobeItem
import com.example.wardrobe.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ItemDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val item: WardrobeItem? = null,
    val daysSinceLastWear: Int? = null,
    val outfits: List<Outfit> = emptyList()
)
@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    wardrobeItemRepository: WardrobeItemRepository,
    outfitRepository: OutfitRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val itemId = checkNotNull(savedStateHandle.get<Int>("itemId"))
    val uiState: StateFlow<ItemDetailUiState> =
        combine(
            wardrobeItemRepository.getById(itemId),
            outfitRepository.getOutfitsForItem(itemId)
        ) { item, outfits ->
            if (item == null) {
                ItemDetailUiState(
                    isLoading = false,
                    errorMessage = appContext.getString(R.string.error_item_not_found),
                    item = null,
                    outfits = emptyList()
                )
            } else {
                val daysSinceLastWear = item.lastWorn?.let {
                    TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it).toInt()
                }
                ItemDetailUiState(
                    isLoading = false,
                    item = item,
                    daysSinceLastWear = daysSinceLastWear,
                    outfits = outfits.sortedByDescending { it.lastWorn }
                )
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = ItemDetailUiState(isLoading = true)
            )
}
