package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * FastTactileWaveform renders responsive audio equalizer bars on a single Canvas DrawScope
 * to avoid per-frame composable allocations.
 */
@Composable
fun FastTactileWaveform(
    amplitude: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 30
) {
    val activeBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF22D3EE), Color(0xFFA855F7), Color(0xFFF59E0B))
    )
    val idleBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF334155).copy(alpha = 0.5f), Color(0xFF1E293B).copy(alpha = 0.5f))
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barWidth = totalWidth / (barCount * 1.45f)
        val gap = barWidth * 0.45f

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            val dynamicScale = if (isRecording) {
                // Wave distortion based on speech RMS energy
                val waveOffset = sin((progress * 6.283f) + (amplitude * 12f))
                (amplitude * 0.75f + (waveOffset * 0.28f).toFloat()).coerceIn(0.12f, 1.0f)
            } else {
                0.10f
            }

            val barHeight = (canvasHeight * dynamicScale).coerceAtLeast(6.dp.toPx())
            val xOffset = i * (barWidth + gap)
            val yOffset = (canvasHeight - barHeight) / 2f

            drawRoundRect(
                brush = if (isRecording) activeBrush else idleBrush,
                topLeft = Offset(xOffset, yOffset),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
