package com.example.wardrobe.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.wardrobe.database.daos.OutfitDao
import com.example.wardrobe.database.daos.OutfitItemDao
import com.example.wardrobe.database.daos.ScheduledItemDao
import com.example.wardrobe.database.daos.ScheduledOutfitDao
import com.example.wardrobe.database.daos.WardrobeItemDao
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.OutfitItem
import com.example.wardrobe.database.entities.ScheduledOutfit
import com.example.wardrobe.database.entities.WardrobeItem
import com.example.wardrobe.database.entities.ScheduledItem

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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wardrobe_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}