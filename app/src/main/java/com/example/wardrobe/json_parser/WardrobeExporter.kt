package com.example.wardrobe.json_parser

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.wardrobe.database.AppDatabase
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.OutfitItem
import com.example.wardrobe.database.entities.ScheduledOutfit
import com.example.wardrobe.database.entities.WardrobeItem
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WardrobeExporter @Inject constructor(private val database: AppDatabase) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToJson(context: Context, uri: Uri): Result<String> {
        return try {
            val wardrobeItems = database.wardrobeItemDao().getAll().first()
            val outfits = database.outfitDao().getAll().first()
            val outfitItems = database.outfitItemDao().getAll().first()
            val scheduledOutfits = database.scheduledOutfitDao().getAll().first()

            Log.d("WardrobeExporter", "WardrobeItems: $wardrobeItems")

            val exportData = WardrobeImport(
                wardrobeItems = wardrobeItems.map { it.toJson() },
                outfits = outfits.map { it.toJson() },
                outfitItems = outfitItems.map { it.toJson() },
                scheduledOutfits = scheduledOutfits.map { it.toJson() }
            )

            val jsonString = gson.toJson(exportData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }

            Result.success("Erfolgreich ${wardrobeItems.size} Items exportiert")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun WardrobeItem.toJson() =
    WardrobeItemJson(
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

private fun Outfit.toJson() =
    OutfitJson(
        id = id,
        imageUriCombined = imageUriCombined,
        imageUriTeaser = imageUriTeaser,
        seasons = seasons
    )

private fun OutfitItem.toJson() =
    OutfitItemJson(
        outfitId = outfitId,
        itemId = itemId
    )

private fun ScheduledOutfit.toJson() =
    ScheduledOutfitJson(
        id = id,
        outfitId = outfitId,
        date = date,
        temperature = temperature
    )