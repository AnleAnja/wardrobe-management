package com.anleanja.wardrobe.view_models

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anleanja.wardrobe.database.OutfitRepository
import com.anleanja.wardrobe.database.WardrobeItemRepository
import com.anleanja.wardrobe.database.entities.Outfit
import com.anleanja.wardrobe.database.entities.OutfitItem
import com.anleanja.wardrobe.database.entities.ScheduledItem
import com.anleanja.wardrobe.database.entities.WardrobeItem
import com.anleanja.wardrobe.R
import com.anleanja.wardrobe.filter_sort.groupWardrobeItemsByCategoryRecentlyWorn
import com.anleanja.wardrobe.storage.ImageStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.toMutableSet

data class AddOutfitUiState(
    val outfitId: Int? = null,
    val imageUri: String? = null,
    val itemsByCategory: Map<String, List<WardrobeItem>> = emptyMap(),
    val selectedItemIds: Set<Int> = emptySet(),
    val seasons: String = "",
    val rating: Int = 0,
    val scheduledOutfitId: Int? = null,
    val lockedItemIds: Set<Int> = emptySet(),
    val isLoading: Boolean = true,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isScheduledOutfit: Boolean = false
    )

sealed class AddOutfitEvent {
    data object RemoveImage : AddOutfitEvent()
    data class ImageUriChanged(val uri: Uri?) : AddOutfitEvent()
    data class ItemsChanged(val itemId: Int) : AddOutfitEvent()
    data class SeasonsChanged(val seasons: String) : AddOutfitEvent()
    data class RatingChanged(val rating: Int) : AddOutfitEvent()
    data class SaveOutfit(val combinedImage: Bitmap? = null) : AddOutfitEvent()
    data object ClearSuccess: AddOutfitEvent()
}

