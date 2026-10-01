package com.anleanja.wardrobe.database.daos

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.anleanja.wardrobe.database.entities.WardrobeItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WardrobeItemDao {
    @Query("SELECT * FROM WARDROBE_ITEMS")
    fun getAll(): Flow<List<WardrobeItem>>

    @Query("SELECT * FROM WARDROBE_ITEMS where id = :id")
    fun getById(id: Int): Flow<WardrobeItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WardrobeItem): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItem(item: WardrobeItem): Int

    @Query("DELETE FROM wardrobe_items WHERE id = :id")
    suspend fun deleteItem(id: Int)

    @Query("DELETE FROM wardrobe_items")
    suspend fun deleteAll()

    @Query("SELECT image_uri FROM wardrobe_items")
    suspend fun imageUris(): List<String>

    @Query("SELECT id, image_uri FROM wardrobe_items")
    suspend fun imageUrisById(): List<WardrobeItemImage>
}

data class WardrobeItemImage(
    val id: Int,
    @ColumnInfo(name = "image_uri") val imageUri: String?,
)