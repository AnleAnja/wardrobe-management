package com.anleanja.wardrobe.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anleanja.wardrobe.canvas.CANVAS_ASPECT_RATIO
import com.anleanja.wardrobe.canvas.CanvasItem
import com.anleanja.wardrobe.canvas.boxOn
import com.anleanja.wardrobe.canvas.inDrawOrder
import kotlin.math.roundToInt

/**
 * Lets the user arrange [items] on a 3:4 canvas. The layout itself lives in the caller (the
 * view model), so it survives rotation; gestures are reported as fractions of the canvas.
 */
@Composable
fun OutfitCanvasEditor(
    items: List<CanvasItem>,
    onMove: (itemId: Int, dx: Float, dy: Float) -> Unit,
    onResize: (itemId: Int, delta: Float) -> Unit,
    onBringToFront: (itemId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(items) {
        if (items.none { it.itemId == selectedId }) selectedId = null
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CANVAS_ASPECT_RATIO)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectTapGestures { selectedId = null }
            }
    ) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()
        val currentWidth by rememberUpdatedState(canvasWidth)
        val currentHeight by rememberUpdatedState(canvasHeight)
        val currentOnMove by rememberUpdatedState(onMove)
        val currentOnResize by rememberUpdatedState(onResize)
        val currentOnBringToFront by rememberUpdatedState(onBringToFront)

        items.inDrawOrder().forEach { item ->
            key(item.itemId) {
                val box = item.boxOn(canvasWidth, canvasHeight)
                val isSelected = item.itemId == selectedId
                Box(
                    modifier = Modifier
                        // Measured at its own size even when larger than the canvas, so the
                        // preview matches the saved image.
                        .layout { measurable, _ ->
                            val edge = box.edge.roundToInt()
                            val placeable = measurable.measure(Constraints.fixed(edge, edge))
                            layout(0, 0) { placeable.place(box.left.roundToInt(), box.top.roundToInt()) }
                        }
                        .then(
                            if (isSelected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                Modifier
                            }
                        )
                        .pointerInput(item.itemId) {
                            detectTapGestures {
                                selectedId = item.itemId
                                currentOnBringToFront(item.itemId)
                            }
                        }
                        .pointerInput(item.itemId) {
                            detectDragGestures(
                                onDragStart = {
                                    selectedId = item.itemId
                                    currentOnBringToFront(item.itemId)
                                }
                            ) { change, drag ->
                                change.consume()
                                currentOnMove(item.itemId, drag.x / currentWidth, drag.y / currentHeight)
                            }
                        }
                ) {
                    AsyncImage(
                        model = item.imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                                .pointerInput(item.itemId) {
                                    detectDragGestures { change, drag ->
                                        change.consume()
                                        currentOnResize(item.itemId, (drag.x + drag.y) / 2f / currentWidth)
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}
