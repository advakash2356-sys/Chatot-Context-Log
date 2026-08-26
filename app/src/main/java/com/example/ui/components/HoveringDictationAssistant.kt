package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ActiveAccent
import com.example.ui.theme.ActiveAccentSubtle
import com.example.ui.theme.ActiveDestructive
import com.example.ui.theme.MonoBorder
import com.example.ui.theme.MonoSurface
import com.example.ui.theme.MonoSurfaceElevated
import com.example.ui.theme.MonoTextMuted
import com.example.ui.theme.MonoTextSecondary
import com.example.ui.theme.MonoWhite

/**
 * Hovering Dictation Button that appears whenever the user is typing or focusing on an input field.
 * Allows one-tap speech-to-text dictation that appends directly into the active typing context.
 */
@Composable
fun HoveringDictationButton(
  isDictating: Boolean,
  onToggleDictation: () -> Unit,
  modifier: Modifier = Modifier,
  label: String = "Dictate",
  compact: Boolean = false,
  testTag: String = "hovering_dictation_button"
) {
  val infiniteTransition = rememberInfiniteTransition(label = "hover_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 0.8f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow_alpha"
  )

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(24.dp))
      .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
      .background(if (isDictating) ActiveAccentSubtle else MonoSurfaceElevated)
      .border(
        width = 1.5.dp,
        color = if (isDictating) ActiveAccent.copy(alpha = glowAlpha) else MonoBorder,
        shape = RoundedCornerShape(24.dp)
      )
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) { onToggleDictation() }
      .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = 7.dp)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (isDictating) {
        // Active Pulsing Indicator
        Box(
          modifier = Modifier
            .size(10.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .background(ActiveAccent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.Default.Stop,
          contentDescription = "Stop Dictating",
          tint = ActiveDestructive,
          modifier = Modifier.size(16.dp)
        )
        if (!compact) {
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Listening...",
            style = TextStyle(
              color = ActiveAccent,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            )
          )
        }
      } else {
        // Idle Hover Mic
        Icon(
          imageVector = Icons.Default.Mic,
          contentDescription = "Dictate Speech",
          tint = if (compact) MonoTextSecondary else ActiveAccent,
          modifier = Modifier.size(16.dp)
        )
        if (!compact) {
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = label,
            style = TextStyle(
              color = MonoWhite,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              letterSpacing = 0.5.sp
            )
          )
        }
      }
    }
  }
}

/**
 * Floating Hovering Assistant Pill that docks over the view when typing is in progress.
 */
@Composable
fun FloatingDictationAssistant(
  isVisible: Boolean,
  isDictating: Boolean,
  onToggleDictation: () -> Unit,
  modifier: Modifier = Modifier,
  statusText: String = "Tap to speak into field"
) {
  AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + scaleIn(),
    exit = fadeOut() + scaleOut(),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(30.dp))
        .background(MonoSurface)
        .border(1.5.dp, if (isDictating) ActiveAccent else MonoBorder, RoundedCornerShape(30.dp))
        .clickable { onToggleDictation() }
        .padding(horizontal = 16.dp, vertical = 10.dp)
        .testTag("floating_dictation_assistant"),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(if (isDictating) ActiveAccentSubtle else MonoSurfaceElevated)
          .border(1.dp, if (isDictating) ActiveAccent else MonoBorder, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isDictating) Icons.Default.Stop else Icons.Default.Mic,
          contentDescription = "Dictate",
          tint = if (isDictating) ActiveAccent else MonoWhite,
          modifier = Modifier.size(16.dp)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Text(
        text = if (isDictating) "Listening & typing live..." else statusText,
        style = TextStyle(
          color = if (isDictating) ActiveAccent else MonoTextSecondary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
      )
    }
  }
}
