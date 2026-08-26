package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AcidGreen
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonViolet
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3D Spatial Holographic Core:
 * A true 3D spherical particle and gyro-ring system rendered in real-time.
 * Features 3D coordinate projection, interactive 3D rotation via drag, and audio reactivity.
 */
@Composable
fun SpatialHologramCore(
    amplitude: Float = 0.1f,
    isInteracting: Boolean = false,
    colorAccent: Color = ElectricCyan,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spatial_core_spin")
    val autoRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auto_rot"
    )

    var manualRotationX by remember { mutableFloatStateOf(0.35f) }
    var manualRotationY by remember { mutableFloatStateOf(0f) }

    val effectiveRotY = manualRotationY + autoRotationAngle
    val effectiveRotX = manualRotationX

    // 3D Point model
    val baseRadius = 85f
    val particleCount = 72
    val particles = remember {
        val points = mutableListOf<FloatArray>()
        val phi = (1 + sqrt(5.0)) / 2.0 // Golden ratio for Fibonacci sphere
        for (i in 0 until particleCount) {
            val y = 1 - (i / (particleCount - 1f)) * 2 // -1 to 1
            val radiusAtY = sqrt(1 - y * y)
            val theta = phi * i * 2 * PI
            val x = cos(theta).toFloat() * radiusAtY
            val z = sin(theta).toFloat() * radiusAtY
            points.add(floatArrayOf(x.toFloat(), y, z.toFloat()))
        }
        points
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    manualRotationY += dragAmount.x * 0.015f
                    manualRotationX = (manualRotationX - dragAmount.y * 0.015f).coerceIn(-1.2f, 1.2f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val dynamicScale = (size.minDimension / 2.3f) * (1f + (amplitude * 0.55f).coerceIn(0f, 0.45f))

            // 1. Ambient Volumetric Glow Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorAccent.copy(alpha = (0.45f + amplitude * 0.4f).coerceIn(0.2f, 0.85f)),
                        NeonViolet.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = dynamicScale * 0.9f
                ),
                radius = dynamicScale * 0.9f,
                blendMode = BlendMode.Screen
            )

            // 2. 3D Gyroscope Latitude & Longitude Rings
            val ringSteps = 60
            val ringPoints1 = mutableListOf<Offset>()
            val ringPoints2 = mutableListOf<Offset>()
            val ringPoints3 = mutableListOf<Offset>()

            for (step in 0..ringSteps) {
                val theta = (step.toFloat() / ringSteps) * 2 * PI.toFloat()
                
                // Ring 1 (XY plane rotated)
                val rx1 = cos(theta)
                val ry1 = sin(theta)
                val rz1 = 0f
                // Rotate by RotY and RotX
                val (projX1, projY1, _) = project3D(rx1, ry1, rz1, effectiveRotX, effectiveRotY, dynamicScale, centerX, centerY)
                ringPoints1.add(Offset(projX1, projY1))

                // Ring 2 (XZ equatorial plane tilted)
                val rx2 = cos(theta)
                val ry2 = 0f
                val rz2 = sin(theta)
                val (projX2, projY2, _) = project3D(rx2, ry2, rz2, effectiveRotX + 0.6f, effectiveRotY * 1.3f, dynamicScale * 1.15f, centerX, centerY)
                ringPoints2.add(Offset(projX2, projY2))

                // Ring 3 (YZ polar ring)
                val rx3 = 0f
                val ry3 = cos(theta)
                val rz3 = sin(theta)
                val (projX3, projY3, _) = project3D(rx3, ry3, rz3, effectiveRotX - 0.4f, effectiveRotY * 0.7f, dynamicScale * 1.05f, centerX, centerY)
                ringPoints3.add(Offset(projX3, projY3))
            }

            fun drawProjectedRing(points: List<Offset>, color: Color, strokeWidth: Float) {
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = color,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            drawProjectedRing(ringPoints1, colorAccent.copy(alpha = 0.75f), 2.dp.toPx())
            drawProjectedRing(ringPoints2, NeonViolet.copy(alpha = 0.6f), 1.5.dp.toPx())
            drawProjectedRing(ringPoints3, AcidGreen.copy(alpha = 0.5f), 1.2.dp.toPx())

            // 3. Project and Draw 3D Particle Cloud
            val projectedParticles = particles.map { p ->
                val (px, py, pz) = project3D(p[0], p[1], p[2], effectiveRotX, effectiveRotY, dynamicScale, centerX, centerY)
                Triple(Offset(px, py), pz, (p[1] + 1f) / 2f)
            }.sortedBy { it.second } // Sort by Z-depth for proper painter's occlusion!

            projectedParticles.forEach { (pos, z, colorLerp) ->
                val depthAlpha = ((z + 1.2f) / 2.4f).coerceIn(0.15f, 1f)
                val particleRadius = (2.5.dp.toPx() * (depthAlpha + 0.3f)) * (1f + amplitude * 0.8f)
                val particleColor = if (colorLerp > 0.5f) colorAccent else NeonViolet

                drawCircle(
                    color = particleColor.copy(alpha = depthAlpha),
                    radius = particleRadius,
                    center = pos
                )
            }
        }
    }
}

