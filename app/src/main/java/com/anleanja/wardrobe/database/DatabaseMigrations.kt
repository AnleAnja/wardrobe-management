package com.anleanja.wardrobe.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room schema migrations for post-v1 releases.
 *
 * v1.0 ships with schema version 10 frozen. Before changing entities or bumping
 * [AppDatabase] version, add a migration here and register it in [DatabaseModule].
 */
object DatabaseMigrations {
    const val CURRENT_SCHEMA_VERSION = 10

    val ALL: Array<Migration> = arrayOf(
        // Example for a future release:
        // MIGRATION_10_11,
    )

    @Suppress("unused")
    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add SQL statements when schema version 11 is introduced.
        }
    }
}
