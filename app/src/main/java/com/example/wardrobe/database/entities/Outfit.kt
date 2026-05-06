package com.example.wardrobe.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outfits")
data class Outfit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name="image_uri_teaser") val imageUriTeaser: String? = null,
    @ColumnInfo(name="image_uri_combined") val imageUriCombined: String? = null,
    @ColumnInfo(name="seasons") val seasons: String? = null,
    @ColumnInfo(name="rating") val rating: Int? = null,
    @ColumnInfo(name="times_worn") val timesWorn: Int = 0,
    @ColumnInfo(name="last_worn") val lastWorn: Long? = null
)
