package com.anleanja.wardrobe.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Remembers which local row a record from another installation's backup was merged into,
 * so merging the same backup again updates that row instead of adding a copy.
 */
@Entity(
    tableName = "imported_ids",
    primaryKeys = ["source_id", "kind", "foreign_id"],
)
data class ImportedId(
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "foreign_id") val foreignId: Int,
    @ColumnInfo(name = "local_id") val localId: Int,
) {
    companion object {
        const val KIND_ITEM = "item"
        const val KIND_OUTFIT = "outfit"
        const val KIND_SCHEDULED_OUTFIT = "scheduled_outfit"
    }
}
