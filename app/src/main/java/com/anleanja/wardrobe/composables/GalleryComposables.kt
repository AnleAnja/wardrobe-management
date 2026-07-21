package com.anleanja.wardrobe.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anleanja.wardrobe.composables.ModernEmptyState
import com.anleanja.wardrobe.composables.ModernErrorState
import com.anleanja.wardrobe.composables.ModernLoadingState
import com.anleanja.wardrobe.composables.ModernOutfitCard
import com.anleanja.wardrobe.composables.ModernWardrobeItemCard
import com.anleanja.wardrobe.filter_sort.FilterChips
import com.anleanja.wardrobe.R
import com.anleanja.wardrobe.view_models.OutfitUiState
import com.anleanja.wardrobe.view_models.WardrobeUiState

@Composable
fun WardrobeGalleryModernContent(
    uiState: WardrobeUiState,
    onSortClick: () -> Unit,
    onFilterClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onItemClick: (com.anleanja.wardrobe.database.entities.WardrobeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        GallerySummaryHeader(
            title = "${uiState.wardrobeItems.size} wardrobe pieces",
            subtitle = wardrobeSummary(uiState)
        )
        FilterChips(
            onSortClick = onSortClick,
            onFilterClick = onFilterClick,
            activeFilterCount = uiState.currentFilters.selectedCategories.size + uiState.currentFilters.selectedSeasons.size
        )
        when {
            uiState.isLoading -> ModernLoadingState()
            uiState.errorMessage != null -> ModernErrorState(
                message = uiState.errorMessage,
                onRetry = onRefreshClick
            )
            uiState.wardrobeItems.isEmpty() -> ModernEmptyState(
                title = stringResource(R.string.empty_no_wardrobe_items),
                body = "Add a first item or clear filters to see your closet."
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(
                    items = uiState.wardrobeItems,
                    key = { item -> item.id }
                ) { item ->
                    ModernWardrobeItemCard(
                        item = item,
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun OutfitGalleryModernContent(
    uiState: OutfitUiState,
    onSortClick: () -> Unit,
    onFilterClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOutfitClick: (com.anleanja.wardrobe.database.entities.Outfit) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        GallerySummaryHeader(
            title = "${uiState.outfits.size} outfits",
            subtitle = outfitSummary(uiState)
        )
        FilterChips(
            onSortClick = onSortClick,
            onFilterClick = onFilterClick,
            activeFilterCount = uiState.currentFilters.selectedSeasons.size + if (uiState.currentFilters.temperature != null) 1 else 0
        )
        when {
            uiState.isLoading -> ModernLoadingState()
            uiState.errorMessage != null -> ModernErrorState(
                message = uiState.errorMessage,
                onRetry = onRefreshClick
            )
            uiState.outfits.isEmpty() -> ModernEmptyState(
                title = stringResource(R.string.empty_no_outfits),
                body = "Create an outfit or adjust your filters."
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 170.dp),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(
                    items = uiState.outfits,
                    key = { outfit -> outfit.id }
                ) { outfit ->
                    ModernOutfitCard(
                        outfit = outfit,
                        onClick = { onOutfitClick(outfit) },
                        isSelectionMode = uiState.isSelectionMode
                    )
                }
            }
        }
    }
}

@Composable
private fun GallerySummaryHeader(
    title: String,
    subtitle: String
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

internal fun wardrobeSummary(uiState: WardrobeUiState): String {
    val filterCount = uiState.currentFilters.selectedCategories.size + uiState.currentFilters.selectedSeasons.size
    return "${activeFilterLabel(filterCount)} - Sorted by ${uiState.currentSortOption.displayName}"
}

internal fun outfitSummary(uiState: OutfitUiState): String {
    val filterCount = uiState.currentFilters.selectedSeasons.size + if (uiState.currentFilters.temperature != null) 1 else 0
    val mode = if (uiState.isSelectionMode) "Choose an outfit for a date" else "Browse saved outfit combinations"
    return "${activeFilterLabel(filterCount)} - $mode"
}

private fun activeFilterLabel(count: Int): String = when (count) {
    0 -> "No active filters"
    1 -> "1 active filter"
    else -> "$count active filters"
}
