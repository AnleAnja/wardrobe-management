package com.anleanja.wardrobe.filter_sort

import com.anleanja.wardrobe.support.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterSortUtilsTest {

    private val summerTop = TestFixtures.wardrobeItem(
        id = 1,
        category = "Tops",
        subcategory = "T-Shirts",
        seasons = "Summer,Spring",
        timesWorn = 5,
        lastWorn = 300L,
        rating = 4,
        purchaseDate = 100L
    )
    private val winterCoat = TestFixtures.wardrobeItem(
        id = 2,
        category = "Outerwear",
        subcategory = "Coats",
        seasons = "Winter",
        timesWorn = 1,
        lastWorn = 500L,
        rating = 5,
        purchaseDate = 200L
    )
    private val uncategorized = TestFixtures.wardrobeItem(
        id = 3,
        category = null,
        subcategory = null,
        seasons = "Summer",
        timesWorn = 0,
        lastWorn = null
    )

    @Test
    fun filterWardrobeItems_returnsAllWhenFiltersEmpty() {
        val items = listOf(summerTop, winterCoat)
        assertEquals(items, filterWardrobeItems(items, WardrobeFilters()))
    }

    @Test
    fun filterWardrobeItems_matchesAnySelectedSeason() {
        val filters = WardrobeFilters(selectedSeasons = listOf("Winter"))
        val result = filterWardrobeItems(listOf(summerTop, winterCoat), filters)
        assertEquals(listOf(winterCoat), result)
    }

    @Test
    fun filterWardrobeItems_splitsCommaSeparatedSeasons() {
        val filters = WardrobeFilters(selectedSeasons = listOf("Spring"))
        val result = filterWardrobeItems(listOf(summerTop), filters)
        assertEquals(listOf(summerTop), result)
    }

    @Test
    fun filterWardrobeItems_matchesSelectedCategoryPair() {
        val filters = WardrobeFilters(
            selectedCategories = setOf("Tops" to "T-Shirts")
        )
        val result = filterWardrobeItems(listOf(summerTop, winterCoat), filters)
        assertEquals(listOf(summerTop), result)
    }

    @Test
    fun filterWardrobeItems_excludesItemsMissingCategoryOrSubcategory() {
        val filters = WardrobeFilters(
            selectedCategories = setOf("Tops" to "T-Shirts")
        )
        assertTrue(filterWardrobeItems(listOf(uncategorized), filters).isEmpty())
    }

    @Test
    fun filterWardrobeItems_appliesSeasonAndCategoryTogether() {
        val filters = WardrobeFilters(
            selectedSeasons = listOf("Summer"),
            selectedCategories = setOf("Tops" to "T-Shirts")
        )
        val result = filterWardrobeItems(listOf(summerTop, winterCoat), filters)
        assertEquals(listOf(summerTop), result)
    }

    @Test
    fun sortWardrobeItems_mostWorn() {
        val result = sortWardrobeItems(
            listOf(summerTop, winterCoat),
            WardrobeSortOption.MOST_WORN
        )
        assertEquals(listOf(summerTop, winterCoat), result)
    }

    @Test
    fun sortWardrobeItems_leastWorn() {
        val result = sortWardrobeItems(
            listOf(summerTop, winterCoat),
            WardrobeSortOption.LEAST_WORN
        )
        assertEquals(listOf(winterCoat, summerTop), result)
    }

    @Test
    fun sortWardrobeItems_recentlyWorn() {
        val result = sortWardrobeItems(
            listOf(summerTop, winterCoat),
            WardrobeSortOption.RECENTLY_WORN
        )
        assertEquals(listOf(winterCoat, summerTop), result)
    }

    @Test
    fun sortWardrobeItems_highestRating() {
        val result = sortWardrobeItems(
            listOf(summerTop, winterCoat),
            WardrobeSortOption.HIGHEST_RATING
        )
        assertEquals(listOf(winterCoat, summerTop), result)
    }

    @Test
    fun sortWardrobeItems_recentlyPurchased() {
        val result = sortWardrobeItems(
            listOf(summerTop, winterCoat),
            WardrobeSortOption.RECENTLY_PURCHASED
        )
        assertEquals(listOf(winterCoat, summerTop), result)
    }

    @Test
    fun sortWardrobeItems_nullOptionPreservesInputOrder() {
        val items = listOf(winterCoat, summerTop)
        assertEquals(items, sortWardrobeItems(items, null))
    }

    @Test
    fun extractAvailableCategories_skipsIncompleteItems() {
        val categories = extractAvailableCategories(listOf(summerTop, uncategorized))
        assertEquals(setOf("Tops" to "T-Shirts"), categories)
    }

    @Test
    fun groupWardrobeItemsByCategoryRecentlyWorn_groupsAndOrdersCategories() {
        val olderTop = summerTop.copy(lastWorn = 100L)
        val newerCoat = winterCoat.copy(lastWorn = 900L)
        val grouped = groupWardrobeItemsByCategoryRecentlyWorn(listOf(olderTop, newerCoat))

        assertEquals(listOf("Outerwear", "Tops"), grouped.keys.toList())
        assertEquals(listOf(newerCoat), grouped["Outerwear"])
        assertEquals(listOf(olderTop), grouped["Tops"])
    }

    @Test
    fun groupWardrobeItemsByCategoryRecentlyWorn_usesUncategorizedLabel() {
        val grouped = groupWardrobeItemsByCategoryRecentlyWorn(listOf(uncategorized))
        assertEquals(listOf(uncategorized), grouped["Uncategorized"])
    }

    @Test
    fun filterOutfitsBySeason_returnsAllWhenNoSeasonSelected() {
        val outfits = listOf(
            TestFixtures.outfit(id = 1, seasons = "Summer"),
            TestFixtures.outfit(id = 2, seasons = "Winter")
        )
        assertEquals(outfits, filterOutfitsBySeason(outfits, OutfitFilters()))
    }

    @Test
    fun filterOutfitsBySeason_matchesCommaSeparatedSeasons() {
        val summerOutfit = TestFixtures.outfit(id = 1, seasons = "Summer,Fall")
        val winterOutfit = TestFixtures.outfit(id = 2, seasons = "Winter")
        val filters = OutfitFilters(selectedSeasons = listOf("Fall"))

        val result = filterOutfitsBySeason(listOf(summerOutfit, winterOutfit), filters)
        assertEquals(listOf(summerOutfit), result)
    }

    @Test
    fun filterOutfitsByTemperature_returnsAllWhenTemperatureNull() {
        val outfits = listOf(TestFixtures.outfit(id = 1))
        val scheduled = listOf(TestFixtures.scheduledOutfit(outfitId = 1, temperature = 10))
        assertEquals(outfits, filterOutfitsByTemperature(outfits, scheduled, null))
    }

    @Test
    fun filterOutfitsByTemperature_matchesScheduledRange() {
        val outfit = TestFixtures.outfit(id = 1)
        val scheduled = listOf(
            TestFixtures.scheduledOutfit(id = 1, outfitId = 1, temperature = 5),
            TestFixtures.scheduledOutfit(id = 2, outfitId = 1, temperature = 15)
        )
        val result = filterOutfitsByTemperature(listOf(outfit), scheduled, 10)
        assertEquals(listOf(outfit), result)
    }

    @Test
    fun filterOutfitsByTemperature_excludesOutfitsWithoutScheduledData() {
        val outfit = TestFixtures.outfit(id = 1)
        val result = filterOutfitsByTemperature(listOf(outfit), emptyList(), 10)
        assertTrue(result.isEmpty())
    }

    @Test
    fun filterOutfitsByTemperature_excludesOutfitsOutsideRange() {
        val outfit = TestFixtures.outfit(id = 1)
        val scheduled = listOf(
            TestFixtures.scheduledOutfit(outfitId = 1, temperature = 0),
            TestFixtures.scheduledOutfit(outfitId = 1, temperature = 5)
        )
        val result = filterOutfitsByTemperature(listOf(outfit), scheduled, 10)
        assertTrue(result.isEmpty())
    }

    @Test
    fun sortOutfits_mostWornAndSeason() {
        val low = TestFixtures.outfit(id = 1, timesWorn = 1, seasons = "Winter")
        val high = TestFixtures.outfit(id = 2, timesWorn = 5, seasons = "Summer")

        assertEquals(
            listOf(high, low),
            sortOutfits(listOf(low, high), OutfitSortOption.MOST_WORN)
        )
        assertEquals(
            listOf(high, low),
            sortOutfits(listOf(high, low), OutfitSortOption.SEASON)
        )
    }

    @Test
    fun wardrobeViewModelPipeline_appliesFilterBeforeSort() {
        val items = listOf(
            summerTop.copy(timesWorn = 1),
            summerTop.copy(id = 4, subcategory = "Blouses", timesWorn = 10),
            winterCoat
        )
        val filters = WardrobeFilters(selectedSeasons = listOf("Summer"))
        val filtered = filterWardrobeItems(items, filters)
        val sorted = sortWardrobeItems(filtered, WardrobeSortOption.MOST_WORN)

        assertEquals(listOf(4, 1), sorted.map { it.id })
    }

    @Test
    fun outfitsViewModelPipeline_appliesTemperatureSeasonThenSort() {
        val warmOutfit = TestFixtures.outfit(id = 1, seasons = "Summer", timesWorn = 1)
        val coldOutfit = TestFixtures.outfit(id = 2, seasons = "Winter", timesWorn = 5)
        val scheduled = listOf(
            TestFixtures.scheduledOutfit(outfitId = 1, temperature = 20),
            TestFixtures.scheduledOutfit(outfitId = 2, temperature = 0)
        )
        val filters = OutfitFilters(selectedSeasons = listOf("Summer"), temperature = 20)

        val byTemp = filterOutfitsByTemperature(listOf(warmOutfit, coldOutfit), scheduled, filters.temperature)
        val bySeason = filterOutfitsBySeason(byTemp, filters)
        val sorted = sortOutfits(bySeason, OutfitSortOption.MOST_WORN)

        assertEquals(listOf(warmOutfit), sorted)
    }
}
