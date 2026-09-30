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

        val images = mutableMapOf<String, ByteArray>()
        val json = WardrobeBackup.readZip(ByteArrayInputStream(bytes)) { name, entry ->
            images[name] = entry
        }
        val data = WardrobeBackup.parse(json)

        assertFalse(WardrobeBackup.isLegacy(data))
        assertEquals(2, data.exportVersion)
        assertEquals("images/item.jpg", data.wardrobeItems.first().imageUri)
        assertArrayEquals(image, images["item.jpg"])
    }

    @Test
    fun readZip_rejectsPathTraversal() {
        val output = ByteArrayOutputStream()
        WardrobeBackup.writeZip(output, versionedJson(version = 2)) { zip ->
            zip.putNextEntry(ZipEntry("../secret.txt"))
            zip.write(byteArrayOf(9))
            zip.closeEntry()
        }

        val result = runCatching {
            WardrobeBackup.readZip(ByteArrayInputStream(output.toByteArray())) { _, _ -> }
        }
        assertEquals(
            BackupFormatException.Kind.CORRUPT,
            (result.exceptionOrNull() as BackupFormatException).kind
        )
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
