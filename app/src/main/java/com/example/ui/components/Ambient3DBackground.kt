package com.example.ui.components

import android.graphics.RuntimeShader
import android.os.Build
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.ui.theme.AcidGreen
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonViolet
import org.intellij.lang.annotations.Language
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// AGSL Runtime Shader for Android 13+ (API 33+)
@Language("AGSL")
private const val MESH_GRADIENT_SHADER = """
    uniform float2 uResolution;
    uniform float uTime;
    uniform float uAmplitude;
    uniform float4 uColor1;
    uniform float4 uColor2;
    uniform float4 uColor3;
    uniform float4 uColor4;

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / max(uResolution, float2(1.0, 1.0));
        float warp = uAmplitude * 2.2;
        
        float2 p = uv * 2.0 - 1.0;
        p.x *= (uResolution.x / max(uResolution.y, 1.0));
        
        // Dynamic fluid mesh deformation equations
        float wave1 = sin(p.x * 2.8 + uTime * 0.9 + p.y * (1.8 + warp)) * 0.5 + 0.5;
        float wave2 = cos(p.y * 3.2 - uTime * 0.7 + p.x * 2.2) * 0.5 + 0.5;
        float wave3 = sin(length(p) * 4.0 - uTime * 1.4 + warp * 3.0) * 0.5 + 0.5;
        
        float b1 = smoothstep(0.15, 0.85, wave1);
        float b2 = smoothstep(0.20, 0.80, wave2 * (1.0 + warp * 0.6));
        float b3 = smoothstep(0.25, 0.75, wave3);
        
        half4 col = half4(mix(uColor4, uColor2, b1 * 0.65));
        col = half4(mix(col, uColor1, b2 * (0.55 + warp * 0.45)));
        col = half4(mix(col, uColor3, b3 * (0.35 + warp * 0.65)));
        
        // Subtle ambient radial falloff
        float distFromCenter = length(uv - float2(0.5, 0.5));
        float vignette = clamp(1.0 - distFromCenter * 0.75, 0.15, 1.0);
        col.rgb *= vignette;
        col.a = 1.0;
        
        return col;
    }
"""

/**
 * Ambient3DBackground renders an interactive, real-time Canvas animated mesh gradient background.
 * - Utilizes AGSL RuntimeShader on Android 13+ (API 33+) with real-time uniform bindings.
 * - Layered with a 3D deformable topological grid mesh in Canvas that reacts to microphone amplitude.
 * - Fluidly warps in real time when voice audio is captured.
 */
