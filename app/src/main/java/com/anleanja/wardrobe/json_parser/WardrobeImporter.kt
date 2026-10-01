package com.anleanja.wardrobe.json_parser

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.anleanja.wardrobe.R
import com.anleanja.wardrobe.database.AppDatabase
import com.anleanja.wardrobe.database.entities.Outfit
import com.anleanja.wardrobe.database.entities.OutfitItem
import com.anleanja.wardrobe.database.entities.ScheduledItem
import com.anleanja.wardrobe.database.entities.ScheduledOutfit
import com.anleanja.wardrobe.database.entities.WardrobeItem
import com.anleanja.wardrobe.storage.ImageStorage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

class WardrobeImporter @Inject constructor(
    private val database: AppDatabase,
    private val imageStorage: ImageStorage,
) {
    private val gson = Gson()

    suspend fun importBackup(context: Context, uri: Uri, mode: ImportMode): Result<ImportSuccess> =
        withContext(Dispatchers.IO) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(
                        IllegalStateException(context.getString(R.string.error_could_not_read_file))
                    )
                input.buffered().use { buffered ->
                    buffered.mark(4)
                    val header = ByteArray(4)
                    val read = buffered.read(header)
                    buffered.reset()
                    if (read == 4 && WardrobeBackup.isZip(header)) {
                        importZip(context, buffered, mode)
                    } else {
                        importParsed(WardrobeBackup.parse(buffered.reader().readText(), gson), emptyMap(), mode)
                    }
                }
            } catch (e: BackupFormatException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun importZip(
        context: Context,
        input: InputStream,
        mode: ImportMode,
    ): Result<ImportSuccess> {
        val temp = File(context.cacheDir, "wardrobe-import-${UUID.randomUUID()}").apply { mkdirs() }
        return try {
            val json = WardrobeBackup.readZip(input) { fileName, bytes ->
                File(temp, fileName).writeBytes(bytes)
            }
            val data = WardrobeBackup.parse(json, gson)
            val savedByFileName = mutableMapOf<String, String>()
            val copied = mutableListOf<String>()
            var committed = false
            try {
                temp.listFiles().orEmpty().forEach { file ->
                    val stored = imageStorage.saveBytes(file.readBytes(), WardrobeBackup.extensionOf(file.name))
                        ?: throw BackupFormatException(BackupFormatException.Kind.CORRUPT)
                    copied += stored
                    savedByFileName[file.name] = stored
                }
                val result = importParsed(data, savedByFileName, mode)
                committed = result.isSuccess
                result
            } finally {
                if (!committed) copied.forEach { imageStorage.deleteImage(it) }
            }
        } finally {
            temp.deleteRecursively()
        }
    }

    private suspend fun importParsed(
        data: WardrobeImport,
        savedByFileName: Map<String, String>,
        mode: ImportMode,
    ): Result<ImportSuccess> {
        val rewritten = data.withResolvedImages { uri ->
            WardrobeBackup.resolveImportedImage(uri, savedByFileName)
        }
        database.withTransaction {
            val toWrite = if (mode == ImportMode.MERGE) {
                preserveLocalImages(rewritten)
            } else {
                rewritten
            }
            if (mode == ImportMode.REPLACE) {
                database.scheduledItemDao().deleteAll()
                database.outfitItemDao().deleteAll()
                database.scheduledOutfitDao().deleteAll()
                database.outfitDao().deleteAll()
                database.wardrobeItemDao().deleteAll()
            }
            toWrite.wardrobeItems.forEach { item ->
                database.wardrobeItemDao().insertItem(item.toEntity())
            }
            toWrite.outfits.forEach { outfit ->
                database.outfitDao().insertOutfit(outfit.toEntity())
            }
            toWrite.outfitItems.forEach { outfitItem ->
                database.outfitItemDao().insertItem(outfitItem.toEntity())
            }
            toWrite.scheduledOutfits.forEach { scheduled ->
                database.scheduledOutfitDao().insertOutfit(scheduled.toEntity())
            }
            toWrite.scheduledItems.orEmpty().forEach { scheduledItem ->
                database.scheduledItemDao().insertItem(scheduledItem.toEntity())
            }
        }
        runCatching { imageStorage.deleteUnreferenced(referencedImageUris()) }
        return Result.success(ImportSuccess(legacyWithoutPhotos = WardrobeBackup.isLegacy(data)))
    }

    private suspend fun preserveLocalImages(data: WardrobeImport): WardrobeImport {
        val itemImages = database.wardrobeItemDao().imageUrisById()
            .associate { it.id to it.imageUri }
        val outfitImages = database.outfitDao().imageUrisById()
            .associateBy { it.id }
        return data.copy(
            wardrobeItems = data.wardrobeItems.map { item ->
                item.copy(
                    imageUri = mergeImageUri(
                        existing = itemImages[item.id],
                        imported = item.imageUri,
                        importedIsLocal = imageStorage.isLocalImage(item.imageUri),
                    )
                )
            },
            outfits = data.outfits.map { outfit ->
                val existing = outfitImages[outfit.id]
                outfit.copy(
                    imageUriCombined = mergeImageUri(
                        existing = existing?.imageUriCombined,
                        imported = outfit.imageUriCombined,
                        importedIsLocal = imageStorage.isLocalImage(outfit.imageUriCombined),
                    ),
                    imageUriTeaser = mergeImageUri(
                        existing = existing?.imageUriTeaser,
                        imported = outfit.imageUriTeaser,
                        importedIsLocal = imageStorage.isLocalImage(outfit.imageUriTeaser),
                    ),
                )
            },
        )
    }

    private suspend fun referencedImageUris(): Set<String> = buildSet {
        database.wardrobeItemDao().imageUris().filterTo(this) { it.isNotEmpty() }
        database.outfitDao().teaserUris().filterTo(this) { it.isNotEmpty() }
        database.outfitDao().combinedUris().filterTo(this) { it.isNotEmpty() }
    }
}

/**
 * Keeps the photo already on this device when a merge backup did not install a replacement
 * file. A live file under the app image directory wins; otherwise a non-blank existing URI
 * (including a legacy content URI) is left in place. Unusable backup paths are dropped.
 */
internal fun mergeImageUri(existing: String?, imported: String?, importedIsLocal: Boolean): String? {
    if (importedIsLocal) return imported
    if (!existing.isNullOrBlank()) return existing
    return null
}

private fun WardrobeItemJson.toEntity() = WardrobeItem(
    id = id,
    imageUri = imageUri,
    category = category,
    subcategory = subcategory,
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
    seasons = seasons,
    rating = rating,
    timesWorn = timesWorn,
    lastWorn = lastWorn
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

private fun ScheduledItemJson.toEntity() = ScheduledItem(
    scheduledOutfitId = scheduledOutfitId,
    itemId = itemId
)
