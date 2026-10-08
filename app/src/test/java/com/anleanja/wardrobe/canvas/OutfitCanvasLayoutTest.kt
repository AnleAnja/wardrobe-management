package com.anleanja.wardrobe.canvas

import com.anleanja.wardrobe.database.entities.WardrobeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class OutfitCanvasLayoutTest {

    private val shirt = WardrobeItem(id = 1, imageUri = "file:///shirt.png")
    private val skirt = WardrobeItem(id = 2, imageUri = "file:///skirt.png")
    private val shoes = WardrobeItem(id = 3, imageUri = "file:///shoes.png")

    @Test
    fun syncStacksNewItemsOnTopInSelectionOrder() {
        val items = syncCanvasItems(emptyList(), listOf(shirt, skirt))

        assertEquals(listOf(1, 2), items.map { it.itemId })
        assertEquals(listOf(1, 2), items.map { it.z })
        assertEquals("file:///skirt.png", items[1].imageUri)
    }

    @Test
    fun syncKeepsLayoutOfItemsThatStaySelected() {
        val arranged = syncCanvasItems(emptyList(), listOf(shirt, skirt))
            .moved(1, dx = 0.2f, dy = -0.1f)
            .resized(1, delta = 0.1f)

        val synced = syncCanvasItems(arranged, listOf(shirt, skirt, shoes))

        assertEquals(arranged[0], synced[0])
        assertEquals(arranged[1], synced[1])
        assertEquals(3, synced[2].z)
    }

    @Test
    fun syncDropsDeselectedItems() {
        val arranged = syncCanvasItems(emptyList(), listOf(shirt, skirt, shoes))

        val synced = syncCanvasItems(arranged, listOf(shirt, shoes))

        assertEquals(listOf(1, 3), synced.map { it.itemId })
    }

    @Test
    fun syncPicksUpChangedPhotos() {
        val arranged = syncCanvasItems(emptyList(), listOf(shirt))

        val synced = syncCanvasItems(arranged, listOf(shirt.copy(imageUri = "file:///new.png")))

        assertEquals("file:///new.png", synced.single().imageUri)
    }

    @Test
    fun moveKeepsTheCenterOnTheCanvas() {
        val items = syncCanvasItems(emptyList(), listOf(shirt))

        val moved = items.moved(1, dx = 0.3f, dy = -2f).single()

        assertEquals(0.8f, moved.centerX, DELTA)
        assertEquals(0f, moved.centerY, DELTA)
        assertEquals(1f, items.moved(1, dx = 5f, dy = 0f).single().centerX, DELTA)
    }

    @Test
    fun moveLeavesOtherItemsAlone() {
        val items = syncCanvasItems(emptyList(), listOf(shirt, skirt))

        assertSame(items[1], items.moved(1, 0.1f, 0.1f)[1])
    }

    @Test
    fun resizeIsClamped() {
        val items = syncCanvasItems(emptyList(), listOf(shirt))

        assertEquals(DEFAULT_ITEM_SIZE + 0.1f, items.resized(1, 0.1f).single().size, DELTA)
        assertEquals(MIN_ITEM_SIZE, items.resized(1, -10f).single().size, DELTA)
        assertEquals(MAX_ITEM_SIZE, items.resized(1, 10f).single().size, DELTA)
    }

    @Test
    fun bringToFrontRaisesAboveEveryOtherItem() {
        val items = syncCanvasItems(emptyList(), listOf(shirt, skirt, shoes))

        val raised = items.broughtToFront(1)

        assertEquals(listOf(2, 3, 1), raised.inDrawOrder().map { it.itemId })
    }

    @Test
    fun bringToFrontIsANoOpForTheTopItemOrUnknownIds() {
        val items = syncCanvasItems(emptyList(), listOf(shirt, skirt))

        assertSame(items, items.broughtToFront(2))
        assertSame(items, items.broughtToFront(99))
    }

    @Test
    fun boxScalesWithTheCanvas() {
        val item = CanvasItem(itemId = 1, imageUri = null, centerX = 0.25f, centerY = 0.5f, size = 0.5f)

        assertEquals(ItemBox(left = 0f, top = 125f, edge = 150f), item.boxOn(300f, 400f))
        assertEquals(ItemBox(left = 0f, top = 500f, edge = 600f), item.boxOn(1200f, 1600f))
    }

    @Test
    fun fitInSquareKeepsAspectRatio() {
        assertEquals(100f to 50f, fitInSquare(400, 200, 100f))
        assertEquals(25f to 100f, fitInSquare(100, 400, 100f))
        assertEquals(100f to 100f, fitInSquare(10, 10, 100f))
        assertEquals(0f to 0f, fitInSquare(0, 10, 100f))
    }

    private companion object {
        const val DELTA = 1e-5f
    }
}
