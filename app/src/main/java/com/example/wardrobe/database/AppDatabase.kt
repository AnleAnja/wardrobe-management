package com.example.wardrobe.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.wardrobe.database.daos.OutfitDao
import com.example.wardrobe.database.daos.OutfitItemDao
import com.example.wardrobe.database.daos.ScheduledItemDao
import com.example.wardrobe.database.daos.ScheduledOutfitDao
import com.example.wardrobe.database.daos.WardrobeItemDao
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.OutfitItem
import com.example.wardrobe.database.entities.ScheduledItem
import com.example.wardrobe.database.entities.ScheduledOutfit
import com.example.wardrobe.database.entities.WardrobeItem

@Database(entities = [
    WardrobeItem::class,
    Outfit::class,
    OutfitItem::class,
    ScheduledOutfit::class,
    ScheduledItem::class
], version = 10)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wardrobeItemDao(): WardrobeItemDao
    abstract fun outfitDao(): OutfitDao
    abstract fun outfitItemDao(): OutfitItemDao
    abstract fun scheduledOutfitDao(): ScheduledOutfitDao
    abstract fun scheduledItemDao(): ScheduledItemDao
}
