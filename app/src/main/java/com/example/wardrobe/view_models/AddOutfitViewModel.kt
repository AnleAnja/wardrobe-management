package com.example.wardrobe.view_models

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardrobe.database.OutfitRepository
import com.example.wardrobe.database.WardrobeItemRepository
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.OutfitItem
import com.example.wardrobe.database.entities.ScheduledItem
import com.example.wardrobe.database.entities.WardrobeItem
import com.example.wardrobe.storage.ImageStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    val imageUriTeaser: String? = null,
    val imageUriCombined: String? = null,
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
    data class ImageUriTeaserChanged(val uri: Uri?) : AddOutfitEvent()
    data class ImageUriCombinedChanged(val uri: Uri?) : AddOutfitEvent()
    data class ItemsChanged(val itemId: Int) : AddOutfitEvent()
    data class SeasonsChanged(val seasons: String) : AddOutfitEvent()
    data class RatingChanged(val rating: Int) : AddOutfitEvent()
    data object SaveOutfit : AddOutfitEvent()
    data object ClearSuccess: AddOutfitEvent()
}

@HiltViewModel
class AddOutfitViewModel @Inject constructor(
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
    private var originalTeaserUri: String? = null
    private var originalCombinedUri: String? = null

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
                originalTeaserUri = it.imageUriTeaser
                originalCombinedUri = it.imageUriCombined
                _uiState.value = _uiState.value.copy(
                    outfitId = it.id,
                    imageUriTeaser = it.imageUriTeaser,
                    imageUriCombined = it.imageUriCombined,
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
                _uiState.update { it.copy(isLoading = false, errorMessage = "Outfit not found") }
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
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load items: ${e.message}") }
                }
                .collect { items ->
                    val groupedItems = items
                        .sortedBy { it.lastWorn ?: Long.MIN_VALUE }
                        .groupBy { it.category ?: "Uncategorized" }
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
            AddOutfitEvent.SaveOutfit -> {
                if (_uiState.value.selectedItemIds.isNotEmpty()) {
                    saveOutfit()
                }
            }
            AddOutfitEvent.ClearSuccess -> {
                _uiState.value = _uiState.value.copy(isSuccess = false)
            }
            is AddOutfitEvent.ImageUriTeaserChanged -> handleImagePicked(
                source = event.uri,
                currentProvider = { _uiState.value.imageUriTeaser },
                applyTo = { newUri -> _uiState.update { it.copy(imageUriTeaser = newUri) } }
            )
            is AddOutfitEvent.ImageUriCombinedChanged -> handleImagePicked(
                source = event.uri,
                currentProvider = { _uiState.value.imageUriCombined },
                applyTo = { newUri -> _uiState.update { it.copy(imageUriCombined = newUri) } }
            )
        }
    }

    private fun handleImagePicked(
        source: Uri?,
        currentProvider: () -> String?,
        applyTo: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val current = currentProvider()
            if (source == null) {
                if (current != null && current in sessionPaths) {
                    imageStorage.deleteImage(current)
                    sessionPaths.remove(current)
                }
                applyTo(null)
                return@launch
            }
            val newPath = imageStorage.saveImage(source)
            if (newPath == null) {
                _uiState.update { it.copy(errorMessage = "Could not save image") }
                return@launch
            }
            if (current != null && current in sessionPaths) {
                imageStorage.deleteImage(current)
                sessionPaths.remove(current)
            }
            sessionPaths.add(newPath)
            applyTo(newPath)
        }
    }

    private fun saveOutfit() {
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
                    val outfit = Outfit(
                        id = state.outfitId ?: 0,
                        imageUriTeaser = _uiState.value.imageUriTeaser,
                        imageUriCombined = _uiState.value.imageUriCombined,
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

                    // Save committed: drop the now-orphaned original local images.
                    if (originalTeaserUri != null && originalTeaserUri != state.imageUriTeaser) {
                        imageStorage.deleteImage(originalTeaserUri)
                    }
                    if (originalCombinedUri != null && originalCombinedUri != state.imageUriCombined) {
                        imageStorage.deleteImage(originalCombinedUri)
                    }
                    sessionPaths.clear()
                    saveCommitted = true
                }
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isLoading = false,
                    errorMessage = "Failed to save outfit: ${e.message}"
                )
            }
            // TODO: Implementierung der Bildkombination
            //
            //    Beispiel:
            //    val imageUris = selectedItems.mapNotNull { it.imageUri }
            //    val combinedImageUri = imageStitchingService.createCombinedOutfitImage(imageUris)
            //
            //    Du würdest dann `imageUriTeaser = combinedImageUri` setzen.
            //    Bis das implementiert ist, nutzen wir die einfache Variante.
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
}