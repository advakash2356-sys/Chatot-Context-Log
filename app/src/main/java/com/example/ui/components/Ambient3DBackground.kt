package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ambient3DBackground renders an interactive 3D mesh gradient canvas.
 * When Idle: smoothly breathes in an ambient orbital cycle.
 * When Recording: warps and pulses dynamically in sync with capture amplitude.
 */
@Composable
fun Ambient3DBackground(
    isRecording: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    // Continuous orbital phase transition
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_3d_mesh_loop")

    val breathingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 3000 else 8500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital_phase"
    )

    // Palette tokens
    val baseDarkSlate = Color(0xFF020617)
    val primaryCyan = Color(0xFF06B6D4)
    val vibrantPurple = Color(0xFF8B5CF6)
    val saffronAmber = Color(0xFFF59E0B)
    val recordingCrimson = Color(0xFFEF4444)
    val deepIndigo = Color(0xFF1E1B4B)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .zIndex(0f)
    ) {
        val width = size.width
        val height = size.height

        // 1. Deep Obsidian Base
        drawRect(color = baseDarkSlate)

        // Reactive expansion calculation
        val ampFactor = if (isRecording) (amplitude * 2.0f).coerceIn(0.08f, 1.4f) else 0.05f
        val pulseRadius1 = width * (0.55f + ampFactor * 0.35f)
        val pulseRadius2 = width * (0.65f + ampFactor * 0.40f)

        // 2. Base Indigo Horizon Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    deepIndigo.copy(alpha = 0.65f),
                    Color.Transparent
                ),
                center = Offset(width * 0.5f, height * 0.6f),
                radius = width * 0.9f
            ),
            center = Offset(width * 0.5f, height * 0.6f),
            radius = width * 0.9f,
            blendMode = BlendMode.Screen
        )

        // 3. Orb 1: Upper Orbital Sphere (Cyan to Electric Purple)
        val orb1X = width * 0.35f + sin(breathingPhase.toDouble()).toFloat() * (width * 0.18f)
        val orb1Y = height * 0.28f + cos(breathingPhase.toDouble()).toFloat() * (height * 0.10f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    (if (isRecording) recordingCrimson else primaryCyan).copy(
                        alpha = if (isRecording) 0.42f else 0.26f
                    ),
                    vibrantPurple.copy(alpha = if (isRecording) 0.20f else 0.14f),
                    Color.Transparent
                ),
                center = Offset(orb1X, orb1Y),
                radius = pulseRadius1
            ),
            center = Offset(orb1X, orb1Y),
            radius = pulseRadius1,
            blendMode = BlendMode.Screen
        )

        // 4. Orb 2: Lower Orbital Sphere (Saffron Amber to Rose)
        val orb2X = width * 0.68f - cos(breathingPhase.toDouble()).toFloat() * (width * 0.20f)
        val orb2Y = height * 0.70f - sin(breathingPhase.toDouble()).toFloat() * (height * 0.12f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    (if (isRecording) saffronAmber else vibrantPurple).copy(
                        alpha = if (isRecording) 0.38f else 0.22f
                    ),
                    (if (isRecording) recordingCrimson else primaryCyan).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(orb2X, orb2Y),
                radius = pulseRadius2
            ),
            center = Offset(orb2X, orb2Y),
            radius = pulseRadius2,
            blendMode = BlendMode.Screen
        )

        // 5. Reactive Speech Energy Core (Active during recording)
        if (isRecording) {
            val coreRadius = (width * 0.48f) * (1f + ampFactor * 0.6f)
            val coreX = width * 0.5f + (sin((breathingPhase * 2).toDouble()).toFloat() * 12f)
            val coreY = height * 0.48f + (cos((breathingPhase * 2).toDouble()).toFloat() * 12f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        saffronAmber.copy(alpha = (0.28f + ampFactor * 0.35f).coerceAtMost(0.65f)),
                        recordingCrimson.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(coreX, coreY),
                    radius = coreRadius
                ),
                center = Offset(coreX, coreY),
                radius = coreRadius,
                blendMode = BlendMode.Plus
            )
        }
    }
}
