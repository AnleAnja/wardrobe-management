package com.anleanja.wardrobe.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageFormatTest {

    @Test
    fun recognisesCommonCameraAndWebFormats() {
        assertEquals("jpg", imageExtensionFor(bytes(0xFF, 0xD8, 0xFF, 0xE0)))
        assertEquals("png", imageExtensionFor(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
        assertEquals("webp", imageExtensionFor("RIFF\u0000\u0000\u0000\u0000WEBP".toByteArray(Charsets.US_ASCII)))
        assertEquals("gif", imageExtensionFor("GIF89a".toByteArray(Charsets.US_ASCII)))
        assertEquals("bmp", imageExtensionFor("BM\u0000\u0000".toByteArray(Charsets.US_ASCII)))
    }

    @Test
    fun recognisesHeicAndAvifFromTheirBrand() {
        assertEquals("heic", imageExtensionFor(isoBox("heic")))
        assertEquals("heic", imageExtensionFor(isoBox("mif1")))
        assertEquals("avif", imageExtensionFor(isoBox("avif")))
        assertNull(imageExtensionFor(isoBox("mp42")))
    }

    @Test
    fun rejectsNonImagesAndShortHeaders() {
        assertNull(imageExtensionFor("hello world!".toByteArray(Charsets.US_ASCII)))
        assertNull(imageExtensionFor(bytes(0xFF, 0xD8)))
        assertNull(imageExtensionFor(ByteArray(0)))
    }

    private fun isoBox(brand: String): ByteArray =
        bytes(0x00, 0x00, 0x00, 0x18) + "ftyp$brand".toByteArray(Charsets.US_ASCII)

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
}
