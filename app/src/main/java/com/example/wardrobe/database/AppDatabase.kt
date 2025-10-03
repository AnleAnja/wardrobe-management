package com.example.wardrobe.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.wardrobe.database.daos.WardrobeItemDao
import com.example.wardrobe.database.entities.WardrobeItem

@Database(entities = [WardrobeItem::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wardrobeItemDao(): WardrobeItemDao
}