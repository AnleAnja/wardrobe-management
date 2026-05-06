package com.example.wardrobe.database.daos
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.wardrobe.database.entities.ScheduledItem
import com.example.wardrobe.database.entities.WardrobeItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledItemDao {

    @Query("""
        SELECT w.* FROM wardrobe_items w
        INNER JOIN scheduled_items si ON w.id = si.item_id
        WHERE si.scheduled_outfit_id = :scheduledOutfitId
    """)
    fun getItemsForScheduledOutfit(scheduledOutfitId: Int): Flow<List<WardrobeItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ScheduledItem): Long

    @Query("DELETE FROM scheduled_items WHERE scheduled_outfit_id = :scheduledOutfitId")
    suspend fun deleteItem(scheduledOutfitId: Int)

    @Transaction
    suspend fun replaceAllForScheduledOutfit(scheduledOutfitId: Int, newItemIds: List<Int>) {
        deleteItem(scheduledOutfitId)
        newItemIds.forEach { itemId ->
            insertItem(ScheduledItem(scheduledOutfitId = scheduledOutfitId, itemId = itemId))
        }
    }
}