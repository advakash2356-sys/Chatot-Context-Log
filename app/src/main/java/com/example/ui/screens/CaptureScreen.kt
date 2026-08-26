package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FloatingDictationAssistant
import com.example.ui.components.HoveringDictationButton
import com.example.ui.theme.ActiveAccent
import com.example.ui.theme.ActiveAccentSubtle
import com.example.ui.theme.ActiveDestructive
import com.example.ui.theme.ActiveSuccess
import com.example.ui.theme.MonoBackground
import com.example.ui.theme.MonoBorder
import com.example.ui.theme.MonoBorderSubtle
import com.example.ui.theme.MonoSurface
import com.example.ui.theme.MonoSurfaceElevated
import com.example.ui.theme.MonoTextMuted
import com.example.ui.theme.MonoTextPrimary
import com.example.ui.theme.MonoTextSecondary
import com.example.ui.theme.MonoWhite
import com.example.ui.viewmodel.DictationState
import com.example.ui.viewmodel.MainAppView
import com.example.ui.viewmodel.RoutingToast

@Composable
fun CaptureScreen(
  dictationState: DictationState,
  liveTranscript: String,
  rawVerifiedTranscript: String,
  recordingDurationSeconds: Int,
  micAmplitude: Float,
  toastMessage: RoutingToast?,
  vaultItemCount: Int,
  inlineDictationTarget: String? = null,
  onStartListening: () -> Unit,
  onStopListening: () -> Unit,
  onDiscard: () -> Unit,
  onUpdateVerifiedText: (String) -> Unit,
  onSaveAndProcess: (String?) -> Unit,
  onNavigateToVault: () -> Unit,
  onStartInlineDictation: (String, String, (String) -> Unit) -> Unit = { _, _, _ -> },
  onStopInlineDictation: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  var manualTextInput by remember { mutableStateOf("") }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MonoBackground)
        .padding(horizontal = 24.dp, vertical = 16.dp)
        .verticalScroll(scrollState),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top Minimalist View Switcher [ CAPTURE | VAULT (N) ]
      MinimalistViewSwitcher(
        activeView = MainAppView.CAPTURE,
        vaultCount = vaultItemCount,
        onSelectCapture = {},
        onSelectVault = onNavigateToVault
      )

      Spacer(modifier = Modifier.height(28.dp))

      // 5-State Machine Content Switcher
      AnimatedContent(
        targetState = dictationState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "dictation_state_anim"
      ) { state ->
        when (state) {
          DictationState.IDLE -> {
            IdleStateView(
              manualText = manualTextInput,
              isInlineDictating = inlineDictationTarget == "manual_input",
              onManualTextChange = { manualTextInput = it },
              onStartListening = onStartListening,
              onSubmitManualText = { text ->
                onSaveAndProcess(text)
                manualTextInput = ""
              },
              onToggleInlineDictate = {
                if (inlineDictationTarget == "manual_input") {
                  onStopInlineDictation()
                } else {
                  onStartInlineDictation("manual_input", manualTextInput) { updated ->
                    manualTextInput = updated
                  }
                }
              }
            )
          }

          DictationState.LISTENING -> {
            ListeningStateView(
              durationSeconds = recordingDurationSeconds,
              liveTranscript = liveTranscript,
              micAmplitude = micAmplitude,
              onStopListening = onStopListening
            )
          }

          DictationState.VERIFICATION -> {
            VerificationStateView(
              verifiedText = rawVerifiedTranscript,
              isInlineDictating = inlineDictationTarget == "verified_input",
              onTextChange = onUpdateVerifiedText,
              onSaveAndProcess = { onSaveAndProcess(rawVerifiedTranscript) },
              onDiscard = onDiscard,
              onToggleInlineDictate = {
                if (inlineDictationTarget == "verified_input") {
                  onStopInlineDictation()
                } else {
                  onStartInlineDictation("verified_input", rawVerifiedTranscript) { updated ->
                    onUpdateVerifiedText(updated)
                  }
                }
              }
            )
          }

          DictationState.PROCESSING -> {
            ProcessingStateView()
          }

          DictationState.CONFIRMATION -> {
            ConfirmationStateView(
              toastMessage = toastMessage,
              onNavigateToVault = onNavigateToVault,
              onNewCapture = onDiscard
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(32.dp))
    }

    // Floating Hovering Dictation Assistant when typing manual text in Idle mode
    if (dictationState == DictationState.IDLE && manualTextInput.isNotBlank()) {
      FloatingDictationAssistant(
        isVisible = true,
        isDictating = inlineDictationTarget == "manual_input",
        onToggleDictation = {
          if (inlineDictationTarget == "manual_input") {
            onStopInlineDictation()
          } else {
            onStartInlineDictation("manual_input", manualTextInput) { updated ->
              manualTextInput = updated
            }
          }
        },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(24.dp)
      )
    }
  }
}

