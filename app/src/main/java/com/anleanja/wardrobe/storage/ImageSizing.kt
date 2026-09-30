package com.anleanja.wardrobe.storage

import kotlin.math.max
import kotlin.math.roundToInt

const val MAX_IMAGE_EDGE_PX = 1600
const val JPEG_QUALITY = 85

/** Power-of-two sample size so decoding stays near [maxEdge] on the long side. */
fun sampleSizeForMaxEdge(width: Int, height: Int, maxEdge: Int): Int {
    if (width <= 0 || height <= 0 || maxEdge <= 0) return 1
    var sample = 1
    while (max(width, height) / (sample * 2) >= maxEdge) {
        sample *= 2
    }
    return sample
}

/** Width and height that fit inside [maxEdge] without upscaling. */
fun fittedSize(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
    if (width <= 0 || height <= 0) return width to height
    val longEdge = max(width, height)
    if (longEdge <= maxEdge) return width to height
    val scale = maxEdge.toFloat() / longEdge
    return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
}
