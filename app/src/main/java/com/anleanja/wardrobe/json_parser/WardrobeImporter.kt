package com.anleanja.wardrobe.json_parser

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.anleanja.wardrobe.R
import com.anleanja.wardrobe.database.AppDatabase
import com.anleanja.wardrobe.database.entities.ImportedId
import com.anleanja.wardrobe.database.entities.Outfit
import com.anleanja.wardrobe.database.entities.OutfitItem
import com.anleanja.wardrobe.database.entities.ScheduledItem
import com.anleanja.wardrobe.database.entities.ScheduledOutfit
import com.anleanja.wardrobe.database.entities.WardrobeItem
import com.anleanja.wardrobe.storage.ImageStorage
import com.anleanja.wardrobe.storage.InstallationId
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

class WardrobeImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val imageStorage: ImageStorage,
    private val installationId: InstallationId,
) {
    private val gson = Gson()

    suspend fun importBackup(uri: Uri, mode: ImportMode): Result<ImportSuccess> =
        withContext(Dispatchers.IO) {
            try {
                // The picker's read grant can be gone if the import dialog outlived the process.
                val input = try {
                    context.contentResolver.openInputStream(uri)
                } catch (e: SecurityException) {
                    null
                } catch (e: FileNotFoundException) {
                    null
                } ?: return@withContext Result.failure(
                    IllegalStateException(context.getString(R.string.error_could_not_read_file))
                )
                input.buffered().use { buffered ->
                    buffered.mark(WardrobeBackup.HEADER_SIZE)
                    val header = WardrobeBackup.readHeader(buffered)
                    buffered.reset()
                    if (WardrobeBackup.isZip(header)) {
                        importZip(buffered, mode)
                    } else {
                        val data = WardrobeBackup.parse(WardrobeBackup.readJson(buffered), gson)
                        importParsed(data, savedByFileName = emptyMap(), installed = emptyList(), mode)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun importZip(input: InputStream, mode: ImportMode): Result<ImportSuccess> {
        val temp = File(context.cacheDir, "wardrobe-import-${UUID.randomUUID()}").apply { mkdirs() }
        val installed = mutableListOf<String>()
        var committed = false
        try {
            val limits = BackupLimits(
                maxTotalBytes = (imageStorage.availableBytes() - STORAGE_RESERVE_BYTES).coerceAtLeast(0),
            )
            val json = WardrobeBackup.readZip(input, limits) { fileName -> File(temp, fileName).outputStream() }
            val data = WardrobeBackup.parse(json, gson)
            val savedByFileName = mutableMapOf<String, String>()
            temp.listFiles().orEmpty().forEach { file ->
                val stored = imageStorage.saveImportedImage(file) ?: return@forEach
                installed += stored
                savedByFileName[file.name] = stored
            }
            val result = importParsed(data, savedByFileName, installed, mode)
            committed = true
            return result
        } finally {
            if (!committed) installed.forEach { imageStorage.deleteImage(it) }
            temp.deleteRecursively()
        }
    }

    private suspend fun importParsed(
        data: WardrobeImport,
        savedByFileName: Map<String, String>,
        installed: List<String>,
        mode: ImportMode,
    ): Result<ImportSuccess> {
        val resolved = data.withResolvedImages { uri ->
            WardrobeBackup.resolveImportedImage(uri, savedByFileName)
        }
        val foreignSource = data.sourceId?.takeUnless { WardrobeBackup.isFromSameSource(data, installationId.value) }
        val trustContentUris = foreignSource == null
        val obsoleteImages = database.withTransaction {
            val referencedBefore = referencedLocalImages()
            if (mode == ImportMode.REPLACE) {
                deleteAllRows()
                database.importedIdDao().deleteAll()
            }
            val toWrite = when {
                mode == ImportMode.REPLACE -> {
                    if (foreignSource != null) recordMappings(foreignSource, identityMappings(resolved))
                    keepUsableImages(resolved, trustContentUris)
                }
                foreignSource == null -> preserveLocalImages(resolved, trustContentUris)
                else -> {
                    val remap = WardrobeBackup.remapIds(resolved, knownMappings(foreignSource), nextFreeIds())
                    recordMappings(foreignSource, remap.mappings)
                    preserveLocalImages(remap.data, trustContentUris)
                }
            }
            writeRows(toWrite)
            val installedPaths = installed.mapNotNull { imageStorage.localImageFile(it)?.canonicalPath }
            (referencedBefore + installedPaths) - referencedLocalImages()
        }
        obsoleteImages.forEach { imageStorage.deleteImage(it) }
        return Result.success(ImportSuccess(legacyWithoutPhotos = WardrobeBackup.isLegacy(data)))
    }

    private suspend fun deleteAllRows() {
        database.scheduledItemDao().deleteAll()
        database.outfitItemDao().deleteAll()
        database.scheduledOutfitDao().deleteAll()
        database.outfitDao().deleteAll()
        database.wardrobeItemDao().deleteAll()
    }

    private suspend fun writeRows(data: WardrobeImport) {
        data.wardrobeItems.forEach { item ->
            database.wardrobeItemDao().upsertItem(item.toEntity())
        }
        data.outfits.forEach { outfit ->
            database.outfitDao().upsertOutfit(outfit.toEntity())
        }
        data.outfitItems.forEach { outfitItem ->
            database.outfitItemDao().insertItem(outfitItem.toEntity())
        }
        data.scheduledOutfits.forEach { scheduled ->
            database.scheduledOutfitDao().upsertOutfit(scheduled.toEntity())
        }
        data.scheduledItems.orEmpty().forEach { scheduledItem ->
            database.scheduledItemDao().insertItem(scheduledItem.toEntity())
        }
    }

    private suspend fun nextFreeIds() = NextIds(
        items = database.wardrobeItemDao().maxId() + 1,
        outfits = database.outfitDao().maxId() + 1,
        scheduledOutfits = database.scheduledOutfitDao().maxId() + 1,
    )

    /** Earlier merges from [sourceId], ignoring rows that were deleted locally since. */
    private suspend fun knownMappings(sourceId: String): IdMappings {
        val rows = database.importedIdDao().forSource(sourceId)
        fun mappingsFor(kind: String, existing: Set<Int>) = rows
            .filter { it.kind == kind && it.localId in existing }
            .associate { it.foreignId to it.localId }
        return IdMappings(
            items = mappingsFor(ImportedId.KIND_ITEM, database.wardrobeItemDao().ids().toSet()),
            outfits = mappingsFor(ImportedId.KIND_OUTFIT, database.outfitDao().ids().toSet()),
            scheduledOutfits = mappingsFor(
                ImportedId.KIND_SCHEDULED_OUTFIT,
                database.scheduledOutfitDao().ids().toSet(),
            ),
        )
    }

    private fun identityMappings(data: WardrobeImport) = IdMappings(
        items = data.wardrobeItems.associate { it.id to it.id },
        outfits = data.outfits.associate { it.id to it.id },
        scheduledOutfits = data.scheduledOutfits.associate { it.id to it.id },
    )

    private suspend fun recordMappings(sourceId: String, mappings: IdMappings) {
        fun rows(kind: String, ids: Map<Int, Int>) = ids.map { (foreignId, localId) ->
            ImportedId(sourceId = sourceId, kind = kind, foreignId = foreignId, localId = localId)
        }
        database.importedIdDao().upsertAll(
            rows(ImportedId.KIND_ITEM, mappings.items) +
                rows(ImportedId.KIND_OUTFIT, mappings.outfits) +
                rows(ImportedId.KIND_SCHEDULED_OUTFIT, mappings.scheduledOutfits)
        )
    }

    private fun keepUsableImages(data: WardrobeImport, trustContentUris: Boolean): WardrobeImport {
        return data.withResolvedImages { uri ->
            importedImageUri(uri, imageStorage.isLocalImage(uri), trustContentUris)
        }
    }

    private suspend fun preserveLocalImages(data: WardrobeImport, trustContentUris: Boolean): WardrobeImport {
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
                        trustContentUris = trustContentUris,
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
                        trustContentUris = trustContentUris,
                    ),
                    imageUriTeaser = mergeImageUri(
                        existing = existing?.imageUriTeaser,
                        imported = outfit.imageUriTeaser,
                        importedIsLocal = imageStorage.isLocalImage(outfit.imageUriTeaser),
                        trustContentUris = trustContentUris,
                    ),
                )
            },
        )
    }

    private suspend fun referencedLocalImages(): Set<String> {
        val items = database.wardrobeItemDao().imageUrisById().map { it.imageUri }
        val outfits = database.outfitDao().imageUrisById()
            .flatMap { listOf(it.imageUriTeaser, it.imageUriCombined) }
        return (items + outfits)
            .mapNotNull { uri -> imageStorage.localImageFile(uri)?.canonicalPath }
            .toSet()
    }

    private companion object {
        /** Left free so an import never fills the device. */
        const val STORAGE_RESERVE_BYTES = 200L * 1024 * 1024
    }
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
