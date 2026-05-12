package com.example.wardrobe.storage

import android.content.Context
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
 * Copies picked images into the app's internal filesDir so they survive in-place app
 * updates and don't depend on persistable content:// URI permissions (which the modern
 * photo picker doesn't always grant).
 *
 * Stored as `file://` URIs because that's what Coil and the existing UI expect.
 * Legacy `content://` URIs in the DB keep working alongside these.
 */
@Singleton
class ImageStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val baseDir: File by lazy {
        File(context.filesDir, IMAGE_DIR).also { if (!it.exists()) it.mkdirs() }
    }

    /** Copy the given source URI into filesDir and return a `file://` URI string. */
    suspend fun saveImage(source: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val out = File(baseDir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(source)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            Uri.fromFile(out).toString()
        } catch (e: Exception) {
            null
        }
    }

    /** Delete a previously-saved local image. Safe to call with any string; no-op if it
     *  isn't one of ours. Returns true if a file was deleted. */
    fun deleteImage(uriOrPath: String?): Boolean {
        if (uriOrPath == null || !isLocalImage(uriOrPath)) return false
        return try {
            val path = uriOrPath.toUri().path ?: return false
            File(path).takeIf { it.exists() }?.delete() ?: false
        } catch (e: Exception) {
            false
        }
    }

    /** True if this URI/path points at a file under our internal images dir. */
    fun isLocalImage(uriOrPath: String?): Boolean {
        if (uriOrPath == null) return false
        return try {
            val path = if (uriOrPath.startsWith("file://")) {
                uriOrPath.toUri().path
            } else {
                uriOrPath
            } ?: return false
            File(path).canonicalPath.startsWith(baseDir.canonicalPath)
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val IMAGE_DIR = "wardrobe_images"
    }
}
