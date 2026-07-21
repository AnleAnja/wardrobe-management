package com.anleanja.wardrobe.json_parser

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WardrobeImportJsonTest {

    private val gson = Gson()

    @Test
    fun parsesFullExportPayload() {
        val json = """
            {
              "wardrobeItems": [
                {
                  "id": 1,
                  "imageUri": "file:///item.jpg",
                  "category": "Tops",
                  "subcategory": "T-Shirts",
                  "rating": 4,
                  "price": 19.5,
                  "purchaseDate": 100,
                  "seasons": "Summer",
                  "timesWorn": 2,
                  "lastWorn": 200
                }
              ],
              "outfits": [
                {
                  "id": 10,
                  "imageUriCombined": "file:///combined.jpg",
                  "imageUriTeaser": "file:///teaser.jpg",
                  "seasons": "Summer",
                  "rating": 5,
                  "timesWorn": 1,
                  "lastWorn": 300
                }
              ],
              "outfitItems": [
                { "outfitId": 10, "itemId": 1 }
              ],
              "scheduledOutfits": [
                { "id": 20, "outfitId": 10, "date": 400, "temperature": 18 }
              ],
              "scheduledItems": [
                { "scheduledOutfitId": 20, "itemId": 1 }
              ]
            }
        """.trimIndent()

        val import = gson.fromJson(json, WardrobeImport::class.java)

        assertEquals(1, import.wardrobeItems.size)
        assertEquals("Tops", import.wardrobeItems.first().category)
        assertEquals(10, import.outfits.first().id)
        assertEquals(10, import.outfitItems.first().outfitId)
        assertEquals(18, import.scheduledOutfits.first().temperature)
        assertEquals(1, import.scheduledItems!!.size)
    }

    @Test
    fun parsesLegacyExportWithoutScheduledItems() {
        val json = """
            {
              "wardrobeItems": [],
              "outfits": [],
              "outfitItems": [],
              "scheduledOutfits": []
            }
        """.trimIndent()

        val import = gson.fromJson(json, WardrobeImport::class.java)

        assertTrue(import.wardrobeItems.isEmpty())
        assertNull(import.scheduledItems)
    }

    @Test
    fun roundTrip_preservesData() {
        val original = WardrobeImport(
            wardrobeItems = listOf(
                WardrobeItemJson(
                    id = 1,
                    imageUri = "file:///item.jpg",
                    category = "Tops",
                    subcategory = "T-Shirts",
                    rating = 4,
                    price = 19.5,
                    purchaseDate = 100,
                    seasons = "Summer",
                    timesWorn = 2,
                    lastWorn = 200
                )
            ),
            outfits = listOf(
                OutfitJson(
                    id = 10,
                    imageUriCombined = "file:///combined.jpg",
                    imageUriTeaser = "file:///teaser.jpg",
                    seasons = "Summer",
                    rating = 5,
                    timesWorn = 1,
                    lastWorn = 300
                )
            ),
            outfitItems = listOf(OutfitItemJson(outfitId = 10, itemId = 1)),
            scheduledOutfits = listOf(
                ScheduledOutfitJson(id = 20, outfitId = 10, date = 400, temperature = 18)
            ),
            scheduledItems = listOf(ScheduledItemJson(scheduledOutfitId = 20, itemId = 1))
        )

        val restored = gson.fromJson(gson.toJson(original), WardrobeImport::class.java)

        assertEquals(original, restored)
    }

    @Test
    fun invalidJsonFailsGracefully() {
        val result = runCatching {
            gson.fromJson("{ not valid json", WardrobeImport::class.java)
        }
        assertTrue(result.isFailure)
    }
}
