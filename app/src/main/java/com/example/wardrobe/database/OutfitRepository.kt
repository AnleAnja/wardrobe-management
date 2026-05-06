package com.example.wardrobe.database

import androidx.room.Transaction
import com.example.wardrobe.database.daos.OutfitDao
import com.example.wardrobe.database.daos.OutfitItemDao
import com.example.wardrobe.database.daos.ScheduledItemDao
import com.example.wardrobe.database.daos.ScheduledOutfitDao
import com.example.wardrobe.database.daos.WardrobeItemDao
import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.OutfitItem
import com.example.wardrobe.database.entities.ScheduledItem
import com.example.wardrobe.database.entities.ScheduledOutfit
import com.example.wardrobe.database.entities.WardrobeItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class OutfitRepository @Inject constructor(
    private val outfitDao: OutfitDao,
    private val scheduledOutfitDao: ScheduledOutfitDao,
    private val outfitItemDao: OutfitItemDao,
    private val wardrobeItemDao: WardrobeItemDao,
    private val scheduledItemDao: ScheduledItemDao
) {
    fun getAll(): Flow<List<Outfit>> {
        return outfitDao.getAll()
    }

    fun getById(id: Int): Flow<Outfit?> {
        return outfitDao.getById(id)
    }

    fun getScheduledById(id: Int): Flow<ScheduledOutfit?> {
        return scheduledOutfitDao.getById(id)
    }

    fun getItemsForOutfit(outfitId: Int): Flow<List<WardrobeItem>> {
        return outfitItemDao.getItemsForOutfit(outfitId)
    }

    suspend fun insertOutfit(outfit: Outfit): Long {
        return outfitDao.insertOutfit(outfit)
    }

    suspend fun insertScheduledOutfit(scheduledOutfit: ScheduledOutfit) {
        scheduledOutfitDao.insertOutfit(scheduledOutfit)
        val outfit = outfitDao.getById(scheduledOutfit.outfitId).first()
        outfit?.let {
            val newLastWorn = scheduledOutfit.date ?: 0L
            outfitDao.updateOutfit(it.copy(
                timesWorn = it.timesWorn + 1,
                lastWorn = if (newLastWorn > (it.lastWorn ?: 0L)) newLastWorn else it.lastWorn
            ))
        }
        val items = outfitItemDao.getItemsForOutfit(scheduledOutfit.outfitId).first()
        items.forEach {
            val newItemLastWorn = scheduledOutfit.date ?: 0L
            wardrobeItemDao.updateItem(it.copy(
                timesWorn = it.timesWorn + 1,
                lastWorn = if (newItemLastWorn > (it.lastWorn ?: 0L)) newItemLastWorn else it.lastWorn
            ))
        }
    }

    suspend fun insertOutfitItem(item: OutfitItem) {
        outfitItemDao.insertItem(item)
    }

    suspend fun updateOutfit(outfit: Outfit) {
        outfitDao.updateOutfit(outfit)
    }

    suspend fun deleteOutfit(id: Int) {
        outfitDao.deleteOutfit(id)
    }

    suspend fun insertScheduledItem(item: ScheduledItem) {
        scheduledItemDao.insertItem(item)
    }

    suspend fun deleteScheduledItem(outfitId: Int) {
        scheduledItemDao.deleteItem(outfitId)
    }

    suspend fun replaceScheduledItems(scheduledOutfitId: Int, newItemIds: List<Int>) {
        scheduledItemDao.replaceAllForScheduledOutfit(scheduledOutfitId, newItemIds)
    }

    suspend fun replaceOutfitItems(outfitId: Int, newItemIds: Set<Int>) {
        outfitItemDao.deleteAllItemsForOutfit(outfitId)
        newItemIds.forEach { itemId ->
            outfitItemDao.insertItem(OutfitItem(outfitId = outfitId, itemId = itemId))
        }
    }

    fun getAllScheduledForOutfit(outfitId: Int): Flow<List<ScheduledOutfit>> {
        return scheduledOutfitDao.getAllForOutfit(outfitId)
    }

    fun getOutfitsByIds(outfitIds: List<Int>): Flow<List<Outfit>> {
        return outfitDao.getOutfitsByIds(outfitIds)
    }

    fun getAllScheduled(): Flow<List<ScheduledOutfit>> {
        return scheduledOutfitDao.getAll()
    }

    fun getOutfitByScheduledId(scheduledOutfitId: Int): Flow<Outfit?> {
        return outfitDao.getOutfitByScheduledId(scheduledOutfitId)
    }

    fun getItemsForScheduledOutfit(scheduledOutfitId: Int): Flow<List<WardrobeItem>> {
        return scheduledItemDao.getItemsForScheduledOutfit(scheduledOutfitId)
    }
}