/**
 * Clean Top Segmented Switcher
 */
@Composable
fun MinimalistViewSwitcher(
  activeView: MainAppView,
  vaultCount: Int,
  onSelectCapture: () -> Unit,
  onSelectVault: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(MonoSurface)
      .border(1.dp, MonoBorder, RoundedCornerShape(12.dp))
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    // Capture Tab Button
    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(8.dp))
        .background(if (activeView == MainAppView.CAPTURE) MonoSurfaceElevated else Color.Transparent)
        .border(
          width = if (activeView == MainAppView.CAPTURE) 1.dp else 0.dp,
          color = if (activeView == MainAppView.CAPTURE) MonoBorder else Color.Transparent,
          shape = RoundedCornerShape(8.dp)
        )
        .clickable { onSelectCapture() }
        .padding(vertical = 10.dp)
        .testTag("nav_tab_capture"),
      contentAlignment = Alignment.Center
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(if (activeView == MainAppView.CAPTURE) ActiveAccent else MonoTextMuted)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "CAPTURE",
          style = TextStyle(
            color = if (activeView == MainAppView.CAPTURE) MonoWhite else MonoTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
        )
      }
    }

    // Vault Tab Button
    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(8.dp))
        .background(if (activeView == MainAppView.VAULT) MonoSurfaceElevated else Color.Transparent)
        .border(
          width = if (activeView == MainAppView.VAULT) 1.dp else 0.dp,
          color = if (activeView == MainAppView.VAULT) MonoBorder else Color.Transparent,
          shape = RoundedCornerShape(8.dp)
        )
        .clickable { onSelectVault() }
        .padding(vertical = 10.dp)
        .testTag("nav_tab_vault"),
      contentAlignment = Alignment.Center
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          text = "VAULT",
          style = TextStyle(
            color = if (activeView == MainAppView.VAULT) MonoWhite else MonoTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
        )
        if (vaultCount > 0) {
          Spacer(modifier = Modifier.width(6.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(MonoBorder)
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "$vaultCount",
              style = TextStyle(
                color = MonoTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            )
          }
        }
      }
    }
  }
}

/**
 * STATE 1 - IDLE VIEW
 */
