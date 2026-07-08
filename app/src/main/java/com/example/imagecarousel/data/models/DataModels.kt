// PROVENANCE: BASELINE
// Established: 2026-07-08   Cut-line: aa82f75   Note: pre-existing code; provenance not established; exempt from DDR/gate.
package com.example.imagecarousel.data.models

import android.graphics.Bitmap

//This is simple but could be expanded to include additional metadata
data class ImageDto(
    val image: Bitmap
)

data class ImageResponseDto(
    val images: List<ImageDto>
)