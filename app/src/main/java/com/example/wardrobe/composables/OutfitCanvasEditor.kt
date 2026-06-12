package com.example.wardrobe.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.wardrobe.database.entities.WardrobeItem

private const val BASE_SIZE_DP = 120f
private const val MIN_SCALE = 0.3f
private const val MAX_SCALE = 5f

class CanvasItemState(
    val itemId: Int,
    val imageUri: String?,
    offset: Offset = Offset.Zero,
    scale: Float = 1f,
    zIndex: Float = 0f,
) {
    var offset by mutableStateOf(offset)
    var scale by mutableStateOf(scale)
    var zIndex by mutableStateOf(zIndex)
}

@Composable
fun OutfitCanvasEditor(
    items: List<WardrobeItem>,
    graphicsLayer: GraphicsLayer,
    modifier: Modifier = Modifier,
) {
    val canvasItems = remember { mutableStateListOf<CanvasItemState>() }
    var nextZ by remember { mutableFloatStateOf(1f) }
    var selectedId by remember { mutableStateOf<Int?>(null) }
    val basePx = with(LocalDensity.current) { BASE_SIZE_DP.dp.toPx() }

    LaunchedEffect(items) {
        val selectedIds = items.map { it.id }.toSet()
        canvasItems.removeAll { it.itemId !in selectedIds }
        if (selectedId != null && selectedId !in selectedIds) selectedId = null
        items.forEach { item ->
            if (canvasItems.none { it.itemId == item.id }) {
                canvasItems.add(
                    CanvasItemState(
                        itemId = item.id,
                        imageUri = item.imageUri,
                        zIndex = nextZ++,
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectTapGestures { selectedId = null }
            }
    ) {
        // Recorded layer: only the item images end up in the saved bitmap.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    graphicsLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(graphicsLayer)
                }
        ) {
            canvasItems.sortedBy { it.zIndex }.forEach { state ->
                key(state.itemId) {
                    AsyncImage(
                        model = state.imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                translationX = state.offset.x
                                translationY = state.offset.y
                            }
                            .size((BASE_SIZE_DP * state.scale).dp)
                    )
                }
            }
        }

        // Overlay layer (not recorded): hit targets, selection border and resize handle.
        canvasItems.sortedBy { it.zIndex }.forEach { state ->
            key("overlay-${state.itemId}") {
                val isSelected = state.itemId == selectedId
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            translationX = state.offset.x
                            translationY = state.offset.y
                        }
                        .size((BASE_SIZE_DP * state.scale).dp)
                        .then(
                            if (isSelected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                Modifier
                            }
                        )
                        .pointerInput(state.itemId) {
                            detectTapGestures {
                                selectedId = state.itemId
                                state.zIndex = nextZ++
                            }
                        }
                        .pointerInput(state.itemId) {
                            detectDragGestures(
                                onDragStart = {
                                    selectedId = state.itemId
                                    state.zIndex = nextZ++
                                }
                            ) { change, drag ->
                                change.consume()
                                state.offset += drag
                            }
                        }
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)
                                .pointerInput(state.itemId) {
                                    detectDragGestures { change, drag ->
                                        change.consume()
                                        val delta = (drag.x + drag.y) / 2f / basePx
                                        state.scale = (state.scale + delta).coerceIn(MIN_SCALE, MAX_SCALE)
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}
