// PROVENANCE: BASELINE
// Established: 2026-07-08   Cut-line: aa82f75   Note: pre-existing code; provenance not established; exempt from DDR/gate.
package com.example.imagecarousel.domain.repository

import com.example.imagecarousel.domain.ImageResponse

sealed class DataState<out T> {
    data class Success(val data: ImageResponse) : DataState<ImageResponse>()
    data class Error(val message: String) : DataState<Nothing>()
    object Loading : DataState<Nothing>()
}