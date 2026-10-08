package com.anleanja.wardrobe.canvas

import com.anleanja.wardrobe.database.entities.WardrobeItem
import kotlin.math.min

/** Canvas width divided by height, shared by the editor and the saved image. */
const val CANVAS_ASPECT_RATIO = 3f / 4f
const val DEFAULT_ITEM_SIZE = 0.36f
const val MIN_ITEM_SIZE = 0.1f
const val MAX_ITEM_SIZE = 1.5f

/**
 * A wardrobe item placed on the outfit canvas. Positions and sizes are fractions of the
 * canvas rather than pixels, so the layout fits any canvas size and survives rotation.
 *
 * [centerX] is relative to the canvas width and [centerY] to its height. [size] is the edge
 * of the item's square box relative to the canvas width; the photo is fitted inside that box.
 * Items with a higher [z] are drawn on top.
 */
data class CanvasItem(
    val itemId: Int,
    val imageUri: String?,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val size: Float = DEFAULT_ITEM_SIZE,
    val z: Int = 0,
)

/**
 * Keeps the layout of items that are still selected, drops deselected ones and stacks
 * newly selected items on top in [selected] order.
 */
fun syncCanvasItems(current: List<CanvasItem>, selected: List<WardrobeItem>): List<CanvasItem> {
    val selectedById = selected.associateBy { it.id }
    val kept = current
        .filter { it.itemId in selectedById }
        .map { it.copy(imageUri = selectedById.getValue(it.itemId).imageUri) }
    val keptIds = kept.map { it.itemId }.toSet()
    var nextZ = topZ(kept) + 1
    val added = selected
        .filter { it.id !in keptIds }
        .map { CanvasItem(itemId = it.id, imageUri = it.imageUri, z = nextZ++) }
    return kept + added
}

/** Moves an item by a fraction of the canvas, keeping its center on the canvas. */
fun List<CanvasItem>.moved(itemId: Int, dx: Float, dy: Float): List<CanvasItem> = map {
    if (it.itemId != itemId) {
        it
    } else {
        it.copy(
            centerX = (it.centerX + dx).coerceIn(0f, 1f),
            centerY = (it.centerY + dy).coerceIn(0f, 1f),
        )
    }
}

fun List<CanvasItem>.resized(itemId: Int, delta: Float): List<CanvasItem> = map {
    if (it.itemId != itemId) it else it.copy(size = (it.size + delta).coerceIn(MIN_ITEM_SIZE, MAX_ITEM_SIZE))
}

fun List<CanvasItem>.broughtToFront(itemId: Int): List<CanvasItem> {
    val target = firstOrNull { it.itemId == itemId } ?: return this
    if (all { it === target || it.z < target.z }) return this
    val front = topZ(this) + 1
    return map { if (it.itemId == itemId) it.copy(z = front) else it }
}

fun List<CanvasItem>.inDrawOrder(): List<CanvasItem> = sortedBy { it.z }

/** Pixel rectangle of an item's square box on a canvas of the given size. */
data class ItemBox(val left: Float, val top: Float, val edge: Float)

fun CanvasItem.boxOn(canvasWidth: Float, canvasHeight: Float): ItemBox {
    val edge = size * canvasWidth
    return ItemBox(
        left = centerX * canvasWidth - edge / 2f,
        top = centerY * canvasHeight - edge / 2f,
        edge = edge,
    )
}

/** Size of a [width] x [height] image scaled to fit inside a square of [edge], keeping its aspect ratio. */
fun fitInSquare(width: Int, height: Int, edge: Float): Pair<Float, Float> {
    if (width <= 0 || height <= 0) return 0f to 0f
    val scale = min(edge / width, edge / height)
    return width * scale to height * scale
}

private fun topZ(items: List<CanvasItem>): Int = items.maxOfOrNull { it.z } ?: 0
