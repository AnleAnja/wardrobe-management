package com.example.wardrobe.filter_sort

import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.ScheduledOutfit
import com.example.wardrobe.database.entities.WardrobeItem

fun filterWardrobeItems(items: List<WardrobeItem>, filters: WardrobeFilters): List<WardrobeItem> {
    var filteredList = items

    if (filters.selectedSeasons.isNotEmpty()) {
        filteredList = filteredList.filter { item ->
            val itemSeasons = item.seasons?.split(',')?.map { it.trim() } ?: emptyList()
            filters.selectedSeasons.any { selectedSeason -> itemSeasons.contains(selectedSeason) }
        }
    }

    if (filters.selectedCategories.isNotEmpty()) {
        filteredList = filteredList.filter { item ->
            item.category != null && item.subcategory != null &&
                Pair(item.category, item.subcategory) in filters.selectedCategories
        }
    }

    return filteredList
}

fun sortWardrobeItems(items: List<WardrobeItem>, sortOption: WardrobeSortOption?): List<WardrobeItem> {
    return when (sortOption) {
        WardrobeSortOption.MOST_WORN -> items.sortedByDescending { it.timesWorn }
        WardrobeSortOption.LEAST_WORN -> items.sortedBy { it.timesWorn }
        WardrobeSortOption.RECENTLY_WORN -> items.sortedByDescending { it.lastWorn }
        WardrobeSortOption.LEAST_RECENTLY_WORN -> items.sortedBy { it.lastWorn }
        WardrobeSortOption.RECENTLY_PURCHASED -> items.sortedByDescending { it.purchaseDate }
        WardrobeSortOption.LEAST_RECENTLY_PURCHASED -> items.sortedBy { it.purchaseDate }
        WardrobeSortOption.HIGHEST_RATING -> items.sortedByDescending { it.rating }
        null -> items
    }
}

fun extractAvailableCategories(items: List<WardrobeItem>): Set<Pair<String, String>> {
    return items.mapNotNull { item ->
        if (item.category != null && item.subcategory != null) {
            Pair(item.category, item.subcategory)
        } else {
            null
        }
    }.toSet()
}

fun filterOutfitsBySeason(outfits: List<Outfit>, filters: OutfitFilters): List<Outfit> {
    if (filters.selectedSeasons.isEmpty()) return outfits

    return outfits.filter { outfit ->
        val outfitsSeasons = outfit.seasons?.split(',')?.map { it.trim() } ?: emptyList()
        filters.selectedSeasons.any { selectedSeason -> outfitsSeasons.contains(selectedSeason) }
    }
}

fun filterOutfitsByTemperature(
    outfits: List<Outfit>,
    scheduledOutfits: List<ScheduledOutfit>,
    temperature: Int?
): List<Outfit> {
    if (temperature == null) return outfits

    val outfitTempRanges = scheduledOutfits
        .groupBy { it.outfitId }
        .mapValues { (_, scheduledList) ->
            val temps = scheduledList.mapNotNull { it.temperature }
            if (temps.isEmpty()) null else temps.minOrNull() to temps.maxOrNull()
        }

    return outfits.filter { outfit ->
        val tempRange = outfitTempRanges[outfit.id]
        if (tempRange != null && tempRange.first != null && tempRange.second != null) {
            temperature in tempRange.first!!..tempRange.second!!
        } else {
            false
        }
    }
}

fun sortOutfits(outfits: List<Outfit>, sortOption: OutfitSortOption?): List<Outfit> {
    return when (sortOption) {
        OutfitSortOption.MOST_WORN -> outfits.sortedByDescending { it.timesWorn }
        OutfitSortOption.LEAST_WORN -> outfits.sortedBy { it.timesWorn }
        OutfitSortOption.RECENTLY_WORN -> outfits.sortedByDescending { it.lastWorn }
        OutfitSortOption.LEAST_RECENTLY_WORN -> outfits.sortedBy { it.lastWorn }
        OutfitSortOption.HIGHEST_RATING -> outfits.sortedByDescending { it.rating }
        OutfitSortOption.SEASON -> outfits.sortedBy { it.seasons ?: "" }
        null -> outfits
    }
}
