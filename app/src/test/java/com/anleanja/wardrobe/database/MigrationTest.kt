package com.anleanja.wardrobe.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.anleanja.wardrobe.database.entities.ImportedId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun everyMigrationFromV1MatchesTheExportedSchema() {
        helper.createDatabase(TEST_DB, DatabaseMigrations.V1_SCHEMA_VERSION).close()

        helper.runMigrationsAndValidate(
            TEST_DB,
            DatabaseMigrations.CURRENT_SCHEMA_VERSION,
            true,
            *DatabaseMigrations.ALL,
        ).close()
    }

    @Test
    fun migratingFromV1KeepsExistingWardrobe() {
        helper.createDatabase(TEST_DB, DatabaseMigrations.V1_SCHEMA_VERSION).use { db ->
            seedV1Wardrobe { sql -> db.execSQL(sql) }
        }

        helper.runMigrationsAndValidate(
            TEST_DB,
            DatabaseMigrations.CURRENT_SCHEMA_VERSION,
            true,
            *DatabaseMigrations.ALL,
        ).use { db ->
            assertEquals(2L, db.count("wardrobe_items"))
            assertEquals(1L, db.count("outfits"))
            assertEquals(2L, db.count("outfit_items"))
            assertEquals(1L, db.count("scheduled_outfits"))
            assertEquals(1L, db.count("scheduled_items"))
            assertEquals(0L, db.count("imported_ids"))

            db.query("SELECT image_uri, times_worn FROM wardrobe_items WHERE id = 2").use { cursor ->
                cursor.moveToFirst()
                assertEquals("file:///data/images/skirt.jpg", cursor.getString(0))
                assertEquals(4, cursor.getInt(1))
            }
        }
    }

    @Test
    fun migratedDatabaseOpensWithRoomAndStoresImportedIds() = runTest {
        helper.createDatabase(TEST_DB, DatabaseMigrations.V1_SCHEMA_VERSION).use { db ->
            seedV1Wardrobe { sql -> db.execSQL(sql) }
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*DatabaseMigrations.ALL)
            .build()
        try {
            assertEquals(2, database.wardrobeItemDao().maxId())
            assertEquals(1, database.outfitDao().maxId())

            val mapping = ImportedId("other-phone", ImportedId.KIND_ITEM, foreignId = 7, localId = 2)
            database.importedIdDao().upsertAll(listOf(mapping))
            assertEquals(listOf(mapping), database.importedIdDao().forSource("other-phone"))
        } finally {
            database.close()
        }
    }

    private fun seedV1Wardrobe(exec: (String) -> Unit) {
        exec(
            "INSERT INTO wardrobe_items (id, image_uri, category, times_worn) " +
                "VALUES (1, 'file:///data/images/shirt.jpg', NULL, 2)"
        )
        exec(
            "INSERT INTO wardrobe_items (id, image_uri, category, times_worn) " +
                "VALUES (2, 'file:///data/images/skirt.jpg', NULL, 4)"
        )
        exec("INSERT INTO outfits (id, image_uri_teaser, times_worn) VALUES (1, 'file:///data/images/teaser.jpg', 1)")
        exec("INSERT INTO outfit_items (outfit_id, item_id) VALUES (1, 1)")
        exec("INSERT INTO outfit_items (outfit_id, item_id) VALUES (1, 2)")
        exec("INSERT INTO scheduled_outfits (id, outfit_id, date, temperature) VALUES (1, 1, 1767225600000, 18)")
        exec("INSERT INTO scheduled_items (scheduled_outfit_id, item_id) VALUES (1, 1)")
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.count(table: String): Long =
        query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
