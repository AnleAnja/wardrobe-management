package com.example.wardrobe.json_parser

import android.util.Log
import com.example.wardrobe.database.AppDatabase
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.OutfitItem
import com.example.wardrobe.database.entities.ScheduledOutfit
import com.example.wardrobe.database.entities.WardrobeItem
import com.google.gson.Gson
import javax.inject.Inject

class WardrobeImporter @Inject constructor(private val database: AppDatabase) {
    private val gson = Gson()

    suspend fun importFromJson(jsonString: String): Result<String> {
        return try {
            val importData = gson.fromJson(jsonString, WardrobeImport::class.java)

            // Insert into database
            importData.wardrobeItems.forEach { item ->
                database.wardrobeItemDao().insertItem(item.toEntity())
            }

            importData.outfits.forEach { outfit ->
                database.outfitDao().insertOutfit(outfit.toEntity())
            }

            importData.outfitItems.forEach { outfitItem ->
                database.outfitItemDao().insertItem(outfitItem.toEntity())
            }

            importData.scheduledOutfits.forEach { scheduled ->
                database.scheduledOutfitDao().insertOutfit(scheduled.toEntity())
            }
            Log.d("Successfully imported", "${importData.wardrobeItems.size} items")
            Result.success("Successfully imported ${importData.wardrobeItems.size} items")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Extension functions to convert JSON models to entities
private fun WardrobeItemJson.toEntity() = WardrobeItem(
    id = id,
    imageUri = imageUri,
    category = category,
    rating = rating,
    price = price,
    purchaseDate = purchaseDate,
    seasons = seasons,
    timesWorn = timesWorn,
    lastWorn = lastWorn
)

private fun OutfitJson.toEntity() = Outfit(
    id = id,
    imageUriCombined = imageUriCombined,
    imageUriTeaser = imageUriTeaser,
    seasons = seasons
)

private fun OutfitItemJson.toEntity() = OutfitItem(
    outfitId = outfitId,
    itemId = itemId
)

private fun ScheduledOutfitJson.toEntity() = ScheduledOutfit(
    id = id,
    outfitId = outfitId,
    date = date,
    temperature = temperature
)