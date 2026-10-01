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
            )
        )
    }

    @Test
    fun newRowDropsUnusableBackupUri() {
        assertNull(
            mergeImageUri(
                existing = null,
                imported = "file:///other-device/item.jpg",
                importedIsLocal = false,
            )
        )
    }
}
