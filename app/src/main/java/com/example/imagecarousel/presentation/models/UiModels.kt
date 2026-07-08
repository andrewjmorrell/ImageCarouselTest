// PROVENANCE: BASELINE
// Established: 2026-07-08   Cut-line: aa82f75   Note: pre-existing code; provenance not established; exempt from DDR/gate.
package com.example.imagecarousel.presentation.models

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import java.util.UUID

data class CanvasImage(
    val id: String = UUID.randomUUID().toString(),
    val bitmap: Bitmap,
    var offset: Offset = Offset.Zero,
    var scale: Float = 1f
)