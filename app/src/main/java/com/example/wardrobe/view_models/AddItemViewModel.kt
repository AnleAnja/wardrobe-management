package com.example.wardrobe.view_models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardrobe.database.WardrobeItemRepository
import com.example.wardrobe.database.entities.WardrobeItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddItemUiState(
    val itemId: Int? = null,
    val imageUri: String? = null,
    val purchaseDate: Long? = null,
    val category: String = "",
    val subcategory: String = "",
    val rating: Int = 0,
    val price: String = "",
    val seasons: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

sealed class AddItemEvent {
    data class ImageUriChanged(val uri: String?) : AddItemEvent()
    data class PurchaseDateChanged(val date: Long) : AddItemEvent()
    data class CategoryChanged(val category: String, val subcategory: String) : AddItemEvent()
    data object ClearCategory : AddItemEvent()
    data class RatingChanged(val rating: Int) : AddItemEvent()
    data class PriceChanged(val price: String) : AddItemEvent()
    data class SeasonsChanged(val seasons: String) : AddItemEvent()
    data object SaveItem : AddItemEvent()
    data object ClearSuccess : AddItemEvent()
}

@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val repository: WardrobeItemRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val seasons = listOf("Spring", "Summer", "Fall", "Winter")
    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    private val itemId: Int? = savedStateHandle.get<Int>("itemId")

    init {
        if (itemId != null) {
            loadItemDetails(itemId)
        } else {
            _uiState.value = _uiState.value.copy(purchaseDate = System.currentTimeMillis())
        }
    }

    private fun loadItemDetails(id: Int) {
        viewModelScope.launch {
            val item = repository.getById(id).firstOrNull()
            item?.let {
                _uiState.value = _uiState.value.copy(
                    itemId = it.id,
                    imageUri = it.imageUri,
                    purchaseDate = it.purchaseDate,
                    category = it.category ?: "",
                    subcategory = it.subcategory ?: "",
                    rating = it.rating ?: 0,
                    price = it.price?.toString() ?: "",
                    seasons = it.seasons ?: ""
                )
            }
        }
    }

    fun onEvent(event: AddItemEvent) {
        when (event) {
            is AddItemEvent.ImageUriChanged -> {
                _uiState.value = _uiState.value.copy(imageUri = event.uri)
            }
            is AddItemEvent.PurchaseDateChanged -> {
                _uiState.value = _uiState.value.copy(purchaseDate = event.date)
            }
            is AddItemEvent.CategoryChanged -> {
                _uiState.value = _uiState.value.copy(
                    category = event.category,
                    subcategory = event.subcategory
                )
            }
            is AddItemEvent.ClearCategory -> {
                _uiState.value = _uiState.value.copy(
                    category = "",
                    subcategory = ""
                )
            }
            is AddItemEvent.RatingChanged -> {
                _uiState.value = _uiState.value.copy(rating = event.rating)
            }
            is AddItemEvent.PriceChanged -> {
                _uiState.value = _uiState.value.copy(price = event.price)
            }
            is AddItemEvent.SeasonsChanged -> {
                _uiState.value = _uiState.value.copy(seasons = event.seasons)
            }
            AddItemEvent.SaveItem -> {
                saveItem()
            }
            AddItemEvent.ClearSuccess -> {
                _uiState.value = _uiState.value.copy(isSuccess = false)
            }
        }
    }

    private fun saveItem() {
        val state = _uiState.value

        if (state.category.isBlank() || state.subcategory.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Category is required")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = state.copy(isLoading = true, errorMessage = null)

                val price = if (state.price.isBlank()) null else state.price.toDoubleOrNull()

                val item = WardrobeItem(
                    id = state.itemId ?: 0,
                    imageUri = state.imageUri.takeIf { it != null },
                    category = state.category,
                    subcategory = state.subcategory,
                    rating = state.rating.takeIf { it > 0 },
                    price = price,
                    purchaseDate = state.purchaseDate,
                    seasons = state.seasons.takeIf { it.isNotBlank() }
                )

                if(itemId != 0 && itemId != null) repository.updateItem(item) else repository.insertItem(item)

                _uiState.value = AddItemUiState(isSuccess = true)

            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isLoading = false,
                    errorMessage = "Failed to save item: ${e.message}"
                )
            }
        }
    }

    fun updateSelectedSeasons(selected: List<String>) {
        val seasons = selected.joinToString(", ")
        _uiState.update { it.copy(seasons = seasons) }
    }

    fun getSelectedSeasons(): List<String> {
        return _uiState.value.seasons.split(", ").filter { it.isNotBlank() }
    }
}