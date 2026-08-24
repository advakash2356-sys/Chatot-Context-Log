package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

/**
 * TactileRecordButton provides a skeuomorphic 3D hardware button experience with mechanical depression physics,
 * specular edge lighting, and dynamic glow shadows.
 */
@Composable
fun TactileRecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 3D mechanical physical depression
    val translationY by animateDpAsState(
        targetValue = if (isPressed) 6.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "button_depression"
    )

    val scaleFactor by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "button_scale"
    )

    val buttonShape = RoundedCornerShape(24.dp)

    val idleGradient = Brush.horizontalGradient(
        listOf(Color(0xFFF59E0B), Color(0xFFEA580C), Color(0xFFE11D48))
    )
    val recordingGradient = Brush.horizontalGradient(
        listOf(Color(0xFFEF4444), Color(0xFFDC2626), Color(0xFF991B1B))
    )
    val activeBorderColor = if (isRecording) Color(0xFFFCA5A5) else Color(0xFFFDE68A)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .zIndex(10f)
            .scale(scaleFactor)
            // Simulated mechanical socket chassis
            .drawBehind {
                drawRoundRect(
                    color = if (isRecording) Color(0xFF7F1D1D) else Color(0xFF78350F),
                    topLeft = Offset(0f, 8.dp.toPx()),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            }
            .offset(y = translationY)
            .shadow(
                elevation = if (isPressed) 3.dp else 16.dp,
                shape = buttonShape,
                spotColor = if (isRecording) Color(0xFFEF4444).copy(alpha = 0.65f) else Color(0xFFF59E0B).copy(alpha = 0.55f),
                ambientColor = Color.Black
            )
            .clip(buttonShape)
            .background(if (isRecording) recordingGradient else idleGradient)
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        activeBorderColor.copy(alpha = 0.8f),
                        Color.White.copy(alpha = 0.2f)
                    )
                ),
                shape = buttonShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("tactile_record_button"),
        contentAlignment = Alignment.Center
    ) {
        // Specular highlight line on top edge
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.92f)
                .height(1.5.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                tint = if (isRecording) Color.White else Color(0xFF0F172A),
                modifier = Modifier.size(28.dp)
            )

            Text(
                text = if (isRecording) "STOP & SYNTHESIZE" else "TAP TO CAPTURE",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                color = if (isRecording) Color.White else Color(0xFF0F172A)
            )

            if (isRecording) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
