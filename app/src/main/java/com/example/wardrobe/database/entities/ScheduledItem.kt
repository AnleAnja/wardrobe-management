package com.example.wardrobe.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "scheduled_items",
    primaryKeys = ["scheduled_outfit_id", "item_id"],
    foreignKeys = [
        ForeignKey(
            entity = ScheduledOutfit::class,
            parentColumns = ["id"],
            childColumns = ["scheduled_outfit_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WardrobeItem::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)

data class ScheduledItem(
    @ColumnInfo(name="scheduled_outfit_id") val scheduledOutfitId: Int,
    @ColumnInfo(name="item_id") val itemId: Int
)