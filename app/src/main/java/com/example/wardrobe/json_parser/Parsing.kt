package com.example.wardrobe.json_parser

data class WardrobeImport(
    val wardrobeItems: List<WardrobeItemJson>,
    val outfits: List<OutfitJson>,
    val outfitItems: List<OutfitItemJson>,
    val scheduledOutfits: List<ScheduledOutfitJson>
)

data class WardrobeItemJson(
    val id: Int,
    val imageUri: String?,
    val category: String?,
    val rating: Int?,
    val price: Double?,
    val purchaseDate: Long?,
    val seasons: String?,
    val timesWorn: Int,
    val lastWorn: Long?
)

data class OutfitJson(
    val id: Int,
    val imageUri: String?,
    val seasons: String?
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