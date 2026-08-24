package com.example.data.audio

import java.io.File

sealed interface AudioCaptureState {
    object Idle : AudioCaptureState
    data class Recording(val durationSeconds: Int, val amplitude: Float) : AudioCaptureState
    data class Completed(val outputFile: File? = null) : AudioCaptureState
    data class Error(val message: String) : AudioCaptureState
}