@Composable
private fun IdleStateView(
  manualText: String,
  isInlineDictating: Boolean,
  onManualTextChange: (String) -> Unit,
  onStartListening: () -> Unit,
  onSubmitManualText: (String) -> Unit,
  onToggleInlineDictate: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "Voice Capture",
      style = TextStyle(
        color = MonoWhite,
        fontSize = 28.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-0.5).sp
      )
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Speak freely. The system autonomously routes action items, calendar schedules, and synthesized notes to your Vault.",
      style = TextStyle(
        color = MonoTextSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center
      ),
      modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(48.dp))

    // Primary Tactile Capture Button
    Box(
      modifier = Modifier
        .size(120.dp)
        .clip(CircleShape)
        .background(MonoSurface)
        .border(1.5.dp, MonoBorder, CircleShape)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) { onStartListening() }
        .testTag("capture_record_button"),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(CircleShape)
          .background(MonoSurfaceElevated)
          .border(1.dp, MonoBorder, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Mic,
          contentDescription = "Start Voice Dictation",
          tint = MonoWhite,
          modifier = Modifier.size(36.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "TAP TO RECORD",
      style = TextStyle(
        color = MonoTextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp
      )
    )

    Spacer(modifier = Modifier.height(48.dp))

    // Minimalist Manual Input Card with Hovering Dictation Button
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(MonoSurface)
        .border(1.dp, MonoBorderSubtle, RoundedCornerShape(12.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "OR TYPE THOUGHTS",
          style = TextStyle(
            color = MonoTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
        )

        // Hovering Dictation Button on the Typing Header
        HoveringDictationButton(
          isDictating = isInlineDictating,
          onToggleDictation = onToggleInlineDictate,
          label = "Dictate",
          compact = false,
          testTag = "manual_typing_hover_dictate"
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      BasicTextField(
        value = manualText,
        onValueChange = onManualTextChange,
        textStyle = TextStyle(
          color = MonoTextPrimary,
          fontSize = 14.sp,
          lineHeight = 20.sp
        ),
        cursorBrush = SolidColor(ActiveAccent),
        modifier = Modifier
          .fillMaxWidth()
          .height(72.dp)
          .testTag("manual_text_input"),
        decorationBox = { innerTextField ->
          if (manualText.isEmpty() && !isInlineDictating) {
            Text(
              text = "Type or dictate a note, meeting summary, or tasks...",
              style = TextStyle(
                color = MonoTextMuted,
                fontSize = 14.sp
              )
            )
          }
          innerTextField()
        }
      )

      AnimatedVisibility(visible = manualText.isNotBlank()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
          horizontalArrangement = Arrangement.End
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(MonoWhite)
              .clickable { onSubmitManualText(manualText) }
              .padding(horizontal = 14.dp, vertical = 8.dp)
              .testTag("submit_manual_text_button"),
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Process",
                style = TextStyle(
                  color = Color.Black,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * STATE 2 - LISTENING VIEW (Active STT Live Text Streaming)
 */
@Composable
private fun ListeningStateView(
  durationSeconds: Int,
  liveTranscript: String,
  micAmplitude: Float,
  onStopListening: () -> Unit
) {
  val minutes = durationSeconds / 60
  val seconds = durationSeconds % 60
  val timeString = String.format("%02d:%02d", minutes, seconds)

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    // Active Status Pill with Single Vivid Accent Color
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(20.dp))
        .background(ActiveAccentSubtle)
        .border(1.dp, ActiveAccent, RoundedCornerShape(20.dp))
        .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(ActiveAccent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "LISTENING • $timeString",
          style = TextStyle(
            color = ActiveAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
        )
      }
    }

    Spacer(modifier = Modifier.height(28.dp))

    // Live Streaming Transcript Box
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(240.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(MonoSurface)
        .border(1.dp, MonoBorder, RoundedCornerShape(16.dp))
        .padding(20.dp)
    ) {
      if (liveTranscript.isBlank()) {
        Text(
          text = "Listening to your voice...",
          style = TextStyle(
            color = MonoTextMuted,
            fontSize = 18.sp,
            fontWeight = FontWeight.Light,
            lineHeight = 26.sp
          )
        )
      } else {
        Text(
          text = liveTranscript,
          style = TextStyle(
            color = MonoWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 26.sp
          ),
          modifier = Modifier.testTag("live_transcript_text")
        )
      }
    }

    Spacer(modifier = Modifier.height(36.dp))

    // Stop Listening Button
    Box(
      modifier = Modifier
        .size(90.dp)
        .clip(CircleShape)
        .background(ActiveDestructive)
        .clickable { onStopListening() }
        .testTag("capture_stop_button"),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Stop,
        contentDescription = "Stop Listening",
        tint = MonoWhite,
        modifier = Modifier.size(40.dp)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "TAP TO FINISH",
      style = TextStyle(
        color = MonoTextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp
      )
    )
  }
}

/**
 * STATE 3 - VERIFICATION VIEW (Explicit Save & Process or Discard)
 */
@Composable
private fun VerificationStateView(
  verifiedText: String,
  isInlineDictating: Boolean,
  onTextChange: (String) -> Unit,
  onSaveAndProcess: () -> Unit,
  onDiscard: () -> Unit,
  onToggleInlineDictate: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.Start
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "VERIFY TRANSCRIPT",
        style = TextStyle(
          color = MonoTextMuted,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
      )

      // Hovering Dictation Button inside Transcript verification
      HoveringDictationButton(
        isDictating = isInlineDictating,
        onToggleDictation = onToggleInlineDictate,
        label = "Append Voice",
        compact = false,
        testTag = "verification_hover_dictate"
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Editable Transcript Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(220.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(MonoSurface)
        .border(1.dp, if (isInlineDictating) ActiveAccent else MonoBorder, RoundedCornerShape(14.dp))
        .padding(16.dp)
    ) {
      BasicTextField(
        value = verifiedText,
        onValueChange = onTextChange,
        textStyle = TextStyle(
          color = MonoWhite,
          fontSize = 16.sp,
          lineHeight = 24.sp
        ),
        cursorBrush = SolidColor(ActiveAccent),
        modifier = Modifier
          .fillMaxSize()
          .testTag("verification_text_input")
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Two Explicit Primary Actions: Save & Process OR Discard
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Discard Button
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(10.dp))
          .background(MonoSurfaceElevated)
          .border(1.dp, MonoBorder, RoundedCornerShape(10.dp))
          .clickable { onDiscard() }
          .padding(vertical = 14.dp)
          .testTag("discard_dictation_button"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = null,
            tint = MonoTextSecondary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Discard",
            style = TextStyle(
              color = MonoTextSecondary,
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold
            )
          )
        }
      }

      // Save & Process Button
      Box(
        modifier = Modifier
          .weight(1.5f)
          .clip(RoundedCornerShape(10.dp))
          .background(MonoWhite)
          .clickable { onSaveAndProcess() }
          .padding(vertical = 14.dp)
          .testTag("save_and_process_button"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Save & Process",
            style = TextStyle(
              color = Color.Black,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          )
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}

/**
 * STATE 4 - PROCESSING VIEW (Clear Loading State)
 */
@Composable
private fun ProcessingStateView() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 40.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(64.dp)
        .clip(CircleShape)
        .background(MonoSurface)
        .border(1.dp, MonoBorder, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator(
        color = ActiveAccent,
        strokeWidth = 3.dp,
        modifier = Modifier.size(32.dp)
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "Processing Intelligence...",
      style = TextStyle(
        color = MonoWhite,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium
      )
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Extracting tasks, dates, and synthesizing notes into your Vault.",
      style = TextStyle(
        color = MonoTextSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center
      ),
      modifier = Modifier.padding(horizontal = 24.dp)
    )
  }
}

/**
 * STATE 5 - CONFIRMATION VIEW (Success Toast & Summary)
 */
@Composable
private fun ConfirmationStateView(
  toastMessage: RoutingToast?,
  onNavigateToVault: () -> Unit,
  onNewCapture: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MonoSurface)
      .border(1.dp, MonoBorder, RoundedCornerShape(16.dp))
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Icon(
      imageVector = Icons.Default.CheckCircle,
      contentDescription = null,
      tint = ActiveSuccess,
      modifier = Modifier.size(48.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "Saved & Routed to Vault",
      style = TextStyle(
        color = MonoWhite,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
      )
    )

    if (toastMessage != null) {
      Spacer(modifier = Modifier.height(8.dp))
      if (toastMessage.noteTitle.isNotBlank()) {
        Text(
          text = "\"${toastMessage.noteTitle}\"",
          style = TextStyle(
            color = MonoTextSecondary,
            fontSize = 14.sp
          )
        )
      }
      if (toastMessage.tasksCount > 0) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "• ${toastMessage.tasksCount} action items extracted",
          style = TextStyle(
            color = ActiveAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
        )
      }
      if (toastMessage.eventTitle != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "• Calendar event scheduled",
          style = TextStyle(
            color = ActiveSuccess,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .background(MonoSurfaceElevated)
          .border(1.dp, MonoBorder, RoundedCornerShape(8.dp))
          .clickable { onNewCapture() }
          .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "New Capture",
          style = TextStyle(
            color = MonoWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
        )
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .background(MonoWhite)
          .clickable { onNavigateToVault() }
          .padding(vertical = 12.dp)
          .testTag("confirmation_open_vault_button"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Open Vault",
          style = TextStyle(
            color = Color.Black,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
        )
      }
    }
  }
}
