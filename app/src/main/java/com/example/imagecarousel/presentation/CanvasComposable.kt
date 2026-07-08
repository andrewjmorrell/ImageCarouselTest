// PROVENANCE: BASELINE
// Established: 2026-07-08   Cut-line: aa82f75   Note: pre-existing code; provenance not established; exempt from DDR/gate.
package com.example.imagecarousel.presentation

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.imagecarousel.R
import com.example.imagecarousel.presentation.models.CanvasImage
import kotlin.collections.forEach
import kotlin.math.roundToInt

@Composable
fun CanvasComposable(
    modifier: Modifier = Modifier,
    initialOrientation: Int,
    canvasBounds: IntRect,
    onCanvasBoundsChange: (IntRect) -> Unit,
    canvasImages: List<CanvasImage>,
    draggingCanvasItemId: String?
) {

    val viewModel: ImageCarouselViewModel = hiltViewModel<ImageCarouselViewModel>()

    Box(
        modifier = modifier
            .then(
                // Use this to keep the canvas square based upon orientation
                if (initialOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    Modifier.fillMaxHeight()
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            .aspectRatio(1f)
            .padding(horizontal = dimensionResource(R.dimen.canvas_padding))
            .background(Color.Black)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val size = coords.size
                onCanvasBoundsChange(
                    IntRect(
                        pos.x.roundToInt(),
                        pos.y.roundToInt(),
                        (pos.x + size.width).roundToInt(),
                        (pos.y + size.height).roundToInt()
                    )
                )
            }
    ) {
        //Place each image on the canvas
        canvasImages.forEach { item ->
            var frameOffset by remember(item.id) { mutableStateOf(item.offset) }
            var userScale by remember(item.id) { mutableFloatStateOf(item.scale) }

            LaunchedEffect(item.offset) { frameOffset = item.offset }
            LaunchedEffect(item.scale) { userScale = item.scale }

            val densityLocal = LocalDensity.current
            val bmpW = item.bitmap.width.toFloat()
            val bmpH = item.bitmap.height.toFloat()
            val canvasW = canvasBounds.width.toFloat()
            val canvasH = canvasBounds.height.toFloat()

            val initialFitFraction = 0.6f
            val scaleToFitCanvas = minOf(canvasW / bmpW, canvasH / bmpH, 1f) * initialFitFraction
            val (baseFrameWpx, baseFrameHpx) = remember(item.id, canvasBounds.width, canvasBounds.height) {
                val w = (bmpW * scaleToFitCanvas).coerceAtLeast(1f)
                val h = (bmpH * scaleToFitCanvas).coerceAtLeast(1f)
                w to h
            }

            // Don't let the image shrink below 48 dp when resizing
            val minFramePx = with(densityLocal) { 48.dp.toPx() }
            val minScaleW = (minFramePx / baseFrameWpx).coerceAtMost(1f)
            val minScaleH = (minFramePx / baseFrameHpx).coerceAtMost(1f)
            val minScale = maxOf(minScaleW, minScaleH)

            val frameScale = userScale.coerceIn(minScale, 8f)
            val frameWpx = baseFrameWpx * frameScale
            val frameHpx = baseFrameHpx * frameScale
            val (frameWdp, frameHdp) = remember(userScale, baseFrameWpx, baseFrameHpx) {
                with(densityLocal) { frameWpx.toDp() to frameHpx.toDp() }
            }

            Box(
                modifier = Modifier
                    .size(width = frameWdp, height = frameHdp)
                    .offset { IntOffset(frameOffset.x.roundToInt(), frameOffset.y.roundToInt()) }
                    .graphicsLayer { alpha = if (draggingCanvasItemId == item.id) 0f else 1f }
                    .clip(RoundedCornerShape(1.dp))
                    .clipToBounds()
                    .background(Color.Black)
                    .pointerInput("frameDragOrPinch-${item.id}") {
                        var pendingOffset: Offset? = null
                        var pendingScale: Float? = null
                        awaitEachGesture {
                            //Listen for gestures on the canvas
                            do {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.any { it.pressed }
                                if (!pressed) break

                                val pointers = event.changes.count { it.pressed }

                                // Two pointers so we are resizing
                                if (pointers >= 2) {
                                    val oldScale = userScale
                                    val zoom = event.calculateZoom()

                                    val maxScaleW = canvasBounds.width.toFloat() / baseFrameWpx
                                    val maxScaleH = canvasBounds.height.toFloat() / baseFrameHpx
                                    val maxAllowedScale = minOf(maxScaleW, maxScaleH, 8f)

                                    val newScale = (oldScale * zoom).coerceIn(minScale, maxAllowedScale)
                                    val k = if (oldScale == 0f) 1f else newScale / oldScale

                                    val centroid = event.calculateCentroid(useCurrent = true)
                                    val deltaTopLeft = centroid * (1f - k)
                                    val unclamped = frameOffset + deltaTopLeft

                                    val newW = baseFrameWpx * newScale
                                    val newH = baseFrameHpx * newScale
                                    val maxX = (canvasBounds.width - newW).coerceAtLeast(0f)
                                    val maxY = (canvasBounds.height - newH).coerceAtLeast(0f)

                                    frameOffset = Offset(
                                        x = unclamped.x.coerceIn(0f, maxX),
                                        y = unclamped.y.coerceIn(0f, maxY)
                                    )
                                    userScale = newScale
                                    pendingOffset = frameOffset
                                    pendingScale = newScale

                                    event.changes.forEach { c ->
                                        if (c.positionChange() != Offset.Zero) c.consume()
                                    }
                                } else {
                                    // Once pointer.  We are moving the image.
                                    val change = event.changes.first { it.pressed }
                                    val delta = change.positionChange()
                                    if (delta != Offset.Zero) {
                                        val currentScale = userScale.coerceIn(minScale, 8f)
                                        val currentFrameW = baseFrameWpx * currentScale
                                        val currentFrameH = baseFrameHpx * currentScale
                                        val maxX = (canvasBounds.width - currentFrameW).coerceAtLeast(0f)
                                        val maxY = (canvasBounds.height - currentFrameH).coerceAtLeast(0f)

                                        frameOffset = Offset(
                                            (frameOffset.x + delta.x).coerceIn(0f, maxX),
                                            (frameOffset.y + delta.y).coerceIn(0f, maxY)
                                        )
                                        pendingOffset = frameOffset
                                        change.consume()
                                    }
                                }
                            } while (true)
                            // Commit final values once per gesture
                            pendingOffset?.let { viewModel.updateOffset(item.id, it) }
                            pendingScale?.let { viewModel.updateUserScale(item.id, it) }
                            pendingOffset = null
                            pendingScale = null
                        }
                    }
            ) {
                val imgBitmap = remember(item.bitmap) { item.bitmap.asImageBitmap() }

                Image(
                    bitmap = imgBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CanvasComposablePreview() {
    // Sample bitmaps
    val bmpRed = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888).apply {
        eraseColor(android.graphics.Color.RED)
    }
    val bmpGreen = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888).apply {
        eraseColor(android.graphics.Color.GREEN)
    }
    val bmpBlue = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888).apply {
        eraseColor(android.graphics.Color.BLUE)
    }

    val images = listOf(
        CanvasImage(id = "1", bitmap = bmpRed),
        CanvasImage(id = "2", bitmap = bmpGreen),
        CanvasImage(id = "3", bitmap = bmpBlue)
    )

    CanvasComposable(
        initialOrientation = Configuration.ORIENTATION_PORTRAIT,
        canvasBounds = IntRect(0, 0, 500, 500),
        onCanvasBoundsChange = {},
        canvasImages = images,
        draggingCanvasItemId = null
    )
}
