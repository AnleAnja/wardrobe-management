package com.anleanja.wardrobe.database.daos
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.anleanja.wardrobe.database.entities.Outfit
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

    @Query("DELETE FROM outfits")
    suspend fun deleteAll()

    /** Updates in place on id conflict, so child rows are not cascade-deleted like REPLACE would. */
    @Upsert
    suspend fun upsertOutfit(outfit: Outfit)

    @Query("SELECT COALESCE(MAX(id), 0) FROM outfits")
    suspend fun maxId(): Int

    @Query("SELECT id FROM outfits")
    suspend fun ids(): List<Int>

    @Query("SELECT id, image_uri_teaser, image_uri_combined FROM outfits")
    suspend fun imageUrisById(): List<OutfitImages>
}

data class OutfitImages(
    val id: Int,
    @ColumnInfo(name = "image_uri_teaser") val imageUriTeaser: String?,
    @ColumnInfo(name = "image_uri_combined") val imageUriCombined: String?,
)