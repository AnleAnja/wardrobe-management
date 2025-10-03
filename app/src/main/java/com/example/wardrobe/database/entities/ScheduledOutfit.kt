package com.example.wardrobe.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "scheduled_outfits",
    foreignKeys = [
        ForeignKey(
            entity = Outfit::class,
            parentColumns = ["id"],
            childColumns = ["outfit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ScheduledOutfit(
    @PrimaryKey val id: Int,

    @ColumnInfo(name="outfit_id") val outfitId: Int,
    @ColumnInfo(name="date") val date: Long?,
    @ColumnInfo(name="temperature") val temperature: Double?
)
