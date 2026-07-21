package com.anleanja.wardrobe.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun stringListRoundTrip() {
        val original = listOf("Spring", null, "Summer")
        val encoded = converters.fromStringList(original)
        val decoded = converters.toStringList(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun nullStringListRoundTrip() {
        assertNull(converters.fromStringList(null))
        assertNull(converters.toStringList(null))
    }

    @Test
    fun emptyStringListRoundTrip() {
        val original = emptyList<String>()
        val decoded = converters.toStringList(converters.fromStringList(original))
        assertEquals(original, decoded)
    }
}
