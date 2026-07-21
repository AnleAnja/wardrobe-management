package com.anleanja.wardrobe.database

import androidx.room.Transaction
import com.anleanja.wardrobe.database.daos.OutfitDao
import com.anleanja.wardrobe.database.daos.OutfitItemDao
import com.anleanja.wardrobe.database.daos.ScheduledItemDao
import com.anleanja.wardrobe.database.daos.ScheduledOutfitDao
import com.anleanja.wardrobe.database.daos.WardrobeItemDao
import com.anleanja.wardrobe.database.entities.Outfit
import com.anleanja.wardrobe.database.entities.OutfitItem
import com.anleanja.wardrobe.database.entities.ScheduledItem
import com.anleanja.wardrobe.database.entities.ScheduledOutfit
import com.anleanja.wardrobe.database.entities.WardrobeItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

    fun getOutfitsForItem(itemId: Int): Flow<List<Outfit>> {
        return outfitItemDao.getOutfitsForItem(itemId)
    }

    suspend fun insertOutfit(outfit: Outfit): Long {
        return outfitDao.insertOutfit(outfit)
    }

    suspend fun insertScheduledOutfit(scheduledOutfit: ScheduledOutfit) {
        val insertedId = scheduledOutfitDao.insertOutfit(scheduledOutfit).toInt()
        val wearTimestamp = resolveWearTimestamp(scheduledOutfit.date) ?: return

        val outfit = outfitDao.getById(scheduledOutfit.outfitId).firstOrNull()
        outfit?.let {
            outfitDao.updateOutfit(
                it.copy(
                    timesWorn = it.timesWorn + 1,
                    lastWorn = if (wearTimestamp > (it.lastWorn ?: 0L)) wearTimestamp else it.lastWorn
                )
            )
        }

        val baseItems = outfitItemDao.getItemsForOutfit(scheduledOutfit.outfitId).first()
        val scheduledItems = scheduledItemDao.getItemsForScheduledOutfit(insertedId).first()
        applyWearStatsToItems(
            (baseItems + scheduledItems).distinctBy { it.id },
            wearTimestamp
        )
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

    suspend fun deleteScheduledOutfit(id: Int) {
        scheduledOutfitDao.deleteOutfit(id)
    }

    suspend fun insertScheduledItem(item: ScheduledItem) {
        scheduledItemDao.insertItem(item)
    }

    suspend fun deleteScheduledItem(outfitId: Int) {
        scheduledItemDao.deleteItem(outfitId)
    }

    suspend fun replaceScheduledItems(scheduledOutfitId: Int, newItemIds: List<Int>) {
        scheduledItemDao.replaceAllForScheduledOutfit(scheduledOutfitId, newItemIds)

        val scheduled = scheduledOutfitDao.getById(scheduledOutfitId).firstOrNull() ?: return
        val wearTimestamp = resolveWearTimestamp(scheduled.date) ?: return

        val baseItemIds = outfitItemDao.getItemsForOutfit(scheduled.outfitId)
            .first()
            .map { it.id }
            .toSet()
        val scheduledOnlyItems = newItemIds
            .filter { it !in baseItemIds }
            .mapNotNull { wardrobeItemDao.getById(it).firstOrNull() }
        applyWearStatsToItems(scheduledOnlyItems, wearTimestamp)
    }

    private fun resolveWearTimestamp(scheduledDateMillis: Long?): Long? {
        if (scheduledDateMillis == null) return null
        val zone = ZoneId.systemDefault()
        val scheduledDate = Instant.ofEpochMilli(scheduledDateMillis).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        if (scheduledDate.isAfter(today)) return null

        return if (scheduledDate == today) {
            System.currentTimeMillis()
        } else {
            scheduledDate.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        }
    }

    private suspend fun applyWearStatsToItems(items: List<WardrobeItem>, wearTimestamp: Long) {
        items.forEach { item ->
            wardrobeItemDao.updateItem(
                item.copy(
                    timesWorn = item.timesWorn + 1,
                    lastWorn = if (wearTimestamp > (item.lastWorn ?: 0L)) wearTimestamp else item.lastWorn
                )
            )
        }
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