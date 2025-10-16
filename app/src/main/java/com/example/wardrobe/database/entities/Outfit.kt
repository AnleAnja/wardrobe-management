package com.example.wardrobe.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outfits")
data class Outfit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name="image_uri") val imageUri: String?,
    @ColumnInfo(name="seasons") val seasons: String? = null
)
