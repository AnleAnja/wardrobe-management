package com.anleanja.wardrobe.database

import com.anleanja.wardrobe.database.daos.WardrobeItemDao
import com.anleanja.wardrobe.database.entities.WardrobeItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WardrobeItemRepository @Inject constructor(
    private val wardrobeItemDao: WardrobeItemDao
) {
    fun getAll(): Flow<List<WardrobeItem>> {
        return wardrobeItemDao.getAll()
    }

    fun getById(id: Int): Flow<WardrobeItem?> {
        return wardrobeItemDao.getById(id)
    }

    suspend fun insertItem(item: WardrobeItem) {
        wardrobeItemDao.insertItem(item)
    }

    suspend fun updateItem(item: WardrobeItem) {
        wardrobeItemDao.updateItem(item)
    }

    suspend fun deleteItem(id: Int) {
        wardrobeItemDao.deleteItem(id)
    }
}
