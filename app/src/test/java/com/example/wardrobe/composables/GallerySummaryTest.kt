package com.example.wardrobe.composables

import com.example.wardrobe.filter_sort.OutfitFilters
import com.example.wardrobe.filter_sort.WardrobeFilters
import com.example.wardrobe.filter_sort.WardrobeSortOption
import com.example.wardrobe.view_models.OutfitUiState
import com.example.wardrobe.view_models.WardrobeUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class GallerySummaryTest {

    @Test
    fun `wardrobe summary without filters mentions sort option`() {
        val state = WardrobeUiState(currentSortOption = WardrobeSortOption.HIGHEST_RATING)
        assertEquals("No active filters - Sorted by Highest Rating", wardrobeSummary(state))
    }

    @Test
    fun `wardrobe summary counts categories and seasons`() {
        val state = WardrobeUiState(
            currentFilters = WardrobeFilters(
                selectedSeasons = listOf("Summer"),
                selectedCategories = setOf("Tops" to "T-Shirts")
            )
        )
        assertEquals("2 active filters - Sorted by Recently Worn", wardrobeSummary(state))
    }

    @Test
    fun `wardrobe summary uses singular for one filter`() {
        val state = WardrobeUiState(
            currentFilters = WardrobeFilters(selectedSeasons = listOf("Winter"))
        )
        assertEquals("1 active filter - Sorted by Recently Worn", wardrobeSummary(state))
    }

    @Test
    fun `outfit summary in browse mode`() {
        assertEquals(
            "No active filters - Browse saved outfit combinations",
            outfitSummary(OutfitUiState())
        )
    }

    @Test
    fun `outfit summary counts temperature as a filter and shows selection mode`() {
        val state = OutfitUiState(
            currentFilters = OutfitFilters(selectedSeasons = listOf("Fall"), temperature = 15),
            isSelectionMode = true
        )
        assertEquals("2 active filters - Choose an outfit for a date", outfitSummary(state))
    }
}
