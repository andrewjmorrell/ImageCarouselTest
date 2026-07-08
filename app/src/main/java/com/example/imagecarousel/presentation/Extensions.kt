// PROVENANCE: BASELINE
// Established: 2026-07-08   Cut-line: aa82f75   Note: pre-existing code; provenance not established; exempt from DDR/gate.
package com.example.imagecarousel.presentation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import kotlin.math.roundToInt

fun IntRect.contains(point: Offset): Boolean {
    val x = point.x.roundToInt()
    val y = point.y.roundToInt()
    return x in left..right && y in top..bottom
}

fun Dp.toPx(density: androidx.compose.ui.unit.Density): Float =
    with(density) { this@toPx.toPx() }