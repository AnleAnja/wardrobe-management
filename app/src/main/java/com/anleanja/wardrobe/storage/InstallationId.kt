package com.anleanja.wardrobe.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Random id for this wardrobe, written into backups so a merge can tell where they came from.
 *
 * Kept in filesDir rather than shared preferences so Android backup and device transfer
 * restore it together with the database it describes. Written synchronously and atomically
 * because an export may embed it immediately.
 */
@Singleton
class InstallationId @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val value: String by lazy {
        val file = File(context.filesDir, FILE_NAME)
        file.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
            ?: UUID.randomUUID().toString().also { id -> writeAtomically(file, id) }
    }

    private fun writeAtomically(target: File, id: String) {
        val temp = File(target.parentFile, "$FILE_NAME.tmp")
        temp.outputStream().use { stream ->
            stream.write(id.toByteArray(Charsets.UTF_8))
            stream.fd.sync()
        }
        if (!temp.renameTo(target)) {
            temp.delete()
            target.writeText(id)
        }
    }

    private companion object {
        const val FILE_NAME = "installation_id"
    }
}
