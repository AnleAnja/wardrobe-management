package com.example.wardrobe.database.entities

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wardrobe_items")
data class WardrobeItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    @ColumnInfo(name="image_uri") val imageUri: String? = null,
    @ColumnInfo(name="category") val category: String? = null,
    @ColumnInfo(name="rating") val rating: Int? = null,
    @ColumnInfo(name="price") val price: Double? = null,
    @ColumnInfo(name="purchase_date") val purchaseDate: Long? = null,
    @ColumnInfo(name="seasons") val seasons: String? = null
)
