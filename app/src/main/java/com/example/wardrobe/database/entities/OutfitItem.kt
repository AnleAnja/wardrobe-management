package com.example.wardrobe.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "outfit_items",
    primaryKeys = ["outfit_id", "item_id"], // Define the composite primary key here
    foreignKeys = [
        ForeignKey(
            entity = Outfit::class,
            parentColumns = ["id"],
            childColumns = ["outfit_id"],
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
data class OutfitItem(
    @ColumnInfo(name="outfit_id") val outfitId: Int,
    @ColumnInfo(name="item_id") val itemId: Int
)