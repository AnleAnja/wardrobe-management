package com.example.wardrobe.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.wardrobe.database.entities.ScheduledOutfit
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledOutfitDao {
    @Query("SELECT * FROM SCHEDULED_OUTFITS")
    fun getAll(): Flow<List<ScheduledOutfit>>

    @Query("SELECT * FROM SCHEDULED_OUTFITS where id = :id")
    fun getById(id: Int): Flow<ScheduledOutfit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutfit(item: ScheduledOutfit): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateOutfit(item: ScheduledOutfit): Int

    @Query("DELETE FROM SCHEDULED_OUTFITS WHERE id = :id")
    suspend fun deleteOutfit(id: Int)
}