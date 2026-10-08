# Room database migrations

## Schema versions

| App release | Room schema version | Change |
|-------------|---------------------|--------|
| v1.0        | 10                  | Initial public schema |
| v1.1        | 11                  | Adds `imported_ids` (maps merged backup rows from other installations to local rows) |

Current entities:

- `WardrobeItem`
- `Outfit`
- `OutfitItem`
- `ScheduledOutfit`
- `ScheduledItem`
- `ImportedId`

## Debug vs release behavior

- **Debug builds** use `fallbackToDestructiveMigration` so local schema experiments reset cleanly.
- **Release builds** register migrations from [`DatabaseMigrations.kt`](app/src/main/java/com/anleanja/wardrobe/database/DatabaseMigrations.kt).

Because debug builds wipe the database instead of migrating, test real upgrades with the unit tests below or with release builds.

## Exported schemas

Room writes every schema version to [`app/schemas/`](app/schemas/com.anleanja.wardrobe.database.AppDatabase) during the build (`room.schemaLocation` in `app/build.gradle.kts`). Commit the new JSON file whenever the schema changes, and never edit or delete files for released versions: the migration tests build old databases from them.

## Adding a migration

1. Bump `DatabaseMigrations.CURRENT_SCHEMA_VERSION` (used by `@Database(version = …)`).
2. Add a `Migration` object in `DatabaseMigrations.kt` and register it in `DatabaseMigrations.ALL`.
3. Build once so Room exports the new schema JSON, then commit it.
4. Extend [`MigrationTest`](app/src/test/java/com/anleanja/wardrobe/database/MigrationTest.kt) with data that the migration must keep.
5. Run `./gradlew :app:testDebugUnitTest`.

Example:

```kotlin
private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE wardrobe_items ADD COLUMN notes TEXT")
    }
}
```

## Migration tests

`MigrationTest` runs on the JVM with Robolectric and Room's `MigrationTestHelper`:

- creates a v1.0 (schema 10) database and validates every migration against the exported schema of the current version;
- checks that wardrobe items, outfits and calendar entries survive the upgrade;
- opens the migrated file with the real `AppDatabase` and the release migrations.

`DatabaseMigrationsTest` additionally checks that `DatabaseMigrations.ALL` covers every step from schema 10 to the current version.
