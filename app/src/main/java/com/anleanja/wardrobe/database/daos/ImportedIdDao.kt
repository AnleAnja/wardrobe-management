package com.anleanja.wardrobe.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.anleanja.wardrobe.database.entities.ImportedId

@Dao
interface ImportedIdDao {
    @Query("SELECT * FROM imported_ids WHERE source_id = :sourceId")
    suspend fun forSource(sourceId: String): List<ImportedId>

    @Upsert
    suspend fun upsertAll(ids: List<ImportedId>)

    @Query("DELETE FROM imported_ids")
    suspend fun deleteAll()
}
