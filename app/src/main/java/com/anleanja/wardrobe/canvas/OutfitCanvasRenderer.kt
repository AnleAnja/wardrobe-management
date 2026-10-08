package com.anleanja.wardrobe.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.roundToInt

const val RENDERED_CANVAS_HEIGHT_PX = 1600

/**
 * Draws [items] on a white canvas the way the editor shows them, using the stored photos
 * rather than a screenshot so the result doesn't depend on what is currently on screen.
 *
 * [decode] loads one photo; it's called once per item in draw order. Returns null when no
 * item photo could be decoded, so a blank image is never saved as the outfit photo.
 */
suspend fun renderOutfitCanvas(
    items: List<CanvasItem>,
    decode: suspend (String) -> Bitmap?,
    heightPx: Int = RENDERED_CANVAS_HEIGHT_PX,
): Bitmap? {
    val widthPx = (heightPx * CANVAS_ASPECT_RATIO).roundToInt()
    val output = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output).apply { drawColor(Color.WHITE) }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    var drewAny = false
    for (item in items.inDrawOrder()) {
        val photo = item.imageUri?.let { decode(it) } ?: continue
        try {
            val box = item.boxOn(widthPx.toFloat(), heightPx.toFloat())
            val (width, height) = fitInSquare(photo.width, photo.height, box.edge)
            val left = box.left + (box.edge - width) / 2f
            val top = box.top + (box.edge - height) / 2f
            canvas.drawBitmap(photo, null, RectF(left, top, left + width, top + height), paint)
            drewAny = true
        } finally {
            photo.recycle()
        }
    }
    if (!drewAny) {
        output.recycle()
        return null
    }
    return output
}
