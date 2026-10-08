package com.anleanja.wardrobe.json_parser

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class ImportMode {
    MERGE,
    REPLACE,
}

data class ImportSuccess(val legacyWithoutPhotos: Boolean)

class BackupFormatException(val kind: Kind) : Exception() {
    enum class Kind { CORRUPT, UNSUPPORTED, TOO_LARGE, NO_SPACE }
}

/**
 * Import guards. Per-entry caps sit well above anything a camera produces, so a user's own
 * export (including full-size photos saved before resizing existed) always fits;
 * [maxTotalBytes] is set from the free storage on the device.
 */
data class BackupLimits(
    val maxJsonBytes: Long = 50L * 1024 * 1024,
    val maxImageBytes: Long = 200L * 1024 * 1024,
    val maxTotalBytes: Long = Long.MAX_VALUE,
    val maxEntries: Int = 100_000,
)

object WardrobeBackup {
    const val LEGACY_VERSION = 1
    const val CURRENT_VERSION = 2
    const val JSON_ENTRY = "wardrobe.json"
    const val IMAGES_PREFIX = "images/"
    const val HEADER_SIZE = 4

    private val gson = Gson()

    /** Reads up to [size] bytes, looping because a single read may return fewer. */
    fun readHeader(input: InputStream, size: Int = HEADER_SIZE): ByteArray {
        val header = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = input.read(header, offset, size - offset)
            if (read < 0) break
            offset += read
        }
        return header.copyOf(offset)
    }

    fun isZip(header: ByteArray): Boolean {
        return header.size >= 4 &&
            header[0] == 0x50.toByte() &&
            header[1] == 0x4B.toByte() &&
            header[2] == 0x03.toByte() &&
            header[3] == 0x04.toByte()
    }

    fun isLegacy(data: WardrobeImport): Boolean {
        return (data.exportVersion ?: LEGACY_VERSION) < CURRENT_VERSION
    }

    fun parse(json: String, parser: Gson = gson): WardrobeImport {
        if (json.isBlank()) throw BackupFormatException(BackupFormatException.Kind.CORRUPT)
        val data = try {
            parser.fromJson(json, WardrobeImport::class.java)
        } catch (e: JsonSyntaxException) {
            throw BackupFormatException(BackupFormatException.Kind.CORRUPT)
        } ?: throw BackupFormatException(BackupFormatException.Kind.CORRUPT)

        try {
            // Gson can leave non-null Kotlin fields null when the JSON omits them.
            data.wardrobeItems.iterator()
            data.outfits.iterator()
            data.outfitItems.iterator()
            data.scheduledOutfits.iterator()
        } catch (e: NullPointerException) {
            throw BackupFormatException(BackupFormatException.Kind.CORRUPT)
        }

        val version = data.exportVersion ?: LEGACY_VERSION
        if (version !in LEGACY_VERSION..CURRENT_VERSION) {
            throw BackupFormatException(BackupFormatException.Kind.UNSUPPORTED)
        }
        return data
    }

    /**
     * Maps local image URIs to zip entry names. [localFileName] returns the file name for a
     * URI this device can bundle, or null when the URI should be left unchanged.
     */
    fun bundleImagePaths(
        references: List<String?>,
        localFileName: (String) -> String?,
    ): Map<String, String> {
        val usedEntries = mutableSetOf<String>()
        val bundled = linkedMapOf<String, String>()
        references.forEach { uri ->
            if (uri == null || bundled.containsKey(uri)) return@forEach
            val rawName = localFileName(uri)
                ?.substringAfterLast('/')
                ?.substringAfterLast('\\')
                ?: return@forEach
            if (rawName.isEmpty() || rawName == "." || rawName == "..") return@forEach
            var entry = IMAGES_PREFIX + rawName
            var suffix = 1
            while (!usedEntries.add(entry)) {
                entry = "$IMAGES_PREFIX${suffix}_$rawName"
                suffix++
            }
            bundled[uri] = entry
        }
        return bundled
    }

    /** File name for an `images/…` entry, or null when [entryName] is not an image entry. */
    fun imageFileName(entryName: String): String? {
        val name = entryName.replace('\\', '/').trimStart('/')
        val parts = name.split('/')
        if (parts.any { it == ".." || it == "." }) return null
        if (parts.size != 2 || parts[0] != "images") return null
        val fileName = parts[1]
        if (fileName.isEmpty()) return null
        return fileName
    }

    fun resolveImportedImage(uri: String?, savedByFileName: Map<String, String>): String? {
        if (uri == null) return null
        val fileName = imageFileName(uri) ?: return uri
        return savedByFileName[fileName]
    }

    fun writeZip(output: OutputStream, json: String, writeImages: (ZipOutputStream) -> Unit) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_ENTRY))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            writeImages(zip)
        }
    }

    /**
     * Streams a backup zip. Each image entry is copied into the stream returned by
     * [openImage]; nothing larger than [limits] is ever held in memory or written out.
     */
    fun readZip(
        input: InputStream,
        limits: BackupLimits = BackupLimits(),
        openImage: (fileName: String) -> OutputStream,
    ): String {
        var json: String? = null
        val seenImages = mutableSetOf<String>()
        var entries = 0
        var totalBytes = 0L
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (++entries > limits.maxEntries) {
                    throw BackupFormatException(BackupFormatException.Kind.TOO_LARGE)
                }
                if (!entry.isDirectory) {
                    val name = entry.name.replace('\\', '/')
                    if (name.split('/').any { it == ".." }) {
                        throw BackupFormatException(BackupFormatException.Kind.CORRUPT)
                    }
                    val remaining = limits.maxTotalBytes - totalBytes
                    if (name == JSON_ENTRY) {
                        if (json != null) throw BackupFormatException(BackupFormatException.Kind.CORRUPT)
                        val buffer = ByteArrayOutputStream()
                        totalBytes += copyWithinLimits(zip, buffer, limits.maxJsonBytes, remaining)
                        json = buffer.toByteArray().decodeToString()
                    } else {
                        imageFileName(name)?.let { fileName ->
                            if (!seenImages.add(fileName)) {
                                throw BackupFormatException(BackupFormatException.Kind.CORRUPT)
                            }
                            totalBytes += openImage(fileName).use { output ->
                                copyWithinLimits(zip, output, limits.maxImageBytes, remaining)
                            }
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return json ?: throw BackupFormatException(BackupFormatException.Kind.CORRUPT)
    }

    /**
     * Copies [input] to [output]. Exceeding [maxEntryBytes] fails with
     * [BackupFormatException.Kind.TOO_LARGE]; exceeding [remainingBytes] (free storage)
     * fails with [BackupFormatException.Kind.NO_SPACE].
     */
    fun copyWithinLimits(
        input: InputStream,
        output: OutputStream,
        maxEntryBytes: Long,
        remainingBytes: Long = Long.MAX_VALUE,
    ): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxEntryBytes) throw BackupFormatException(BackupFormatException.Kind.TOO_LARGE)
            if (total > remainingBytes) throw BackupFormatException(BackupFormatException.Kind.NO_SPACE)
            output.write(buffer, 0, read)
        }
        return total
    }

    fun readJson(input: InputStream, limits: BackupLimits = BackupLimits()): String {
        val buffer = ByteArrayOutputStream()
        copyWithinLimits(input, buffer, limits.maxJsonBytes)
        return buffer.toByteArray().decodeToString()
    }

    /**
     * A backup from another installation has ids that mean nothing on this device. Each
     * record goes to the local row it was merged into before ([known]), or otherwise to the
     * next free id, so unrelated local records are never overwritten and repeating the merge
     * updates instead of duplicating. Repeated ids keep their first record; links whose
     * parent is missing from the backup are dropped.
     */
    fun remapIds(data: WardrobeImport, known: IdMappings, nextFree: NextIds): RemapResult {
        val items = data.wardrobeItems.distinctBy { it.id }
        val outfits = data.outfits.distinctBy { it.id }
        val itemIds = assignIds(items.map { it.id }, known.items, nextFree.items)
        val outfitIds = assignIds(outfits.map { it.id }, known.outfits, nextFree.outfits)
        val scheduled = data.scheduledOutfits.distinctBy { it.id }.filter { it.outfitId in outfitIds }
        val scheduledIds = assignIds(scheduled.map { it.id }, known.scheduledOutfits, nextFree.scheduledOutfits)
        val remapped = data.copy(
            wardrobeItems = items.map { it.copy(id = itemIds.getValue(it.id)) },
            outfits = outfits.map { it.copy(id = outfitIds.getValue(it.id)) },
            outfitItems = data.outfitItems.mapNotNull { link ->
                val outfitId = outfitIds[link.outfitId] ?: return@mapNotNull null
                val itemId = itemIds[link.itemId] ?: return@mapNotNull null
                OutfitItemJson(outfitId = outfitId, itemId = itemId)
            }.distinct(),
            scheduledOutfits = scheduled.map {
                it.copy(id = scheduledIds.getValue(it.id), outfitId = outfitIds.getValue(it.outfitId))
            },
            scheduledItems = data.scheduledItems?.mapNotNull { link ->
                val scheduledId = scheduledIds[link.scheduledOutfitId] ?: return@mapNotNull null
                val itemId = itemIds[link.itemId] ?: return@mapNotNull null
                ScheduledItemJson(scheduledOutfitId = scheduledId, itemId = itemId)
            }?.distinct(),
        )
        return RemapResult(remapped, IdMappings(itemIds, outfitIds, scheduledIds))
    }

    private fun assignIds(foreignIds: List<Int>, known: Map<Int, Int>, firstFree: Int): Map<Int, Int> {
        var next = firstFree
        return foreignIds.associateWith { foreignId -> known[foreignId] ?: next++ }
    }

    /** True when the backup came from this installation, or predates source tracking. */
    fun isFromSameSource(data: WardrobeImport, installationId: String): Boolean {
        return data.sourceId == null || data.sourceId == installationId
    }
}

