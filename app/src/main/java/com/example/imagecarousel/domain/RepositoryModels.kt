// PROVENANCE: BASELINE
// Established: 2026-07-08   Cut-line: aa82f75   Note: pre-existing code; provenance not established; exempt from DDR/gate.
package com.example.imagecarousel.domain

import android.graphics.Bitmap

data class Image(
    val bitmap: Bitmap? = null
)

data class ImageResponse(
    val images: List<Image>
)