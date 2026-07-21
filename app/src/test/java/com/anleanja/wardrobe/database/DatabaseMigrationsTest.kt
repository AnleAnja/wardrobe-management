package com.anleanja.wardrobe.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseMigrationsTest {

    @Test
    fun v1SchemaVersionIsFrozenAtTen() {
        assertEquals(10, DatabaseMigrations.CURRENT_SCHEMA_VERSION)
    }

    @Test
    fun v1HasNoRegisteredMigrationsYet() {
        assertTrue(DatabaseMigrations.ALL.isEmpty())
    }
}
