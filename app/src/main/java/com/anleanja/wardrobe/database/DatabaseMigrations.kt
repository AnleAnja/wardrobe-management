package com.anleanja.wardrobe.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room schema migrations for post-v1 releases.
 *
 * v1.0 shipped with schema version 10. Before changing entities or bumping
 * [AppDatabase] version, add a migration here; [DatabaseModule] registers [ALL].
 */
object DatabaseMigrations {
    const val V1_SCHEMA_VERSION = 10
    const val CURRENT_SCHEMA_VERSION = 11

    internal const val CREATE_IMPORTED_IDS =
        "CREATE TABLE IF NOT EXISTS `imported_ids` (" +
            "`source_id` TEXT NOT NULL, " +
            "`kind` TEXT NOT NULL, " +
            "`foreign_id` INTEGER NOT NULL, " +
            "`local_id` INTEGER NOT NULL, " +
            "PRIMARY KEY(`source_id`, `kind`, `foreign_id`))"

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(CREATE_IMPORTED_IDS)
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_10_11,
    )
}
