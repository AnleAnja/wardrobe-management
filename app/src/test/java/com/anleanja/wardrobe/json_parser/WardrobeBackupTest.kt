package com.anleanja.wardrobe.json_parser

import com.google.gson.Gson
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry

class WardrobeBackupTest {

    private val gson = Gson()

    @Test
    fun parse_acceptsLegacyJsonWithoutVersion() {
        val data = WardrobeBackup.parse(legacyJson())

        assertTrue(WardrobeBackup.isLegacy(data))
        assertEquals(1, data.wardrobeItems.size)
        assertNull(data.exportVersion)
    }

    @Test
    fun parse_rejectsCorruptAndUnsupportedFiles() {
        val corrupt = runCatching { WardrobeBackup.parse("{}") }
        assertEquals(
            BackupFormatException.Kind.CORRUPT,
            (corrupt.exceptionOrNull() as BackupFormatException).kind
        )

        val unsupported = runCatching { WardrobeBackup.parse(versionedJson(version = 99)) }
        assertEquals(
            BackupFormatException.Kind.UNSUPPORTED,
            (unsupported.exceptionOrNull() as BackupFormatException).kind
        )
    }

    @Test
    fun bundleImagePaths_reusesOneEntryPerLocalFile() {
        val bundled = WardrobeBackup.bundleImagePaths(
            references = listOf("file:///a.jpg", "file:///a.jpg", "content://remote", null),
            localFileName = { uri -> if (uri.startsWith("file://")) "a.jpg" else null },
        )

        assertEquals(mapOf("file:///a.jpg" to "images/a.jpg"), bundled)
    }

    @Test
    fun resolveImportedImage_rewritesBundledPathsAndDropsMissingOnes() {
        val saved = mapOf("item.jpg" to "file:///new/item.jpg")

        assertEquals(
            "file:///new/item.jpg",
            WardrobeBackup.resolveImportedImage("images/item.jpg", saved)
        )
        assertNull(WardrobeBackup.resolveImportedImage("images/missing.jpg", saved))
        assertEquals(
            "file:///legacy/item.jpg",
            WardrobeBackup.resolveImportedImage("file:///legacy/item.jpg", saved)
        )
    }

    @Test
    fun zipRoundTrip_keepsJsonAndImageBytes() {
        val image = byteArrayOf(1, 2, 3, 4)
        val output = ByteArrayOutputStream()
        WardrobeBackup.writeZip(output, versionedJson(version = 2)) { zip ->
            zip.putNextEntry(ZipEntry("images/item.jpg"))
            zip.write(image)
            zip.closeEntry()
        }
        val bytes = output.toByteArray()
        assertTrue(WardrobeBackup.isZip(bytes.copyOf(4)))

        val images = mutableMapOf<String, ByteArrayOutputStream>()
        val json = WardrobeBackup.readZip(ByteArrayInputStream(bytes)) { name ->
            ByteArrayOutputStream().also { images[name] = it }
        }
        val data = WardrobeBackup.parse(json)

        assertFalse(WardrobeBackup.isLegacy(data))
        assertEquals(2, data.exportVersion)
        assertEquals("images/item.jpg", data.wardrobeItems.first().imageUri)
        assertArrayEquals(image, images.getValue("item.jpg").toByteArray())
    }

    @Test
    fun readZip_rejectsPathTraversal() {
        val zip = zipOf("../secret.txt" to byteArrayOf(9))

        assertKind(BackupFormatException.Kind.CORRUPT) { readAll(zip) }
    }

    @Test
    fun readZip_rejectsDuplicateImageEntries() {
        val zip = zipOf("images/a.jpg" to byteArrayOf(1), "images\\a.jpg" to byteArrayOf(2))

        assertKind(BackupFormatException.Kind.CORRUPT) { readAll(zip) }
    }

