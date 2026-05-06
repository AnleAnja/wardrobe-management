package com.example.wardrobe.filter_sort

interface SortOption {
    val displayName: String
}

enum class WardrobeSortOption(override val displayName: String) : SortOption {
    MOST_WORN("Most Worn"),
    LEAST_WORN("Least Worn"),
    RECENTLY_WORN("Recently Worn"),
    LEAST_RECENTLY_WORN("Least Recently Worn"),
    RECENTLY_PURCHASED("Recently Purchased"),
    LEAST_RECENTLY_PURCHASED("Least Recently Purchased"),
    HIGHEST_RATING("Highest Rating")
}

enum class OutfitSortOption(override val displayName: String) : SortOption {
    MOST_WORN("Most Worn"),
    LEAST_WORN("Least Worn"),
    RECENTLY_WORN("Recently Worn"),
    LEAST_RECENTLY_WORN("Least Recently Worn"),
    HIGHEST_RATING("Highest Rating")
}

data class WardrobeFilters(
    val selectedSeasons: List<String> = emptyList(),
    val selectedCategories: Set<Pair<String, String>> = emptySet()
)

data class OutfitFilters(
    val selectedSeasons: List<String> = emptyList(),
    val temperature: Int? = null
)