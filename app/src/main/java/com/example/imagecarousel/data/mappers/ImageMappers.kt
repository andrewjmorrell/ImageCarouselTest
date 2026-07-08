// PROVENANCE: BASELINE
// Established: 2026-07-08   Cut-line: aa82f75   Note: pre-existing code; provenance not established; exempt from DDR/gate.
package com.example.imagecarousel.data.mappers

import com.example.imagecarousel.data.models.ImageDto
import com.example.imagecarousel.data.models.ImageResponseDto
import com.example.imagecarousel.domain.Image
import com.example.imagecarousel.domain.ImageResponse

fun ImageDto.toDomain(): Image = Image(
    bitmap = image
)

fun ImageResponseDto.toDomain(): ImageResponse = ImageResponse(
    images = images.map { it.toDomain() }
)