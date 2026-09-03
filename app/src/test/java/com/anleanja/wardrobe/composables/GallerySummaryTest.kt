package com.anleanja.wardrobe.composables

import androidx.compose.ui.unit.dp
import com.anleanja.wardrobe.filter_sort.OutfitFilters
import com.anleanja.wardrobe.filter_sort.WardrobeFilters
import com.anleanja.wardrobe.filter_sort.WardrobeSortOption
import com.anleanja.wardrobe.view_models.OutfitUiState
import com.anleanja.wardrobe.view_models.WardrobeUiState
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

    @Test
    fun `gallery stays two columns on compact phone widths`() {
        assertEquals(2, galleryColumnCount(360.dp))
        assertEquals(2, galleryColumnCount(411.dp))
        assertEquals(2, galleryColumnCount(599.dp))
    }

    @Test
    fun `gallery uses three columns on medium widths`() {
        assertEquals(3, galleryColumnCount(600.dp))
        assertEquals(3, galleryColumnCount(839.dp))
    }

    @Test
    fun `gallery uses four columns on expanded widths`() {
        assertEquals(4, galleryColumnCount(840.dp))
    }
}
