// PROVENANCE: BASELINE
// Established: 2026-07-08   Cut-line: aa82f75   Note: pre-existing code; provenance not established; exempt from DDR/gate.
package com.example.imagecarousel.presentation

import com.example.imagecarousel.domain.Image

data class CarouselUiState(
    val loading: Boolean = false,
    val images: List<Image> = emptyList(),
    val error: String? = null
)