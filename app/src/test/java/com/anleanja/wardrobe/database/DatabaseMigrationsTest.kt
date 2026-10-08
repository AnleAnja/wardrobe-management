package com.anleanja.wardrobe.database

import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseMigrationsTest {

    @Test
    fun v1SchemaVersionStaysAtTen() {
        assertEquals(10, DatabaseMigrations.V1_SCHEMA_VERSION)
    }

    @Test
    fun migrationsCoverEveryVersionSinceV1() {
        val steps = DatabaseMigrations.ALL.map { it.startVersion to it.endVersion }
        val expected = (DatabaseMigrations.V1_SCHEMA_VERSION until DatabaseMigrations.CURRENT_SCHEMA_VERSION)
            .map { it to it + 1 }
        assertEquals(expected, steps)
    }
}