@Composable
fun Ambient3DBackground(
    isRecording: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_mesh_time")

    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 62.831853f, // 20 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 15000 else 30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mesh_time_sec"
    )

    // Pre-cache runtime shader instance if supported
    val runtimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                RuntimeShader(MESH_GRADIENT_SHADER)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .zIndex(0f)
    ) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val effectiveAmp = if (isRecording) amplitude.coerceIn(0.05f, 1.0f) else 0.04f
        var shaderDrawn = false

        if (runtimeShader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                // Configure AGSL Shader uniforms
                runtimeShader.setFloatUniform("uResolution", width, height)
                runtimeShader.setFloatUniform("uTime", animTime)
                runtimeShader.setFloatUniform("uAmplitude", effectiveAmp)

                val c1 = if (isRecording) Color(0xFFEF4444) else ElectricCyan
                val c2 = NeonViolet
                val c3 = if (isRecording) NeonAmber else AcidGreen
                val c4 = CyberBackground

                runtimeShader.setColorUniform("uColor1", android.graphics.Color.valueOf(c1.red, c1.green, c1.blue, 1.0f))
                runtimeShader.setColorUniform("uColor2", android.graphics.Color.valueOf(c2.red, c2.green, c2.blue, 1.0f))
                runtimeShader.setColorUniform("uColor3", android.graphics.Color.valueOf(c3.red, c3.green, c3.blue, 1.0f))
                runtimeShader.setColorUniform("uColor4", android.graphics.Color.valueOf(c4.red, c4.green, c4.blue, 1.0f))

                drawRect(brush = ShaderBrush(runtimeShader))
                shaderDrawn = true
            } catch (e: Throwable) {
                shaderDrawn = false
            }
        }
        
        if (!shaderDrawn) {
            // High-Performance Multi-Orb Radial Shader Mesh Fallback
            drawRect(color = CyberBackground)

            val baseAmpFactor = effectiveAmp * 2.2f
            val orb1X = width * 0.35f + sin(animTime.toDouble()).toFloat() * (width * 0.20f)
            val orb1Y = height * 0.30f + cos(animTime.toDouble() * 0.8).toFloat() * (height * 0.12f)
            val orb1Radius = width * (0.6f + baseAmpFactor * 0.35f)

            val orb2X = width * 0.70f - cos(animTime.toDouble() * 0.7).toFloat() * (width * 0.22f)
            val orb2Y = height * 0.72f - sin(animTime.toDouble() * 0.9).toFloat() * (height * 0.15f)
            val orb2Radius = width * (0.7f + baseAmpFactor * 0.40f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (isRecording) Color(0xFFEF4444) else ElectricCyan).copy(alpha = 0.38f + effectiveAmp * 0.3f),
                        NeonViolet.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = orb1Radius
                ),
                center = Offset(orb1X, orb1Y),
                radius = orb1Radius,
                blendMode = BlendMode.Screen
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (isRecording) NeonAmber else NeonViolet).copy(alpha = 0.32f + effectiveAmp * 0.3f),
                        AcidGreen.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = orb2Radius
                ),
                center = Offset(orb2X, orb2Y),
                radius = orb2Radius,
                blendMode = BlendMode.Screen
            )
        }

        // --- 3D DEFORMABLE TOPOLOGICAL WIREFRAME MESH GRID ---
        // Render 3D undulating topographic isometric grid lines that ripple dynamically with mic audio
        val cols = 14
        val rows = 22
        val cellWidth = width / cols
        val cellHeight = height / rows

        // Compute 3D displaced vertices
        val gridPoints = Array(rows + 1) { r ->
            Array(cols + 1) { c ->
                val normX = c.toFloat() / cols
                val normY = r.toFloat() / rows

                // Wave perturbation function based on distance, time, and amplitude
                val distCenter = sqrt((normX - 0.5f) * (normX - 0.5f) + (normY - 0.5f) * (normY - 0.5f))
                val wavePhase = distCenter * 14.0 - animTime * 2.5
                val displacementZ = (sin(wavePhase) * (effectiveAmp * 32.dp.toPx() + 6.dp.toPx())).toFloat()

                // Perspective isometric projection offset
                val px = c * cellWidth + (normY - 0.5f) * 12.dp.toPx()
                val py = r * cellHeight - displacementZ

                Offset(px, py)
            }
        }

        val meshStrokeColor = if (isRecording) {
            Color(0xFFEF4444).copy(alpha = (0.12f + effectiveAmp * 0.25f).coerceIn(0.1f, 0.45f))
        } else {
            ElectricCyan.copy(alpha = (0.08f + effectiveAmp * 0.18f).coerceIn(0.06f, 0.3f))
        }

        // Draw horizontal mesh lines
        for (r in 0..rows) {
            val path = Path()
            path.moveTo(gridPoints[r][0].x, gridPoints[r][0].y)
            for (c in 1..cols) {
                val prev = gridPoints[r][c - 1]
                val curr = gridPoints[r][c]
                val midX = (prev.x + curr.x) / 2f
                val midY = (prev.y + curr.y) / 2f
                path.quadraticTo(prev.x, prev.y, midX, midY)
            }
            path.lineTo(gridPoints[r][cols].x, gridPoints[r][cols].y)
            drawPath(path, color = meshStrokeColor, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))
        }

        // Draw vertical mesh lines
        for (c in 0..cols) {
            val path = Path()
            path.moveTo(gridPoints[0][c].x, gridPoints[0][c].y)
            for (r in 1..rows) {
                val prev = gridPoints[r - 1][c]
                val curr = gridPoints[r][c]
                val midX = (prev.x + curr.x) / 2f
                val midY = (prev.y + curr.y) / 2f
                path.quadraticTo(prev.x, prev.y, midX, midY)
            }
            path.lineTo(gridPoints[rows][c].x, gridPoints[rows][c].y)
            drawPath(path, color = meshStrokeColor.copy(alpha = meshStrokeColor.alpha * 0.6f), style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round))
        }

        // Highlight reactive vertex glow points
        if (effectiveAmp > 0.08f) {
            for (r in 2 until rows step 3) {
                for (c in 2 until cols step 3) {
                    val pt = gridPoints[r][c]
                    val ptGlowRadius = (2.5.dp.toPx() + effectiveAmp * 5.dp.toPx())
                    drawCircle(
                        color = (if (isRecording) NeonAmber else ElectricCyan).copy(alpha = (effectiveAmp * 0.6f).coerceIn(0.15f, 0.75f)),
                        radius = ptGlowRadius,
                        center = pt
                    )
                }
            }
        }
    }
}