    @Test
    fun readZip_rejectsMissingJson() {
        val output = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("images/a.jpg"))
            zip.write(byteArrayOf(1))
            zip.closeEntry()
        }

        assertKind(BackupFormatException.Kind.CORRUPT) { readAll(output.toByteArray()) }
    }

    @Test
    fun readZip_rejectsOversizedEntriesAndTotals() {
        val zip = zipOf("images/a.jpg" to ByteArray(64), "images/b.jpg" to ByteArray(64))

        assertKind(BackupFormatException.Kind.TOO_LARGE) {
            readAll(zip, BackupLimits(maxImageBytes = 32))
        }
        assertKind(BackupFormatException.Kind.NO_SPACE) {
            val jsonBytes = versionedJson(version = 2).toByteArray().size.toLong()
            readAll(zip, BackupLimits(maxTotalBytes = jsonBytes + 100))
        }
        assertKind(BackupFormatException.Kind.TOO_LARGE) {
            readAll(zip, BackupLimits(maxEntries = 2))
        }
        assertKind(BackupFormatException.Kind.TOO_LARGE) {
            readAll(zip, BackupLimits(maxJsonBytes = 10))
        }
    }

    @Test
    fun readJson_rejectsFilesOverTheLimit() {
        assertKind(BackupFormatException.Kind.TOO_LARGE) {
            WardrobeBackup.readJson(ByteArrayInputStream(ByteArray(11)), BackupLimits(maxJsonBytes = 10))
        }
    }

    @Test
    fun readHeader_keepsReadingWhenTheStreamReturnsOneByteAtATime() {
        val bytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14)
        val trickle = object : java.io.InputStream() {
            private var index = 0
            override fun read(): Int = if (index < bytes.size) bytes[index++].toInt() and 0xFF else -1
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (len == 0) return 0
                val next = read()
                if (next < 0) return -1
                b[off] = next.toByte()
                return 1
            }
        }

        assertTrue(WardrobeBackup.isZip(WardrobeBackup.readHeader(trickle)))
        assertEquals(2, WardrobeBackup.readHeader(ByteArrayInputStream(byteArrayOf(1, 2))).size)
    }

    @Test
    fun defaultLimitsFitFullSizeCameraPhotos() {
        val zip = zipOf("images/original.jpg" to ByteArray(60 * 1024 * 1024))

        readAll(zip)
    }

    @Test
    fun remapIds_assignsSequentialIdsAndKeepsLinksConsistent() {
        val data = sampleImport(version = 2, imageUri = "images/item.jpg").copy(
            outfits = listOf(OutfitJson(id = 1, imageUriCombined = null, imageUriTeaser = null, seasons = null)),
            outfitItems = listOf(OutfitItemJson(outfitId = 1, itemId = 1), OutfitItemJson(outfitId = 1, itemId = 99)),
            scheduledOutfits = listOf(
                ScheduledOutfitJson(id = 3, outfitId = 1, date = 10L, temperature = null),
                ScheduledOutfitJson(id = 4, outfitId = 42, date = 11L, temperature = null),
            ),
            scheduledItems = listOf(ScheduledItemJson(scheduledOutfitId = 3, itemId = 1)),
        )

        val result = WardrobeBackup.remapIds(data, IdMappings(), NextIds(items = 11, outfits = 21, scheduledOutfits = 31))
        val remapped = result.data

        assertEquals(listOf(11), remapped.wardrobeItems.map { it.id })
        assertEquals(listOf(21), remapped.outfits.map { it.id })
        assertEquals(listOf(OutfitItemJson(outfitId = 21, itemId = 11)), remapped.outfitItems)
        assertEquals(
            listOf(ScheduledOutfitJson(id = 31, outfitId = 21, date = 10L, temperature = null)),
            remapped.scheduledOutfits
        )
        assertEquals(listOf(ScheduledItemJson(scheduledOutfitId = 31, itemId = 11)), remapped.scheduledItems)
        assertEquals(IdMappings(items = mapOf(1 to 11), outfits = mapOf(1 to 21), scheduledOutfits = mapOf(3 to 31)), result.mappings)
    }

    @Test
    fun remapIds_reusesKnownMappingsAndHandlesOddIds() {
        val item = sampleImport(version = 2, imageUri = "images/item.jpg").wardrobeItems.first()
        val data = sampleImport(version = 2, imageUri = "images/item.jpg").copy(
            wardrobeItems = listOf(
                item.copy(id = 0),
                item.copy(id = -5),
                item.copy(id = 7, category = "first"),
                item.copy(id = 7, category = "duplicate"),
            ),
        )

        val result = WardrobeBackup.remapIds(
            data,
            known = IdMappings(items = mapOf(7 to 3)),
            nextFree = NextIds(items = 10, outfits = 1, scheduledOutfits = 1),
        )

        assertEquals(listOf(10, 11, 3), result.data.wardrobeItems.map { it.id })
        assertEquals("first", result.data.wardrobeItems.last().category)
        assertEquals(mapOf(0 to 10, -5 to 11, 7 to 3), result.mappings.items)
    }

    @Test
    fun isFromSameSource_treatsUntaggedBackupsAsLocal() {
        val untagged = sampleImport(version = 2, imageUri = "images/item.jpg")

        assertTrue(WardrobeBackup.isFromSameSource(untagged, "this-install"))
        assertTrue(WardrobeBackup.isFromSameSource(untagged.copy(sourceId = "this-install"), "this-install"))
        assertFalse(WardrobeBackup.isFromSameSource(untagged.copy(sourceId = "other"), "this-install"))
    }

    private fun zipOf(vararg images: Pair<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        WardrobeBackup.writeZip(output, versionedJson(version = 2)) { zip ->
            images.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun readAll(zip: ByteArray, limits: BackupLimits = BackupLimits()): String {
        return WardrobeBackup.readZip(ByteArrayInputStream(zip), limits) { java.io.OutputStream.nullOutputStream() }
    }

    private fun assertKind(expected: BackupFormatException.Kind, block: () -> Unit) {
        val error = runCatching(block).exceptionOrNull()
        assertEquals(expected, (error as? BackupFormatException)?.kind)
    }

    private fun legacyJson(): String {
        return gson.toJson(sampleImport(version = null, imageUri = "file:///item.jpg"))
    }

    private fun versionedJson(version: Int): String {
        return gson.toJson(sampleImport(version = version, imageUri = "images/item.jpg"))
    }

    private fun sampleImport(version: Int?, imageUri: String) = WardrobeImport(
        wardrobeItems = listOf(
            WardrobeItemJson(
                id = 1,
                imageUri = imageUri,
                category = "Tops",
                subcategory = null,
                rating = null,
                price = null,
                purchaseDate = null,
                seasons = null,
                timesWorn = 0,
                lastWorn = null,
            )
        ),
        outfits = emptyList(),
        outfitItems = emptyList(),
        scheduledOutfits = emptyList(),
        exportVersion = version,
    )
}
