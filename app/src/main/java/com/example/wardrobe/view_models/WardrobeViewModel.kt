package com.example.wardrobe.view_models

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wardrobe.database.WardrobeItemRepository
import com.example.wardrobe.database.entities.WardrobeItem
import com.example.wardrobe.filter_sort.WardrobeFilters
import com.example.wardrobe.filter_sort.WardrobeSortOption
import com.example.wardrobe.filter_sort.extractAvailableCategories
import com.example.wardrobe.filter_sort.filterWardrobeItems
import com.example.wardrobe.filter_sort.sortWardrobeItems
import com.example.wardrobe.R
import com.example.wardrobe.json_parser.WardrobeExporter
import com.example.wardrobe.json_parser.WardrobeImporter
import com.example.wardrobe.navigation.NavigationEvent
import com.example.wardrobe.storage.ImageStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val appContext: Context,
    private val repository: WardrobeItemRepository,
    private val exporter: WardrobeExporter,
    private val importer: WardrobeImporter,
    private val imageStorage: ImageStorage
) : ViewModel() {
    private val _uiState = MutableStateFlow(WardrobeUiState())
    val uiState: StateFlow<WardrobeUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    private val _sortOption = MutableStateFlow(WardrobeSortOption.RECENTLY_WORN)
    private val _filters = MutableStateFlow(WardrobeFilters())

    private val allItemsFlow = repository.getAll()

    init {
        viewModelScope.launch {
            combine(allItemsFlow, _sortOption, _filters) { allItems, sort, filters ->
                val filteredItems = filterWardrobeItems(allItems, filters)
                val sortedItems = sortWardrobeItems(filteredItems, sort)
                Triple(sortedItems, extractAvailableCategories(allItems), sort to filters)
            }.catch { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = appContext.getString(R.string.error_load_items, e.message ?: "")
                    )
                }
            }.collect { (sortedItems, availableCategories, sortAndFilters) ->
                val (sort, filters) = sortAndFilters
                _uiState.update {
                    it.copy(
                        wardrobeItems = sortedItems,
                        availableCategories = availableCategories,
                        currentSortOption = sort,
                        currentFilters = filters,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
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
                            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.error_could_not_read_file)) }
                            return@launch
                        }
                        importer.importFromJson(jsonContent).onFailure { e ->
                            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.error_import_failed, e.message ?: "")) }
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = appContext.getString(R.string.error_import_failed, e.message ?: "")) }
                    }
                }
            }

            is WardrobeScreenEvent.ExportJson -> {
                viewModelScope.launch {
                    val result =
                        exporter.exportToJson(event.context.applicationContext, event.uri)
                    result.onFailure { e ->
                        _uiState.update { it.copy(errorMessage = appContext.getString(R.string.error_export_failed, e.message ?: "")) }
                    }
                }
            }

            is WardrobeScreenEvent.AddItemClicked -> {
                _navigationEvent.value = NavigationEvent.NavigateToAddItem
            }

            is WardrobeScreenEvent.ItemClicked -> {
                _navigationEvent.value = NavigationEvent.NavigateToItemDetail(event.item)
            }

            is WardrobeScreenEvent.RefreshRequested -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
            is WardrobeScreenEvent.ApplyFilters -> {
                _filters.update { event.filters }
            }

            is WardrobeScreenEvent.ApplySortOption -> {
                _sortOption.update { event.sortOption }
            }

            WardrobeScreenEvent.ClearFilters -> {
                _filters.update { WardrobeFilters() }
            }
        }
    }

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
