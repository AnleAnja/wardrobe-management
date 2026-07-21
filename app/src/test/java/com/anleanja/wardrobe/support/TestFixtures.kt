package com.anleanja.wardrobe.support

import com.anleanja.wardrobe.database.entities.Outfit
import com.anleanja.wardrobe.database.entities.ScheduledOutfit
import com.anleanja.wardrobe.database.entities.WardrobeItem

object TestFixtures {
    fun wardrobeItem(
        id: Int = 0,
        category: String? = "Tops",
        subcategory: String? = "T-Shirts",
        seasons: String? = "Summer",
        rating: Int? = 3,
        price: Double? = null,
        purchaseDate: Long? = null,
        timesWorn: Int = 0,
        lastWorn: Long? = null,
        imageUri: String? = null
    ) = WardrobeItem(
        id = id,
        imageUri = imageUri,
        category = category,
        subcategory = subcategory,
        rating = rating,
        price = price,
        purchaseDate = purchaseDate,
        seasons = seasons,
        timesWorn = timesWorn,
        lastWorn = lastWorn
    )

    fun outfit(
        id: Int = 0,
        seasons: String? = "Summer",
        rating: Int? = null,
        timesWorn: Int = 0,
        lastWorn: Long? = null,
        imageUriTeaser: String? = null,
        imageUriCombined: String? = null
    ) = Outfit(
        id = id,
        imageUriTeaser = imageUriTeaser,
        imageUriCombined = imageUriCombined,
        seasons = seasons,
        rating = rating,
        timesWorn = timesWorn,
        lastWorn = lastWorn
    )

    fun scheduledOutfit(
        id: Int = 0,
        outfitId: Int = 1,
        date: Long? = null,
        temperature: Int? = null
    ) = ScheduledOutfit(
        id = id,
        outfitId = outfitId,
        date = date,
        temperature = temperature
    )
}
