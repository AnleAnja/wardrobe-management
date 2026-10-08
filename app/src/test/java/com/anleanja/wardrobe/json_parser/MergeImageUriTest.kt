package com.anleanja.wardrobe.json_parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MergeImageUriTest {

    @Test
    fun localImportedFileReplacesExistingPhoto() {
        assertEquals(
            "file:///wardrobe_images/new.jpg",
            mergeImageUri(
                existing = "file:///wardrobe_images/old.jpg",
                imported = "file:///wardrobe_images/new.jpg",
                importedIsLocal = true,
                trustContentUris = true,
            )
        )
    }

    @Test
    fun unusableBackupKeepsExistingContentUri() {
        assertEquals(
            "content://media/item",
            mergeImageUri(
                existing = "content://media/item",
                imported = "file:///other-device/item.jpg",
                importedIsLocal = false,
                trustContentUris = false,
            )
        )
    }

    @Test
    fun missingBackupPhotoKeepsExistingLocalFile() {
        assertEquals(
            "file:///wardrobe_images/current.jpg",
            mergeImageUri(
                existing = "file:///wardrobe_images/current.jpg",
                imported = null,
                importedIsLocal = false,
                trustContentUris = true,
            )
        )
    }

    @Test
    fun newRowKeepsImportedLocalFile() {
        assertEquals(
            "file:///wardrobe_images/new.jpg",
            mergeImageUri(
                existing = null,
                imported = "file:///wardrobe_images/new.jpg",
                importedIsLocal = true,
                trustContentUris = false,
            )
        )
    }

    @Test
    fun contentUriIsKeptOnlyForThisDevicesBackups() {
        assertEquals(
            "content://media/item",
            mergeImageUri(existing = null, imported = "content://media/item", importedIsLocal = false, trustContentUris = true)
        )
        assertEquals(
            "content://media/item",
            importedImageUri(imported = "content://media/item", importedIsLocal = false, trustContentUris = true)
        )
        assertNull(
            mergeImageUri(existing = null, imported = "content://media/item", importedIsLocal = false, trustContentUris = false)
        )
        assertNull(
            importedImageUri(imported = "content://media/item", importedIsLocal = false, trustContentUris = false)
        )
    }

    @Test
    fun importedImageUriDropsPathsFromOtherDevices() {
        assertNull(importedImageUri(imported = "file:///other-device/item.jpg", importedIsLocal = false, trustContentUris = true))
        assertNull(importedImageUri(imported = null, importedIsLocal = false, trustContentUris = true))
    }

    @Test
    fun newRowDropsUnusableBackupUri() {
        assertNull(
            mergeImageUri(
                existing = null,
                imported = "file:///other-device/item.jpg",
                importedIsLocal = false,
                trustContentUris = true,
            )
        )
    }
}
