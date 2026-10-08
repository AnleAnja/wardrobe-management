package com.anleanja.wardrobe.canvas

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OutfitCanvasRendererTest {

    @Test
    fun drawsItemsWhereTheEditorShowsThem() = runTest {
        val items = listOf(
            CanvasItem(itemId = 1, imageUri = "red", centerX = 0.25f, centerY = 0.25f, size = 0.4f, z = 1),
        )

        val rendered = renderOutfitCanvas(items, decode = { solid(Color.RED, 10, 10) }, heightPx = 400)

        assertNotNull(rendered)
        rendered!!
        assertEquals(300, rendered.width)
        assertEquals(400, rendered.height)
        // Box: edge 120px centered at (75, 100).
        assertEquals(Color.RED, rendered.getPixel(75, 100))
        assertEquals(Color.RED, rendered.getPixel(20, 45))
        assertEquals(Color.WHITE, rendered.getPixel(250, 350))
    }

    @Test
    fun fitsPhotosInsideTheirBox() = runTest {
        val items = listOf(CanvasItem(itemId = 1, imageUri = "wide", centerX = 0.5f, centerY = 0.5f, size = 0.4f))

        val rendered = renderOutfitCanvas(items, decode = { solid(Color.RED, 20, 10) }, heightPx = 400)!!

        // 120px box at (90..210, 140..260); a 2:1 photo fills 120x60 around y = 200.
        assertEquals(Color.RED, rendered.getPixel(150, 200))
        assertEquals(Color.WHITE, rendered.getPixel(150, 150))
    }

    @Test
    fun higherItemsAreDrawnOnTop() = runTest {
        val items = listOf(
            CanvasItem(itemId = 1, imageUri = "blue", z = 2),
            CanvasItem(itemId = 2, imageUri = "red", z = 1),
        )
        val colors = mapOf("red" to Color.RED, "blue" to Color.BLUE)

        val rendered = renderOutfitCanvas(items, decode = { solid(colors.getValue(it), 10, 10) }, heightPx = 400)!!

        assertEquals(Color.BLUE, rendered.getPixel(150, 200))
    }

    @Test
    fun skipsItemsWhosePhotoCannotBeDecoded() = runTest {
        val items = listOf(
            CanvasItem(itemId = 1, imageUri = "missing", z = 2),
            CanvasItem(itemId = 2, imageUri = "red", z = 1),
        )

        val rendered = renderOutfitCanvas(
            items,
            decode = { if (it == "red") solid(Color.RED, 10, 10) else null },
            heightPx = 400,
        )!!

        assertEquals(Color.RED, rendered.getPixel(150, 200))
    }

    @Test
    fun returnsNullWhenNothingCouldBeDrawn() = runTest {
        val items = listOf(
            CanvasItem(itemId = 1, imageUri = null),
            CanvasItem(itemId = 2, imageUri = "missing"),
        )

        assertNull(renderOutfitCanvas(items, decode = { null }))
        assertNull(renderOutfitCanvas(emptyList(), decode = { solid(Color.RED, 1, 1) }))
    }

    private fun solid(color: Int, width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
}