@HiltViewModel
class AddOutfitViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val wardrobeItemRepository: WardrobeItemRepository,
    private val outfitRepository: OutfitRepository,
    private val imageStorage: ImageStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val seasons = listOf("Spring", "Summer", "Fall", "Winter")

    private val _uiState = MutableStateFlow(AddOutfitUiState())
    val uiState: StateFlow<AddOutfitUiState> = _uiState.asStateFlow()

    private val outfitId: Int? = savedStateHandle.get<Int>("outfitId")
    private val scheduledOutfitId: Int? = savedStateHandle.get<Int>("scheduledOutfitId")

    // Original images loaded from DB; only deleted once Save commits a replacement.
    private var originalImageUri: String? = null

    // Files we copied into filesDir during this session that aren't committed yet.
    private val sessionPaths = mutableSetOf<String>()
    private var saveCommitted = false

    init {
        when {
            scheduledOutfitId != null -> {
                _uiState.update {
                    it.copy(
                        scheduledOutfitId = scheduledOutfitId,
                        isScheduledOutfit = true,
                        isLoading = true
                    )
                }
                loadScheduledOutfitDetails(scheduledOutfitId)
            }

            outfitId != null -> {
                _uiState.update { it.copy(isLoading = true) }
                loadOutfitDetails(outfitId)
            }

            else -> {
                loadAllItems()
            }
        }
    }

    private fun loadOutfitDetails(id: Int) {
        viewModelScope.launch {
            val outfit = outfitRepository.getById(id).firstOrNull() ?: return@launch
            val itemsInOutfit = outfitRepository.getItemsForOutfit(outfit.id).first()
            outfit.let {
                originalImageUri = it.imageUriTeaser ?: it.imageUriCombined
                _uiState.value = _uiState.value.copy(
                    outfitId = it.id,
                    imageUri = it.imageUriTeaser ?: it.imageUriCombined,
                    seasons = it.seasons ?: "",
                    rating = it.rating ?: 0,
                    selectedItemIds = itemsInOutfit.map { it.id }.toSet(),
                    isLoading = true
                )
            }
            loadAllItems()
        }
    }

    private fun loadScheduledOutfitDetails(id: Int) {
        viewModelScope.launch {
            val outfit = outfitRepository.getOutfitByScheduledId(id).firstOrNull()
            if (outfit == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = appContext.getString(R.string.error_outfit_not_found)) }
                return@launch
            }
            val items = outfitRepository.getItemsForOutfit(outfit.id).first()
            val itemIds = items.map { it.id }.toSet()

            val scheduledItems = outfitRepository.getItemsForScheduledOutfit(id).first()
            val ids = scheduledItems.map { it.id }.toSet()

            val allSelectedIds = itemIds + ids

            _uiState.update {
                it.copy(
                    outfitId = outfit.id,
                    selectedItemIds = allSelectedIds,
                    lockedItemIds = itemIds,
                    isLoading = true
                )
            }
            loadAllItems()
        }
    }

    private fun loadAllItems() {
        viewModelScope.launch {
            if (_uiState.value.itemsByCategory.isEmpty()) {
                _uiState.update { it.copy(isLoading = true) }
            }
            wardrobeItemRepository.getAll()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = appContext.getString(R.string.error_load_items, e.message ?: "")) }
                }
                .collect { items ->
                    val groupedItems = groupWardrobeItemsByCategoryRecentlyWorn(items)
                    _uiState.update { it.copy(isLoading = false, itemsByCategory = groupedItems) }
                }
        }
    }

    fun onEvent(event: AddOutfitEvent) {
        when (event) {
            is AddOutfitEvent.ItemsChanged -> {
                _uiState.update { currentState ->
                    if (event.itemId in currentState.lockedItemIds) {
                        return@update currentState
                    }
                    val newSelection = currentState.selectedItemIds.toMutableSet()
                    if (event.itemId in newSelection) {
                        newSelection.remove(event.itemId)
                    } else {
                        newSelection.add(event.itemId)
                    }
                    currentState.copy(selectedItemIds = newSelection)
                }
            }
            is AddOutfitEvent.SeasonsChanged -> {
                _uiState.update { it.copy(seasons = event.seasons) }
            }
            is AddOutfitEvent.RatingChanged -> {
                _uiState.value = _uiState.value.copy(rating = event.rating)
            }
            is AddOutfitEvent.SaveOutfit -> {
                if (_uiState.value.selectedItemIds.isNotEmpty()) {
                    saveOutfit(event.combinedImage)
                }
            }
            AddOutfitEvent.ClearSuccess -> {
                _uiState.value = _uiState.value.copy(isSuccess = false)
            }
            AddOutfitEvent.RemoveImage -> {
                handleImagePicked(null)
            }
            is AddOutfitEvent.ImageUriChanged -> {
                handleImagePicked(event.uri)
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
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.error_could_not_save_image))
                }
                return@launch
            }
            if (current != null && current in sessionPaths) {
                imageStorage.deleteImage(current)
                sessionPaths.remove(current)
            }
            sessionPaths.add(newPath)
            _uiState.update { it.copy(imageUri = newPath) }
        }
    }

    private fun saveOutfit(combinedImage: Bitmap?) {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                _uiState.value = state.copy(isLoading = true, errorMessage = null)
                if (_uiState.value.isScheduledOutfit && scheduledOutfitId != null) {
                    val originalItemIds = state.lockedItemIds
                    val currentSelection = state.selectedItemIds

                    val itemsToAdd = currentSelection - originalItemIds

                    outfitRepository.replaceScheduledItems(scheduledOutfitId, itemsToAdd.toList())
                } else {
                    val teaserUri = if (combinedImage != null) {
                        val saved = imageStorage.saveBitmap(combinedImage)
                        if (saved == null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = appContext.getString(R.string.error_could_not_save_image)
                                )
                            }
                            return@launch
                        }
                        sessionPaths.add(saved)
                        saved
                    } else {
                        state.imageUri
                    }
                    val outfit = Outfit(
                        id = state.outfitId ?: 0,
                        imageUriTeaser = teaserUri,
                        imageUriCombined = null,
                        seasons = _uiState.value.seasons,
                        rating = state.rating.takeIf { it > 0 },
                    )
                    val outfitId = if (outfit.id != 0) {
                        outfitRepository.updateOutfit(outfit)
                        outfit.id
                    } else {
                        outfitRepository.insertOutfit(outfit).toInt()
                    }
                    outfitRepository.replaceOutfitItems(outfitId, state.selectedItemIds)

                    if (originalImageUri != null && originalImageUri != teaserUri) {
                        imageStorage.deleteImage(originalImageUri)
                    }
                    _uiState.update { it.copy(imageUri = teaserUri) }
                    sessionPaths.clear()
                    saveCommitted = true
                }
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isLoading = false,
                    errorMessage = appContext.getString(R.string.error_save_outfit_failed, e.message ?: "")
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
        toClean.forEach { imageStorage.deleteImage(it) }
    }
}