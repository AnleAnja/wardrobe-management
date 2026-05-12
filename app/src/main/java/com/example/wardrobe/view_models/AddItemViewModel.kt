package com.example.wardrobe.view_models

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardrobe.database.WardrobeItemRepository
import com.example.wardrobe.database.entities.WardrobeItem
import com.example.wardrobe.storage.ImageStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    /** Pass a non-null Uri when the user picked an image, null when they cleared it. */
    data class ImageUriChanged(val uri: Uri?) : AddItemEvent()
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
    private val imageStorage: ImageStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val seasons = listOf("Spring", "Summer", "Fall", "Winter")
    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    private val itemId: Int? = savedStateHandle.get<Int>("itemId")

    // Image originally loaded from DB; only deleted once Save commits a replacement.
    private var originalImageUri: String? = null

    // Files we copied into filesDir during this editing session that haven't been
    // committed yet. Used to clean up freshly-picked images on cancel/replace.
    private val sessionPaths = mutableSetOf<String>()
    private var saveCommitted = false

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
                originalImageUri = it.imageUri
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
            is AddItemEvent.ImageUriChanged -> handleImagePicked(event.uri)
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

    private fun handleImagePicked(source: Uri?) {
        viewModelScope.launch {
            val current = _uiState.value.imageUri
            if (source == null) {
                if (current != null && current in sessionPaths) {
                    imageStorage.deleteImage(current)
                    sessionPaths.remove(current)
                }
                _uiState.update { it.copy(imageUri = null) }
                return@launch
            }
            val newPath = imageStorage.saveImage(source)
            if (newPath == null) {
                _uiState.update { it.copy(errorMessage = "Could not save image") }
                return@launch
            }
            // Replacing an uncommitted session image: drop the old copy.
            if (current != null && current in sessionPaths) {
                imageStorage.deleteImage(current)
                sessionPaths.remove(current)
            }
            sessionPaths.add(newPath)
            _uiState.update { it.copy(imageUri = newPath) }
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
                    imageUri = state.imageUri,
                    category = state.category,
                    subcategory = state.subcategory,
                    rating = state.rating.takeIf { it > 0 },
                    price = price,
                    purchaseDate = state.purchaseDate,
                    seasons = state.seasons.takeIf { it.isNotBlank() }
                )

                if (itemId != null && itemId != 0) repository.updateItem(item) else repository.insertItem(item)

                // Save committed: if we replaced or cleared the original local image, delete it.
                val original = originalImageUri
                if (original != null && original != state.imageUri) {
                    imageStorage.deleteImage(original)
                }
                sessionPaths.clear()
                saveCommitted = true

                _uiState.value = AddItemUiState(isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isLoading = false,
                    errorMessage = "Failed to save item: ${e.message}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (saveCommitted) return
        // User cancelled — drop any freshly-copied images that never got persisted.
        val toClean = sessionPaths.toList()
        sessionPaths.clear()
        if (toClean.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                toClean.forEach { imageStorage.deleteImage(it) }
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
