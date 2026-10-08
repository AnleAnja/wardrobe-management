package com.anleanja.wardrobe.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores wardrobe photos in the app's internal filesDir so they survive in-place app
 * updates and don't depend on persistable content:// URI permissions.
 *
 * New photos are resized to a [MAX_IMAGE_EDGE_PX] long edge. Opaque photos are saved as JPEG
 * at [JPEG_QUALITY]; photos with transparency stay PNG so cut-outs still layer on the outfit
 * canvas. Stored as `file://` URIs because that's what Coil and the existing UI expect.
 * Legacy `content://` URIs in the DB keep working alongside these.
 */
@Singleton
class ImageStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val baseDir: File by lazy {
        File(context.filesDir, IMAGE_DIR).also { if (!it.exists()) it.mkdirs() }
    }

    /** Decode, downscale, and store [source]. Returns a `file://` URI string. */
    suspend fun saveImage(source: Uri): String? = withContext(Dispatchers.IO) {
        var temp: File? = null
        try {
            val sourceFile = File.createTempFile("wardrobe-src", ".img", context.cacheDir)
            temp = sourceFile
            context.contentResolver.openInputStream(source)?.use { input ->
                sourceFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            val decoded = decodeSampled(sourceFile) ?: return@withContext null
            try {
                writeEncoded(decoded, keepAlpha = hasTransparentPixels(decoded))
            } finally {
                if (!decoded.isRecycled) decoded.recycle()
            }
        } catch (e: Exception) {
            null
        } finally {
            temp?.delete()
        }
    }

    /** Encode a bitmap (for example a rendered outfit canvas) as a JPEG in filesDir. */
    suspend fun saveBitmap(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            writeEncoded(bitmap, keepAlpha = false)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decodes a stored photo (`file://` URI, plain path or legacy `content://` URI) upright and
     * downsampled to roughly [maxEdge]. Returns null when it can't be read.
     */
    suspend fun decodeImage(uriOrPath: String, maxEdge: Int = MAX_IMAGE_EDGE_PX): Bitmap? =
        withContext(Dispatchers.IO) {
            var temp: File? = null
            try {
                val file = if (uriOrPath.startsWith("content://")) {
                    File.createTempFile("wardrobe-decode", ".img", context.cacheDir).also { copy ->
                        temp = copy
                        context.contentResolver.openInputStream(uriOrPath.toUri())?.use { input ->
                            copy.outputStream().use { output -> input.copyTo(output) }
                        } ?: return@withContext null
                    }
                } else {
                    localPath(uriOrPath)?.let(::File) ?: return@withContext null
                }
                decodeSampled(file, maxEdge)
            } catch (e: Exception) {
                null
            } finally {
                temp?.delete()
            }
        }

    /**
     * Moves an already-encoded backup photo into the images dir without re-encoding it.
     * Returns null when [source] is not a recognised image (see [imageExtensionFor]);
     * I/O failures throw and leave nothing behind.
     */
    fun saveImportedImage(source: File): String? {
        val header = source.inputStream().use { input ->
            val buffer = ByteArray(IMAGE_HEADER_BYTES)
            buffer.copyOf(input.read(buffer).coerceAtLeast(0))
        }
        val extension = imageExtensionFor(header) ?: return null
        val out = File(baseDir, "${UUID.randomUUID()}.$extension")
        if (!source.renameTo(out)) {
            try {
                source.copyTo(out)
            } catch (e: Exception) {
                out.delete()
                throw e
            }
        }
        return Uri.fromFile(out).toString()
    }

    /** Delete a previously-saved local image. No-op for anything outside our images dir. */
    fun deleteImage(uriOrPath: String?): Boolean {
        val file = localImageFile(uriOrPath) ?: return false
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /** Free bytes on the volume that holds the images dir. */
    fun availableBytes(): Long = baseDir.usableSpace

    fun isLocalImage(uriOrPath: String?): Boolean = localImageFile(uriOrPath) != null

    fun localImageFile(uriOrPath: String?): File? {
        if (uriOrPath == null) return null
        return try {
            val path = localPath(uriOrPath) ?: return null
            val base = baseDir.canonicalPath
            val inside = path == base || path.startsWith(base + File.separator)
            if (!inside) return null
            File(path).takeIf { it.isFile }
        } catch (e: Exception) {
            null
        }
    }

    private fun writeEncoded(bitmap: Bitmap, keepAlpha: Boolean): String? {
        val extension = if (keepAlpha) "png" else "jpg"
        val out = File(baseDir, "${UUID.randomUUID()}.$extension")
        val encoded = bitmapForEncoding(bitmap, fillTransparency = !keepAlpha)
        return try {
            val wrote = out.outputStream().use { stream ->
                if (keepAlpha) {
                    encoded.compress(Bitmap.CompressFormat.PNG, 100, stream)
                } else {
                    encoded.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
                }
            }
            if (!wrote) {
                out.delete()
                null
            } else {
                Uri.fromFile(out).toString()
            }
        } catch (e: Exception) {
            out.delete()
            null
        } finally {
            if (encoded != bitmap && !encoded.isRecycled) encoded.recycle()
        }
    }

    /** [Bitmap.hasAlpha] is true for most decoded PNGs even when every pixel is opaque. */
    private fun hasTransparentPixels(bitmap: Bitmap): Boolean {
        if (!bitmap.hasAlpha()) return false
        val row = IntArray(bitmap.width)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            if (row.any { Color.alpha(it) != 0xFF }) return true
        }
        return false
    }

    private fun decodeSampled(file: File, maxEdge: Int = MAX_IMAGE_EDGE_PX): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeForMaxEdge(bounds.outWidth, bounds.outHeight, maxEdge)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        return applyExif(decoded, file)
    }

    private fun applyExif(bitmap: Bitmap, file: File): Bitmap {
        val orientation = ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (oriented != bitmap) bitmap.recycle()
        return oriented
    }

    /**
     * JPEG has no alpha, so when [fillTransparency] is set transparent pixels are drawn
     * on white instead of turning black.
     */
    private fun bitmapForEncoding(source: Bitmap, fillTransparency: Boolean): Bitmap {
        var current = if (source.config == Bitmap.Config.HARDWARE) {
            source.copy(Bitmap.Config.ARGB_8888, false) ?: return source
        } else {
            source
        }
        val (width, height) = fittedSize(current.width, current.height, MAX_IMAGE_EDGE_PX)
        if (width != current.width || height != current.height) {
            val scaled = Bitmap.createScaledBitmap(current, width, height, true)
            if (current != source) current.recycle()
            current = scaled
        }
        if (fillTransparency && current.hasAlpha()) {
            val opaque = Bitmap.createBitmap(current.width, current.height, Bitmap.Config.ARGB_8888)
            Canvas(opaque).apply {
                drawColor(Color.WHITE)
                drawBitmap(current, 0f, 0f, null)
            }
            if (current != source) current.recycle()
            current = opaque
        }
        return current
    }

    private fun localPath(uriOrPath: String): String? {
        val path = if (uriOrPath.startsWith("file://")) uriOrPath.toUri().path else uriOrPath
        return path?.let { File(it).canonicalPath }
    }

    companion object {
        private const val IMAGE_DIR = "wardrobe_images"
    }
}
