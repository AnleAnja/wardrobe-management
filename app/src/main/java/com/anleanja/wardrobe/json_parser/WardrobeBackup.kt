package com.anleanja.wardrobe.json_parser

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
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
    enum class Kind { CORRUPT, UNSUPPORTED }
}

object WardrobeBackup {
    const val LEGACY_VERSION = 1
    const val CURRENT_VERSION = 2
    const val JSON_ENTRY = "wardrobe.json"
    const val IMAGES_PREFIX = "images/"

    private val gson = Gson()

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

    fun extensionOf(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpeg", "jpg" -> "jpg"
            "png" -> "png"
            "webp" -> "webp"
            else -> "jpg"
        }
    }

    fun writeZip(output: OutputStream, json: String, writeImages: (ZipOutputStream) -> Unit) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_ENTRY))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            writeImages(zip)
        }
    }

    fun readZip(input: InputStream, onImage: (fileName: String, bytes: ByteArray) -> Unit): String {
        var json: String? = null
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name.replace('\\', '/')
                    if (name.split('/').any { it == ".." }) {
                        throw BackupFormatException(BackupFormatException.Kind.CORRUPT)
                    }
                    when {
                        name == JSON_ENTRY -> json = zip.readBytes().decodeToString()
                        else -> imageFileName(name)?.let { fileName ->
                            onImage(fileName, zip.readBytes())
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return json ?: throw BackupFormatException(BackupFormatException.Kind.CORRUPT)
    }
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
