package com.example.wardrobe.database.daos
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.OutfitItem
import com.example.wardrobe.database.entities.WardrobeItem
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitItemDao {
    @Query("SELECT * FROM OUTFIT_ITEMS")
    fun getAll(): Flow<List<OutfitItem>>

    @Query("SELECT * FROM OUTFIT_ITEMS where outfit_id = :outfitId and item_id = :itemId")
    fun getById(outfitId: Int, itemId: Int): Flow<OutfitItem>

    @Query("""
        SELECT w.* FROM wardrobe_items w
        INNER JOIN outfit_items oi ON w.id = oi.item_id
        WHERE oi.outfit_id = :outfitId
    """)
    fun getItemsForOutfit(outfitId: Int): Flow<List<WardrobeItem>>

    @Query("""
        SELECT o.* FROM outfits o
        INNER JOIN outfit_items oi ON o.id = oi.outfit_id
        WHERE oi.item_id = :itemId
    """)
    fun getOutfitsForItem(itemId: Int): Flow<List<Outfit>>


    @Query("DELETE FROM outfit_items WHERE outfit_id = :outfitId")
    suspend fun deleteAllItemsForOutfit(outfitId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: OutfitItem): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItem(item: OutfitItem): Int

    @Query("DELETE FROM OUTFIT_ITEMS WHERE outfit_id = :outfitId and item_id = :itemId")
    suspend fun deleteItem(outfitId: Int, itemId: Int)
}