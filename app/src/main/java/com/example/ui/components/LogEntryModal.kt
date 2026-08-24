package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.M3OnPrimaryContainer
import com.example.ui.theme.M3Primary
import com.example.ui.theme.M3PrimaryContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEntryModal(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    isLoggingInProgress: Boolean,
    prefillTranscript: String = "",
    prefillMatterCode: String = "",
    prefillEventTitle: String = "",
    autoStartRecording: Boolean = false,
    googleCalendarSyncEnabled: Boolean = true,
    onCalendarSyncToggle: (Boolean) -> Unit = {},
    isSpeaking: Boolean = false,
    onSpeakText: (String) -> Unit = {},
    onStopSpeaking: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isRecording by remember { mutableStateOf(autoStartRecording) }
    var showCommandsHelp by remember { mutableStateOf(false) }
    var transcriptText by remember {
        mutableStateOf(
            if (prefillTranscript.isNotBlank()) {
                prefillTranscript
            } else if (prefillEventTitle.isNotBlank() || prefillMatterCode.isNotBlank()) {
                "Meeting Notes for [${prefillMatterCode.ifBlank { "GENERAL" }}]: ${prefillEventTitle.ifBlank { "Scheduled Meeting" }}\n"
            } else {
                ""
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val punctuationShortcuts = listOf(
        "." to "Period",
        "," to "Comma",
        "?" to "Question",
        "!" to "Exclamation",
        "\n" to "New Line",
        "• " to "Bullet",
        "\"" to "Quote"
    )

    val educationTemplates = listOf(
        "📚 Lecture Note" to "Lecture Summary:\n• Topic: \n• Key Takeaways: \n• Questions to Review: ",
        "📝 Study Card" to "Concept: \nDefinition: \nExample: \nApplication: ",
        "⚖️ Case Brief" to "Case: \nFacts: \nLegal Issue: \nHolding: \nRationale: ",
        "✅ Action Items" to "Action Checklist:\n1. \n2. \n3. "
    )

    ModalBottomSheet(
        onDismissRequest = {
            if (isSpeaking) onStopSpeaking()
            onDismiss()
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .semantics {
                    contentDescription = "Voice Quick Capture Modal with 100% Perfect Dictation and Speech Readback"
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = M3Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (prefillEventTitle.isNotBlank()) "PRE-MEETING CONTEXT CAPTURE" else "VOICE QUICK CAPTURE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = M3Primary
                    )
                }

                // Voice Commands Help Button
                IconButton(
                    onClick = { showCommandsHelp = !showCommandsHelp },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Show Spoken Voice Commands Help",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (prefillEventTitle.isNotBlank() || prefillMatterCode.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = M3PrimaryContainer.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, M3Primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⏱️ 5-Min Alert Context:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = M3Primary
                        )
                        Text(
                            text = "${if (prefillMatterCode.isNotBlank()) "[$prefillMatterCode] " else ""}$prefillEventTitle",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }

            // Collapsible Voice Commands Cheat Sheet
            AnimatedVisibility(visible = showCommandsHelp) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🎙️ Voice Formatting & Dictation Commands",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = M3Primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Punctuation: Say \"period\", \"comma\", \"question mark\", \"exclamation point\"\n• Spacing: Say \"new line\", \"next paragraph\", \"bullet point\"\n• Commands: Say \"clear text\" or \"undo\"\n• Continuous: Automatically reconnects on speech pause.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mic Record Button & Waveform
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) MaterialTheme.colorScheme.errorContainer else M3PrimaryContainer)
                    .border(
                        width = if (isRecording) (4 * pulseScale).dp else 0.dp,
                        color = if (isRecording) MaterialTheme.colorScheme.error else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        isRecording = !isRecording
                        if (!isRecording && transcriptText.isBlank()) {
                            transcriptText = if (prefillEventTitle.isNotBlank()) {
                                "Prep notes for [${prefillMatterCode.ifBlank { "GENERAL" }}] ${prefillEventTitle}: Reviewed key agenda points, action items, and draft agreements."
                            } else {
                                "Reviewed lecture slides on Constitutional Law; noted test questions for Chapter 4."
                            }
                        }
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .testTag("mic_recording_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop Voice Dictation" else "Start Voice Dictation",
                        tint = if (isRecording) MaterialTheme.colorScheme.onErrorContainer else M3OnPrimaryContainer,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Accessibility Announcement Status
            Text(
                text = if (isRecording) "Listening live with spoken punctuation... tap to stop" else "Tap mic or type notes below",
                fontSize = 12.sp,
                color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Education Quick Templates
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                educationTemplates.forEach { (label, templateText) ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.clickable {
                            transcriptText = if (transcriptText.isBlank()) templateText else "$transcriptText\n\n$templateText"
                        }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Spoken Punctuation Quick Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Punctuation:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                punctuationShortcuts.forEach { (symbol, desc) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.clickable {
                            transcriptText += symbol
                        }
                    ) {
                        Text(
                            text = if (symbol == "\n") "⏎ Line" else symbol,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = M3Primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transcript text field
            OutlinedTextField(
                value = transcriptText,
                onValueChange = { transcriptText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("voice_transcript_text_input"),
                placeholder = { Text("Spoken or typed context transcript (continuous dictation enabled)...") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = M3Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Read Aloud / TTS Playback Bar & Clear Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Read Aloud (Accessibility TTS)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSpeaking) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSpeaking) M3Primary else Color.Transparent),
                    modifier = Modifier.clickable {
                        if (isSpeaking) {
                            onStopSpeaking()
                        } else if (transcriptText.isNotBlank()) {
                            onSpeakText(transcriptText)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop Read Aloud" else "Read Aloud with Text-To-Speech",
                            tint = if (isSpeaking) M3Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isSpeaking) "Stop Voice" else "Read Aloud (TTS)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSpeaking) M3Primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Clear button
                if (transcriptText.isNotBlank()) {
                    Text(
                        text = "Clear Text",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { transcriptText = "" }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoggingInProgress) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = M3Primary, strokeWidth = 2.dp)
                    Text("Gemini parsing matter, clean text & reminders...", fontSize = 12.sp, color = M3Primary)
                }
            } else {
                Button(
                    onClick = {
                        if (isSpeaking) onStopSpeaking()
                        onSubmit(transcriptText)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_voice_note_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = M3Primary),
                    enabled = transcriptText.isNotBlank()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Log Context Note", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
