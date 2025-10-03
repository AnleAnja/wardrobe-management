package com.example.wardrobe.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.wardrobe.database.entities.WardrobeItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WardrobeItemDao {
    @Query("SELECT * FROM WARDROBE_ITEMS")
    fun getAll(): Flow<List<WardrobeItem>>

    @Query("SELECT * FROM WARDROBE_ITEMS where id = :id")
    fun getById(id: Int): Flow<WardrobeItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WardrobeItem): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItem(item: WardrobeItem): Int

    @Delete
    suspend fun deleteItem(item: WardrobeItem)
}