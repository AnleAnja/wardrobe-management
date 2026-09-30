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
 * New photos are resized to a [MAX_IMAGE_EDGE_PX] long edge and saved as JPEG at
 * [JPEG_QUALITY]. Stored as `file://` URIs because that's what Coil and the existing UI expect.
 * Legacy `content://` URIs in the DB keep working alongside these.
 */
@Singleton
class ImageStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val baseDir: File by lazy {
        File(context.filesDir, IMAGE_DIR).also { if (!it.exists()) it.mkdirs() }
    }

    /** Decode, downscale, and store [source] as a JPEG. Returns a `file://` URI string. */
    suspend fun saveImage(source: Uri): String? = withContext(Dispatchers.IO) {
        val temp = File.createTempFile("wardrobe-src", ".img", context.cacheDir)
        try {
            context.contentResolver.openInputStream(source)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            val decoded = decodeSampled(temp) ?: return@withContext null
            try {
                writeJpeg(decoded)
            } finally {
                if (!decoded.isRecycled) decoded.recycle()
            }
        } catch (e: Exception) {
            null
        } finally {
            temp.delete()
        }
    }

    /** Encode a bitmap (for example a rendered outfit canvas) as a JPEG in filesDir. */
    suspend fun saveBitmap(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            writeJpeg(bitmap)
        } catch (e: Exception) {
            null
        }
    }

    /** Store already-encoded backup bytes without resizing them again. */
    fun saveBytes(bytes: ByteArray, extension: String): String? {
        val ext = extension.lowercase().removePrefix(".")
        if (ext !in ALLOWED_EXTENSIONS || bytes.isEmpty()) return null
        return try {
            val out = File(baseDir, "${UUID.randomUUID()}.$ext")
            out.writeBytes(bytes)
            Uri.fromFile(out).toString()
        } catch (e: Exception) {
            null
        }
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

    /** Deletes files in the images dir that are not in [referenced]. */
    fun deleteUnreferenced(referenced: Set<String>) {
        val keep = referenced.mapNotNull { uri -> localImageFile(uri)?.canonicalPath }.toSet()
        baseDir.listFiles().orEmpty().forEach { file ->
            if (file.isFile && file.canonicalPath !in keep) {
                file.delete()
            }
        }
    }

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

    private fun writeJpeg(bitmap: Bitmap): String? {
        val out = File(baseDir, "${UUID.randomUUID()}.jpg")
        val encoded = bitmapForJpeg(bitmap)
        return try {
            val wrote = out.outputStream().use { stream ->
                encoded.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
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

    private fun decodeSampled(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeForMaxEdge(bounds.outWidth, bounds.outHeight, MAX_IMAGE_EDGE_PX)
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
     * JPEG has no alpha. Canvas snapshots are drawn on white so transparent pixels
     * don't turn black when the PNG is replaced.
     */
    private fun bitmapForJpeg(source: Bitmap): Bitmap {
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
        if (current.hasAlpha()) {
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
        private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    }
}
