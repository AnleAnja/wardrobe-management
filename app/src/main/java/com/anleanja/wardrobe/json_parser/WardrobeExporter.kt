package com.anleanja.wardrobe.json_parser

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.anleanja.wardrobe.R
import com.anleanja.wardrobe.database.AppDatabase
import com.anleanja.wardrobe.database.entities.Outfit
import com.anleanja.wardrobe.database.entities.OutfitItem
import com.anleanja.wardrobe.database.entities.ScheduledItem
import com.anleanja.wardrobe.database.entities.ScheduledOutfit
import com.anleanja.wardrobe.database.entities.WardrobeItem
import com.anleanja.wardrobe.storage.ImageStorage
import com.anleanja.wardrobe.storage.InstallationId
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.zip.ZipEntry
import javax.inject.Inject

class WardrobeExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val imageStorage: ImageStorage,
    private val installationId: InstallationId,
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportBackup(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        var opened = false
        try {
            val wardrobeItems = database.wardrobeItemDao().getAll().first()
            val outfits = database.outfitDao().getAll().first()
            val outfitItems = database.outfitItemDao().getAll().first()
            val scheduledOutfits = database.scheduledOutfitDao().getAll().first()
            val scheduledItems = database.scheduledItemDao().getAll().first()

            val bundled = WardrobeBackup.bundleImagePaths(
                references = buildList {
                    wardrobeItems.forEach { add(it.imageUri) }
                    outfits.forEach {
                        add(it.imageUriTeaser)
                        add(it.imageUriCombined)
                    }
                },
                localFileName = { imageUri -> imageStorage.localImageFile(imageUri)?.name },
            )

            val exportData = WardrobeImport(
                wardrobeItems = wardrobeItems.map { it.toJson(bundled) },
                outfits = outfits.map { it.toJson(bundled) },
                outfitItems = outfitItems.map { it.toJson() },
                scheduledOutfits = scheduledOutfits.map { it.toJson() },
                scheduledItems = scheduledItems.map { it.toJson() },
                exportVersion = WardrobeBackup.CURRENT_VERSION,
                exportedAt = Instant.now().toString(),
                sourceId = installationId.value,
            )

            val output = context.contentResolver.openOutputStream(uri)
            if (output == null) {
                return@withContext Result.failure(
                    IllegalStateException(context.getString(R.string.error_could_not_write_file))
                )
            }
            opened = true
            output.use { stream ->
                WardrobeBackup.writeZip(stream, gson.toJson(exportData)) { zip ->
                    bundled.forEach { (sourceUri, entryName) ->
                        val file = imageStorage.localImageFile(sourceUri) ?: return@forEach
                        zip.putNextEntry(ZipEntry(entryName))
                        file.inputStream().use { input -> input.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }

            Result.success(context.getString(R.string.success_export, wardrobeItems.size))
        } catch (e: CancellationException) {
            if (opened) deletePartialFile(uri)
            throw e
        } catch (e: Exception) {
            if (opened) deletePartialFile(uri)
            Result.failure(e)
        }
    }

    private fun deletePartialFile(uri: Uri) {
        runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
    }
}

private fun String?.bundledOrOriginal(bundled: Map<String, String>): String? {
    if (this == null) return null
    return bundled[this] ?: this
}

private fun WardrobeItem.toJson(bundled: Map<String, String>) =
    WardrobeItemJson(
        id = id,
        imageUri = imageUri.bundledOrOriginal(bundled),
        category = category,
        subcategory = subcategory,
        rating = rating,
        price = price,
        purchaseDate = purchaseDate,
        seasons = seasons,
        timesWorn = timesWorn,
        lastWorn = lastWorn
    )

private fun Outfit.toJson(bundled: Map<String, String>) =
    OutfitJson(
        id = id,
        imageUriCombined = imageUriCombined.bundledOrOriginal(bundled),
        imageUriTeaser = imageUriTeaser.bundledOrOriginal(bundled),
        seasons = seasons,
        rating = rating,
        timesWorn = timesWorn,
        lastWorn = lastWorn
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

private fun ScheduledItem.toJson() =
    ScheduledItemJson(
        scheduledOutfitId = scheduledOutfitId,
        itemId = itemId
    )