// 3D Projection Math Helper
private fun project3D(
    x: Float, y: Float, z: Float,
    rotX: Float, rotY: Float,
    scale: Float,
    cx: Float, cy: Float
): Triple<Float, Float, Float> {
    // Rotate Y
    val cosY = cos(rotY.toDouble()).toFloat()
    val sinY = sin(rotY.toDouble()).toFloat()
    val x1 = x * cosY + z * sinY
    val z1 = -x * sinY + z * cosY

    // Rotate X
    val cosX = cos(rotX.toDouble()).toFloat()
    val sinX = sin(rotX.toDouble()).toFloat()
    val y2 = y * cosX - z1 * sinX
    val z2 = y * sinX + z1 * cosX

    // Perspective projection (camera distance = 3.5)
    val distance = 3.2f
    val fov = 1.0f / (distance - (z2 * 0.7f))
    val projX = cx + (x1 * scale * fov)
    val projY = cy + (y2 * scale * fov)

    return Triple(projX, projY, z2)
}

/**
 * Spatial3DCard provides interactive 3D spatial gyro-tilt with pointer tracking,
 * specular dynamic refraction sheen, and mechanical bevel extrusion.
 */
@Composable
fun Spatial3DCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    accentColor: Color = ElectricCyan,
    elevation: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }
    var glintCenter by remember { mutableStateOf(Offset(0.5f, 0.5f)) }

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationX = tiltX.value
                rotationY = tiltY.value
                cameraDistance = 14f * density
                shadowElevation = elevation.toPx()
                this.shape = shape
                clip = false
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val normX = (offset.x / size.width) - 0.5f
                        val normY = (offset.y / size.height) - 0.5f
                        glintCenter = Offset(offset.x / size.width, offset.y / size.height)
                        
                        coroutineScope.launch {
                            tiltX.animateTo(-normY * 16f, spring(stiffness = Spring.StiffnessMediumLow))
                            tiltY.animateTo(normX * 16f, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                        tryAwaitRelease()
                        coroutineScope.launch {
                            tiltX.animateTo(0f, spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy))
                            tiltY.animateTo(0f, spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                    }
                )
            }
            // 3D Physical Extruded Bevel Underbody
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFF04060A),
                    topLeft = Offset(0f, 6.dp.toPx()),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                )
            }
            .shadow(elevation, shape, spotColor = accentColor.copy(alpha = 0.25f), ambientColor = Color.Black)
            .clip(shape)
            .background(CyberSurface)
            .border(
                width = 1.dp,
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.45f),
                        GlassBorder,
                        Color.White.copy(alpha = 0.04f)
                    ),
                    center = Offset(glintCenter.x * 600f, glintCenter.y * 400f),
                    radius = 350f
                ),
                shape = shape
            )
            .drawWithContent {
                drawContent()
                // Dynamic Specular Glass Glint
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(glintCenter.x * size.width, glintCenter.y * size.height),
                        radius = size.maxDimension * 0.7f
                    ),
                    blendMode = BlendMode.Screen
                )
            }
    ) {
        content()
    }
}
