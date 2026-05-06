package com.example.wardrobe.database.daos
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.wardrobe.database.entities.Outfit
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitDao {
    @Query("SELECT * FROM OUTFITS")
    fun getAll(): Flow<List<Outfit>>

    @Query("SELECT * FROM OUTFITS where id = :id")
    fun getById(id: Int): Flow<Outfit?>

    @Query("SELECT * FROM outfits WHERE id IN (:outfitIds)")
    fun getOutfitsByIds(outfitIds: List<Int>): Flow<List<Outfit>>

    @Query("""
        SELECT o.* FROM outfits o
        INNER JOIN scheduled_outfits so ON o.id = so.outfit_id
        WHERE so.id = :scheduledOutfitId
    """)
    fun getOutfitByScheduledId(scheduledOutfitId: Int): Flow<Outfit?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutfit(outfit: Outfit): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateOutfit(outfit: Outfit): Int

    @Query("DELETE FROM OUTFITS WHERE id = :id")
    suspend fun deleteOutfit(id: Int)
}