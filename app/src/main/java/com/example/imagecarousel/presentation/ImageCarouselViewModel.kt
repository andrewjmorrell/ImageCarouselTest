package com.example.imagecarousel.presentation

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imagecarousel.domain.ImageResponse
import com.example.imagecarousel.domain.repository.DataState
import com.example.imagecarousel.domain.usecases.GetImagesUseCase
import com.example.imagecarousel.presentation.models.CanvasImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ImageCarouselViewModel @Inject constructor(private val getImagesUseCase: GetImagesUseCase): ViewModel() {

    private val _uiState = MutableStateFlow<CarouselUiState>(CarouselUiState())
    val uiState: StateFlow<CarouselUiState> = _uiState

    private val _canvasImages = mutableStateListOf<CanvasImage>()
    val canvasImages: List<CanvasImage> get() = _canvasImages

    val undoCommands = ArrayDeque<LastCommand>()
    val redoCommands = ArrayDeque<LastCommand>()

    // Load the images from the use case.  Defaults to 5 but can be changed.
    fun loadImages(count: Int = 5) {
        viewModelScope.launch(Dispatchers.IO) {
            getImagesUseCase.getImages(count).onStart {
                _uiState.value = CarouselUiState(loading = true)
            }.catch {
                _uiState.value = CarouselUiState(error = it.message)
            }.collect { dataState ->
                when (dataState) {
                    is DataState.Success -> {
                        _uiState.value = CarouselUiState(images = dataState.data.images)
                    }
                    is DataState.Error -> {
                        _uiState.value = CarouselUiState(loading = false, error = dataState.message)
                    }
                    is DataState.Loading -> {
                        _uiState.value = CarouselUiState(loading = true)
                    }
                }
            }

        }
    }

    fun addCanvasImage(bm: Bitmap, offset: Offset, userScale: Float = 1f, isUndo: Boolean = false) {
        val id = UUID.randomUUID().toString()
        _canvasImages += CanvasImage(
            id = id,
            bitmap = bm,
            offset = offset,
            scale = userScale
        )
        if (!isUndo) {
            undoCommands.addFirst(LastCommand("add",
                id,
                null,
                null))

        }
    }

    fun removeCanvasImage(id: String) {
        _canvasImages.removeAll { it.id == id }
    }

    fun updateOffset(id: String, offset: Offset, isUndo: Boolean = false) {

        Log.d("UpdateOffset", "Update Offset")
        _canvasImages.indexOfFirst { it.id == id }
            .takeIf { it >= 0 }?.let { idx ->
                if (!isUndo) {
                    undoCommands.addFirst(LastCommand("offset",
                        id,
                        _canvasImages[idx].offset,
                        null))

                }
                _canvasImages[idx] = _canvasImages[idx].copy(offset = offset)

            }
    }

    fun updateUserScale(id: String, userScale: Float, isUndo: Boolean = false) {
        _canvasImages.indexOfFirst { it.id == id }
            .takeIf { it >= 0 }?.let { idx ->
                if (!isUndo) {
                    undoCommands.addFirst(LastCommand("scale",
                        id,
                        null,
                        _canvasImages[idx].scale))

                }
                _canvasImages[idx] = _canvasImages[idx].copy(scale = userScale)
            }
    }

    fun undo() {
        val lastCommand = undoCommands.firstOrNull()
        when (lastCommand?.command) {
            "scale" -> {
                updateUserScale(lastCommand.id, lastCommand.scale!!, true)
                undoCommands.removeFirst()
            }
            "offset" -> {
                updateOffset(lastCommand.id, lastCommand.offset!!, true)
                undoCommands.removeFirst()
            }
            "add" -> {
                removeCanvasImage(lastCommand.id)
                undoCommands.removeFirst()
            }
            else -> {

            }
        }
    }
}

data class LastCommand(val command: String ,
                       val id: String,
                       val offset: Offset?,
                       val scale: Float?)

