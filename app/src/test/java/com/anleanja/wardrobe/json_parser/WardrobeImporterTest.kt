package com.anleanja.wardrobe.json_parser

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.anleanja.wardrobe.R
import com.anleanja.wardrobe.database.AppDatabase
import com.anleanja.wardrobe.database.entities.Outfit
import com.anleanja.wardrobe.database.entities.OutfitItem
import com.anleanja.wardrobe.database.entities.ScheduledItem
import com.anleanja.wardrobe.database.entities.ScheduledOutfit
import com.anleanja.wardrobe.database.entities.WardrobeItem
import com.anleanja.wardrobe.storage.ImageStorage
import com.anleanja.wardrobe.storage.InstallationId
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WardrobeImporterTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var storage: ImageStorage
    private lateinit var installationId: InstallationId
    private lateinit var importer: WardrobeImporter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        storage = ImageStorage(context)
        installationId = InstallationId(context)
        importer = WardrobeImporter(context, db, storage, installationId)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun mergeFromThisDeviceUpdatesRowsWithoutLosingOutfitLinksOrCalendar() = runBlocking {
        seedLinkedWardrobe()
        val backup = backup(
            sourceId = installationId.value,
            items = listOf(itemJson(id = 1, category = "Updated")),
            outfits = listOf(outfitJson(id = 1)),
        )

        importer.importBackup(backup, ImportMode.MERGE).getOrThrow()

        assertEquals("Updated", db.wardrobeItemDao().getAll().first().single().category)
        assertEquals(listOf(OutfitItem(outfitId = 1, itemId = 1)), db.outfitItemDao().getAll().first())
        assertEquals(1, db.scheduledOutfitDao().getAll().first().size)
        assertEquals(listOf(ScheduledItem(scheduledOutfitId = 1, itemId = 1)), db.scheduledItemDao().getAll().first())
    }

    @Test
    fun mergingAnotherDevicesBackupTwiceAddsItOnceAndLeavesLocalRowsAlone() = runBlocking {
        db.wardrobeItemDao().insertItem(WardrobeItem(id = 1, category = "Mine"))
        val backup = backup(
            sourceId = "other-device",
            items = listOf(itemJson(id = 1, category = "Theirs")),
            outfits = listOf(outfitJson(id = 1)),
            outfitItems = listOf(OutfitItemJson(outfitId = 1, itemId = 1)),
        )

        importer.importBackup(backup, ImportMode.MERGE).getOrThrow()
        importer.importBackup(backup, ImportMode.MERGE).getOrThrow()

        val items = db.wardrobeItemDao().getAll().first().sortedBy { it.id }
        assertEquals(listOf("Mine", "Theirs"), items.map { it.category })
        assertEquals(1, items.first().id)
        assertEquals(1, db.outfitDao().getAll().first().size)
        val links = db.outfitItemDao().getAll().first()
        assertEquals(listOf(items.last().id), links.map { it.itemId })
    }

    @Test
    fun replaceInstallsHeicPhotosAndDeletesOnlyThePhotosItReplaced() = runBlocking {
        val oldPhoto = storeLocalImage(JPEG_HEADER)
        val draftPhoto = storeLocalImage(JPEG_HEADER)
        db.wardrobeItemDao().insertItem(WardrobeItem(id = 1, imageUri = oldPhoto, category = "Old"))
        val backup = backup(
            sourceId = installationId.value,
            items = listOf(itemJson(id = 1, category = "New", imageUri = "images/photo.jpg")),
            images = mapOf("images/photo.jpg" to HEIC_HEADER),
        )

        importer.importBackup(backup, ImportMode.REPLACE).getOrThrow()

        val restored = db.wardrobeItemDao().getAll().first().single()
        val restoredFile = storage.localImageFile(restored.imageUri)
        assertTrue(restoredFile != null && restoredFile.name.endsWith(".heic"))
        assertFalse(storage.isLocalImage(oldPhoto))
        assertTrue(storage.isLocalImage(draftPhoto))
    }

    @Test
    fun anotherDevicesContentUrisAreDroppedButThisDevicesAreKept() = runBlocking {
        val foreign = backup(
            sourceId = "other-device",
            items = listOf(itemJson(id = 1, category = "Foreign", imageUri = "content://media/picker/1")),
        )
        val local = backup(
            sourceId = installationId.value,
            items = listOf(itemJson(id = 50, category = "Local", imageUri = "content://media/picker/2")),
        )

        importer.importBackup(foreign, ImportMode.MERGE).getOrThrow()
        importer.importBackup(local, ImportMode.MERGE).getOrThrow()

        val byCategory = db.wardrobeItemDao().getAll().first().associateBy { it.category }
        assertNull(byCategory.getValue("Foreign").imageUri)
        assertEquals("content://media/picker/2", byCategory.getValue("Local").imageUri)
    }

    @Test
    fun revokedReadGrantReportsThatTheFileCouldNotBeRead() = runBlocking {
        val uri = Uri.parse("content://com.example.documents/backup.zip")
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) {
            throw SecurityException("Permission Denial")
        }

        val result = importer.importBackup(uri, ImportMode.MERGE)

        assertEquals(context.getString(R.string.error_could_not_read_file), result.exceptionOrNull()?.message)
    }

    private suspend fun seedLinkedWardrobe() {
        db.wardrobeItemDao().insertItem(WardrobeItem(id = 1, category = "Original"))
        db.outfitDao().insertOutfit(Outfit(id = 1))
        db.outfitItemDao().insertItem(OutfitItem(outfitId = 1, itemId = 1))
        db.scheduledOutfitDao().insertOutfit(ScheduledOutfit(id = 1, outfitId = 1, date = 1L, temperature = null))
        db.scheduledItemDao().insertItem(ScheduledItem(scheduledOutfitId = 1, itemId = 1))
    }

    private fun storeLocalImage(header: ByteArray): String {
        val file = File(context.cacheDir, "seed-${UUID.randomUUID()}").apply { writeBytes(header + ByteArray(32)) }
        return requireNotNull(storage.saveImportedImage(file))
    }

    private fun backup(
        sourceId: String,
        items: List<WardrobeItemJson> = emptyList(),
        outfits: List<OutfitJson> = emptyList(),
        outfitItems: List<OutfitItemJson> = emptyList(),
        images: Map<String, ByteArray> = emptyMap(),
    ): Uri {
        val data = WardrobeImport(
            wardrobeItems = items,
            outfits = outfits,
            outfitItems = outfitItems,
            scheduledOutfits = emptyList(),
            scheduledItems = emptyList(),
            exportVersion = WardrobeBackup.CURRENT_VERSION,
            sourceId = sourceId,
        )
        val file = File(context.cacheDir, "backup-${UUID.randomUUID()}.zip")
        file.outputStream().use { output ->
            WardrobeBackup.writeZip(output, Gson().toJson(data)) { zip ->
                images.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        }
        return Uri.fromFile(file)
    }

    private fun itemJson(id: Int, category: String, imageUri: String? = null) = WardrobeItemJson(
        id = id,
        imageUri = imageUri,
        category = category,
        rating = null,
        price = null,
        purchaseDate = null,
        seasons = null,
        timesWorn = 0,
        lastWorn = null,
    )

    private fun outfitJson(id: Int) = OutfitJson(id = id, imageUriCombined = null, imageUriTeaser = null, seasons = null)

    private companion object {
        val JPEG_HEADER = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val HEIC_HEADER = byteArrayOf(0, 0, 0, 0x18) + "ftypheic".toByteArray(Charsets.US_ASCII) + ByteArray(32)
    }
}
