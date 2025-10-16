package com.example.wardrobe.database.daos
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.wardrobe.database.entities.OutfitItem
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitItemDao {
    @Query("SELECT * FROM OUTFIT_ITEMS")
    fun getAll(): Flow<List<OutfitItem>>

    @Query("SELECT * FROM OUTFIT_ITEMS where outfit_id = :outfitId and item_id = :itemId")
    fun getById(outfitId: Int, itemId: Int): Flow<OutfitItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: OutfitItem): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItem(item: OutfitItem): Int

    @Query("DELETE FROM OUTFIT_ITEMS WHERE outfit_id = :outfitId and item_id = :itemId")
    suspend fun deleteItem(outfitId: Int, itemId: Int)
}