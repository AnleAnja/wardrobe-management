package com.example.wardrobe.json_parser

data class WardrobeImport(
    val wardrobeItems: List<WardrobeItemJson>,
    val outfits: List<OutfitJson>,
    val outfitItems: List<OutfitItemJson>,
    val scheduledOutfits: List<ScheduledOutfitJson>,
    // Nullable so older exports (without this field) still import cleanly.
    val scheduledItems: List<ScheduledItemJson>? = null
)

data class WardrobeItemJson(
    val id: Int,
    val imageUri: String?,
    val category: String?,
    val subcategory: String? = null,
    val rating: Int?,
    val price: Double?,
    val purchaseDate: Long?,
    val seasons: String?,
    val timesWorn: Int,
    val lastWorn: Long?
)

data class OutfitJson(
    val id: Int,
    val imageUriCombined: String?,
    val imageUriTeaser: String?,
    val seasons: String?,
    val rating: Int? = null,
    val timesWorn: Int = 0,
    val lastWorn: Long? = null
)

data class OutfitItemJson(
    val outfitId: Int,
    val itemId: Int
)

data class ScheduledOutfitJson(
    val id: Int,
    val outfitId: Int,
    val date: Long?,
    val temperature: Int?
)

data class ScheduledItemJson(
    val scheduledOutfitId: Int,
    val itemId: Int
)
