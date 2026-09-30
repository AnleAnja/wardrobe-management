package com.anleanja.wardrobe.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSizingTest {

    @Test
    fun sampleSize_usesPowerOfTwoUntilLongEdgeIsNearTheCap() {
        assertEquals(1, sampleSizeForMaxEdge(1600, 1200, MAX_IMAGE_EDGE_PX))
        assertEquals(2, sampleSizeForMaxEdge(4000, 3000, MAX_IMAGE_EDGE_PX))
        assertEquals(4, sampleSizeForMaxEdge(8000, 6000, MAX_IMAGE_EDGE_PX))
        assertEquals(1, sampleSizeForMaxEdge(0, 3000, MAX_IMAGE_EDGE_PX))
    }

    @Test
    fun fittedSize_downscalesTheLongEdgeAndDoesNotUpscale() {
        assertEquals(1600 to 1200, fittedSize(4000, 3000, MAX_IMAGE_EDGE_PX))
        assertEquals(1600 to 800, fittedSize(2000, 1000, MAX_IMAGE_EDGE_PX))
        assertEquals(1200 to 900, fittedSize(1200, 900, MAX_IMAGE_EDGE_PX))
    }
}
