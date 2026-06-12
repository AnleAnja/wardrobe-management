package com.example.wardrobe.composables

import com.example.wardrobe.database.entities.Outfit
import com.example.wardrobe.database.entities.WardrobeItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

class ModernWardrobeComponentsTest {

    @Test
    fun `wardrobe item title joins category and subcategory`() {
        val item = WardrobeItem(category = "Tops", subcategory = "T-Shirts")
        assertEquals("Tops / T-Shirts", item.modernTitle())
    }

    @Test
    fun `wardrobe item title skips blank subcategory`() {
        val item = WardrobeItem(category = "Tops", subcategory = " ")
        assertEquals("Tops", item.modernTitle())
    }

    @Test
    fun `wardrobe item title falls back when nothing is set`() {
        assertEquals("Wardrobe item", WardrobeItem().modernTitle())
    }

    @Test
    fun `wardrobe item summary includes all populated fields`() {
        val item = WardrobeItem(
            seasons = "Summer",
            rating = 4,
            price = 19.5,
            timesWorn = 3,
            lastWorn = null
        )
        val summary = item.modernSummary()
        assertEquals(
            "Summer - 4/5 rating - ${19.5.toPriceLabel()} - 3 wears - last worn never",
            summary
        )
    }

    @Test
    fun `wardrobe item summary omits unset rating and price`() {
        val item = WardrobeItem(rating = 0, price = null, timesWorn = 0, lastWorn = null)
        assertEquals("0 wears - last worn never", item.modernSummary())
    }

    @Test
    fun `outfit title uses id`() {
        assertEquals("Outfit #7", Outfit(id = 7).modernTitle())
    }

    @Test
    fun `outfit summary omits unset fields`() {
        val outfit = Outfit(seasons = null, rating = null, timesWorn = 2, lastWorn = null)
        assertEquals("2 wears - last worn never", outfit.modernSummary())
    }

    @Test
    fun `price label uses two decimals and euro sign`() {
        assertEquals("19.50 €", 19.5.toPriceLabel(Locale.US))
        assertEquals("19,50 €", 19.5.toPriceLabel(Locale.GERMANY))
    }

    @Test
    fun `date label returns fallback for null`() {
        assertEquals("never", (null as Long?).toDateLabel("never"))
    }

    @Test
    fun `date label formats epoch millis`() {
        val epochMillis = LocalDate.of(2026, 6, 12)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        assertEquals(
            "Jun 12, 2026",
            epochMillis.toDateLabel("never", zoneId = ZoneOffset.UTC, locale = Locale.ENGLISH)
        )
    }
}
