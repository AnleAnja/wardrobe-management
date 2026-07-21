package com.anleanja.wardrobe.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.anleanja.wardrobe.database.daos.OutfitDao
import com.anleanja.wardrobe.database.daos.OutfitItemDao
import com.anleanja.wardrobe.database.daos.ScheduledItemDao
import com.anleanja.wardrobe.database.daos.ScheduledOutfitDao
import com.anleanja.wardrobe.database.daos.WardrobeItemDao
import com.anleanja.wardrobe.database.entities.Outfit
import com.anleanja.wardrobe.database.entities.OutfitItem
import com.anleanja.wardrobe.database.entities.ScheduledItem
import com.anleanja.wardrobe.database.entities.ScheduledOutfit
import com.anleanja.wardrobe.database.entities.WardrobeItem

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