/** Foreign id to local id, per table. */
data class IdMappings(
    val items: Map<Int, Int> = emptyMap(),
    val outfits: Map<Int, Int> = emptyMap(),
    val scheduledOutfits: Map<Int, Int> = emptyMap(),
)

/** First id that is free in each table. */
data class NextIds(val items: Int, val outfits: Int, val scheduledOutfits: Int)

data class RemapResult(val data: WardrobeImport, val mappings: IdMappings)

/**
 * The photo to store for an imported row. A file installed under the app image directory
 * is used. A `content://` URI is kept only when [trustContentUris] is set (the backup came
 * from this installation), because on another device it would point at nothing or at an
 * unrelated photo. Anything else (a path from another device or a missing bundled file) is
 * dropped.
 */
internal fun importedImageUri(
    imported: String?,
    importedIsLocal: Boolean,
    trustContentUris: Boolean,
): String? = when {
    importedIsLocal -> imported
    trustContentUris && imported != null && imported.startsWith("content://") -> imported
    else -> null
}

/**
 * Like [importedImageUri], but a merge keeps the photo already on this device when the
 * backup did not install a replacement file.
 */
internal fun mergeImageUri(
    existing: String?,
    imported: String?,
    importedIsLocal: Boolean,
    trustContentUris: Boolean,
): String? {
    if (importedIsLocal) return imported
    if (!existing.isNullOrBlank()) return existing
    return importedImageUri(imported, importedIsLocal, trustContentUris)
}

fun WardrobeImport.withResolvedImages(resolve: (String?) -> String?): WardrobeImport {
    return copy(
        wardrobeItems = wardrobeItems.map { item ->
            item.copy(imageUri = resolve(item.imageUri))
        },
        outfits = outfits.map { outfit ->
            outfit.copy(
                imageUriCombined = resolve(outfit.imageUriCombined),
                imageUriTeaser = resolve(outfit.imageUriTeaser),
            )
        },
    )
}
