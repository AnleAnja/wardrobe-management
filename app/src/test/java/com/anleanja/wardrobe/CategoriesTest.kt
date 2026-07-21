package com.anleanja.wardrobe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoriesTest {

    @Test
    fun hierarchy_containsJumpsuitsUnderDresses() {
        assertTrue(CategoryHierarchy.hierarchy["Dresses"]!!.contains("Jumpsuits"))
    }

    @Test
    fun superCategories_areSortedAlphabetically() {
        assertEquals(CategoryHierarchy.hierarchy.keys.sorted(), CategoryHierarchy.superCategories)
    }

    @Test
    fun everySuperCategoryHasSubcategories() {
        CategoryHierarchy.superCategories.forEach { category ->
            assertTrue(
                "Expected subcategories for $category",
                CategoryHierarchy.hierarchy[category]!!.isNotEmpty()
            )
        }
    }

    @Test
    fun subcategories_areUniqueWithinEachCategory() {
        CategoryHierarchy.hierarchy.forEach { (category, subcategories) ->
            assertEquals(
                "Duplicate subcategories in $category",
                subcategories.size,
                subcategories.toSet().size
            )
        }
    }
}
