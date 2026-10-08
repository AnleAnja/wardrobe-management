package com.anleanja.wardrobe.storage

const val IMAGE_HEADER_BYTES = 12

private val HEIF_BRANDS = setOf("heic", "heix", "hevc", "hevx", "heim", "heis", "mif1", "msf1")
private val AVIF_BRANDS = setOf("avif", "avis")

/**
 * File extension for an image, recognised from its first [IMAGE_HEADER_BYTES] bytes, or null
 * when the bytes are not a known image. Photos saved before resizing existed were copied as
 * picked, so HEIC, AVIF, GIF, and BMP files can sit behind a `.jpg` name; they are identified
 * here without decoding, which older Android versions cannot do for HEIC.
 */
fun imageExtensionFor(header: ByteArray): String? {
    fun matches(offset: Int, vararg expected: Int): Boolean =
        header.size >= offset + expected.size &&
            expected.indices.all { header[offset + it] == expected[it].toByte() }

    fun ascii(offset: Int, length: Int): String? =
        if (header.size >= offset + length) String(header, offset, length, Charsets.US_ASCII) else null

    return when {
        matches(0, 0xFF, 0xD8, 0xFF) -> "jpg"
        matches(0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> "png"
        ascii(0, 4) == "RIFF" && ascii(8, 4) == "WEBP" -> "webp"
        ascii(0, 4) == "GIF8" -> "gif"
        ascii(0, 2) == "BM" -> "bmp"
        ascii(4, 4) == "ftyp" && ascii(8, 4) in HEIF_BRANDS -> "heic"
        ascii(4, 4) == "ftyp" && ascii(8, 4) in AVIF_BRANDS -> "avif"
        else -> null
    }
}
