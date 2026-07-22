# Room database migrations

## v1.0 schema freeze

Wardrobe v1.0 ships with Room schema version **10**. Do not bump the database version for the first public release unless you add a matching migration.

Current entities:

- `WardrobeItem`
- `Outfit`
- `OutfitItem`
- `ScheduledOutfit`
- `ScheduledItem`

## Debug vs release behavior

- **Debug builds** use `fallbackToDestructiveMigration` so local schema experiments reset cleanly.
- **Release builds** register migrations from [`DatabaseMigrations.kt`](../app/src/main/java/com/anleanja/wardrobe/database/DatabaseMigrations.kt).

## Adding a migration after v1.0

1. Bump `@Database(version = …)` in `AppDatabase.kt`.
2. Add a `Migration` object in `DatabaseMigrations.kt`.
3. Register it in `DatabaseMigrations.ALL`.
4. Test upgrading from a v1.0 install before publishing.

Example:

```kotlin
private val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE wardrobe_items ADD COLUMN notes TEXT")
    }
}
```

## Export schema (recommended)

Consider enabling Room schema export in a future release to diff database changes during review:

```kotlin
@Database(..., exportSchema = true)
```
