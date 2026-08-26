package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.ai.StructuredMeetingNotes
import com.example.data.ai.VoiceContextType
import com.example.data.ai.VoiceFlowResult
import com.example.data.ai.VoiceTone
import com.example.data.ai.VoiceTransform
import com.example.data.local.DictionaryItemEntity
import com.example.data.local.SnippetEntity
import com.example.ui.theme.M3OnPrimaryContainer
import com.example.ui.theme.M3Primary
import com.example.ui.theme.M3PrimaryContainer

@Composable
fun FlowStudioTab(
    wisprInput: String,
    onInputChange: (String) -> Unit,
    selectedContext: VoiceContextType,
    onContextSelect: (VoiceContextType) -> Unit,
    selectedTone: VoiceTone,
    onToneSelect: (VoiceTone) -> Unit,
    selectedLanguage: String,
    onLanguageSelect: (String) -> Unit,
    wisprResult: VoiceFlowResult?,
    isProcessing: Boolean,
    onRunFlow: () -> Unit,
    activeTransform: VoiceTransform?,
    transformResult: String?,
    isTransformLoading: Boolean,
    onRunTransform: (VoiceTransform, String?) -> Unit,
    isRecordingAudio: Boolean,
    recordingSeconds: Int,
    micAmplitude: Float = 0.1f,
    micErrorMessage: String? = null,
    isExtendedContextMode: Boolean = false,
    onToggleExtendedContext: (Boolean) -> Unit = {},
    onLoadLongContextSample: () -> Unit = {},
    onToggleRecording: () -> Unit,
    onSaveAsNote: (matterCode: String, syncCalendar: Boolean) -> Unit,
    dictionaryItems: List<DictionaryItemEntity>,
    snippets: List<SnippetEntity>,
    isSpeaking: Boolean = false,
    onSpeakText: (String) -> Unit = {},
    onStopSpeaking: () -> Unit = {},
    speechRate: Float = 1.0f,
    onSetSpeechRate: (Float) -> Unit = {},
    dictationWaveformLevels: FloatArray = floatArrayOf(0.1f, 0.1f, 0.1f, 0.1f, 0.1f),
    dictationAnnouncement: String? = null,
    dictationLanguage: String = "en-US",
    onSelectDictationLanguage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var customCommandText by remember { mutableStateOf("") }
    var selectedOutputTab by remember { mutableStateOf(0) }
    var showCommandsHelp by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onToggleRecording()
        } else {
            Toast.makeText(context, "Microphone permission is needed for live speech dictation.", Toast.LENGTH_LONG).show()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val samplePrompts = listOf(
        "Self-Correction & Snippet" to "Hey um can you tell Rahul that actually wait no tell Priya that the meeting is moved from Monday to Tuesday at 4 PM and send them my email",
        "Smart Formatting" to "three things we need to finish one update the pitch deck two call the vendor three send the invoice",
        "Deep Meeting Sync" to "Discussed Q3 cloud architecture with Akash and Priya agreed to migrate PostgreSQL triggers next review Friday action items update OAuth scopes and deploy",
        "Rambling AI Prompt" to "I want the AI to help me think through pricing but make it practical and maybe compare three options with target customer and pros cons"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("flow_studio_tab")
    ) {
        // Top Banner / Status
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = M3Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "CHATOT VOICE ENGINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = M3Primary
                        )
                    }
                    Text(
                        text = "Live Dictation & Speech Cleanup",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isRecordingAudio) {
                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Text(
                                text = "REC 00:${recordingSeconds.toString().padStart(2, '0')}",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Live Audio Waveform & Dictation Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wispr_input_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Live Mic Status Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (isRecordingAudio) Icons.Default.GraphicEq else Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isRecordingAudio) Color(0xFFEF4444) else M3Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isRecordingAudio) "Listening Live (Speak Freely)..." else "Voice Dictation / Text Input",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecordingAudio) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Commands Guide toggle
                            IconButton(
                                onClick = { showCommandsHelp = !showCommandsHelp },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Voice commands guide",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (wisprInput.isNotBlank()) {
                                Text(
                                    text = "${wisprInput.split("\\s+".toRegex()).size} words",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Collapsible Voice Commands Help
                    AnimatedVisibility(visible = showCommandsHelp) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🎙️ Spoken Formatting Triggers",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = M3Primary
                                )
                                Text(
                                    text = "• Say \"period\" (.), \"comma\" (,), \"question mark\" (?), \"exclamation mark\" (!)\n• Say \"new line\" (⏎), \"bullet point\" (•), \"next paragraph\"\n• Say \"clear text\" or \"undo\" to revert\n• Dictation automatically continues over pauses seamlessly.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Multi-band Live Waveform visualization bar if recording
                    if (isRecordingAudio) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val bars = 24
                            for (i in 0 until bars) {
                                val bandIdx = (i % dictationWaveformLevels.size)
                                val bandAmp = dictationWaveformLevels[bandIdx]
                                val dynamicHeight = (8 + (bandAmp * 24 * (((i % 4) + 1) / 4f))).coerceIn(4f, 30f)
                                Box(
                                    modifier = Modifier
                                        .width(3.5.dp)
                                        .height(dynamicHeight.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (i % 3 == 0) M3Primary else if (i % 3 == 1) Color(0xFF06B6D4) else Color(0xFF8B5CF6))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Education Mode Quick Templates
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val eduPresets = listOf(
                            "📚 Lecture Note" to "Lecture Summary:\n• Subject: \n• Key Concepts: \n• Follow-Up: ",
                            "📝 Study Card" to "Term: \nDefinition: \nExample: ",
                            "⚖️ Case Brief" to "Case: \nFacts: \nIssue: \nHolding: ",
                            "✅ Action Items" to "Action Checklist:\n1. \n2. "
                        )
                        eduPresets.forEach { (label, template) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.clickable {
                                    onInputChange(if (wisprInput.isBlank()) template else "$wisprInput\n\n$template")
                                }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Spoken Punctuation Quick Shortcuts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Punctuation:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        val puncs = listOf("." to ".", "," to ",", "?" to "?", "!" to "!", "\n" to "⏎", "• " to "•", "\"" to "\"")
                        puncs.forEach { (sym, display) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.clickable {
                                    onInputChange(wisprInput + sym)
                                }
                            ) {
                                Text(
                                    text = display,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = M3Primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = wisprInput,
                        onValueChange = onInputChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isExtendedContextMode) 140.dp else 110.dp)
                            .testTag("wispr_input_field")
                            .semantics {
                                contentDescription = "Voice input field with continuous dictation support"
                            },
                        placeholder = {
                            Text(
                                text = "Tap the dictation mic or speak naturally with 'um', 'uh', self-corrections like 'actually no wait'...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = M3Primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    if (!micErrorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                            Text(micErrorMessage, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Text-To-Speech Read Aloud & Clear Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSpeaking) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSpeaking) M3Primary else Color.Transparent),
                            modifier = Modifier.clickable {
                                if (isSpeaking) {
                                    onStopSpeaking()
                                } else if (wisprInput.isNotBlank()) {
                                    onSpeakText(wisprInput)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = if (isSpeaking) "Stop Read Aloud" else "Read Aloud with TTS",
                                    tint = if (isSpeaking) M3Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isSpeaking) "Stop TTS" else "Read Aloud (TTS)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSpeaking) M3Primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (wisprInput.isNotBlank()) {
                            Text(
                                text = "Clear",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clickable { onInputChange("") }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dictation Mic Button with runtime permission trigger
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    onToggleRecording()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecordingAudio) Color(0xFFEF4444) else M3PrimaryContainer,
                                contentColor = if (isRecordingAudio) Color.White else M3OnPrimaryContainer
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .testTag("record_audio_toggle_btn")
                                .border(
                                    width = if (isRecordingAudio) (2 * pulseScale).dp else 0.dp,
                                    color = if (isRecordingAudio) Color(0xFFEF4444) else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Icon(
                                imageVector = if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Dictate / Record",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRecordingAudio) "Stop & Process" else "Dictate with Mic",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Clean & Flow Action Button
                        Button(
                            onClick = onRunFlow,
                            enabled = wisprInput.isNotBlank() && !isProcessing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = M3Primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("run_wispr_flow_btn")
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cleaning...", fontSize = 12.sp)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clean & Flow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Extended Long-Context Switch & Presets
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = M3Primary, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Extended Context Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Deep synthesis for long meetings, depositions & multi-topic voice notes", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = isExtendedContextMode,
                            onCheckedChange = { onToggleExtendedContext(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = M3Primary)
                        )
                    }

                    AnimatedVisibility(visible = isExtendedContextMode, enter = fadeIn(), exit = fadeOut()) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedButton(
                                onClick = onLoadLongContextSample,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Load 45-Min Meeting & Deposition Context Sample", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Quick Preset Sample Prompts
        item {
            Text(
                text = "TRY QUICK DICTATION SAMPLES:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(samplePrompts) { (title, prompt) ->
                    Surface(
                        modifier = Modifier
                            .clickable {
                                onInputChange(prompt)
                                if (title.contains("Meeting")) {
                                    onContextSelect(VoiceContextType.MEETING_NOTE)
                                } else if (title.contains("AI Prompt")) {
                                    onContextSelect(VoiceContextType.AI_PROMPT)
                                } else {
                                    onContextSelect(VoiceContextType.GENERAL)
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Target Writing Context Selector
        item {
            Text(
                text = "TARGET WRITING CONTEXT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = M3Primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier.horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoiceContextType.entries.forEach { contextType ->
                    FilterChip(
                        selected = selectedContext == contextType,
                        onClick = { onContextSelect(contextType) },
                        label = { Text(contextType.displayName, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = when (contextType) {
                                    VoiceContextType.EMAIL -> Icons.Default.Email
                                    VoiceContextType.TASK_LIST -> Icons.Default.FormatListBulleted
                                    VoiceContextType.MEETING_NOTE -> Icons.Default.Description
                                    VoiceContextType.LONG_CONTEXT_SYNC -> Icons.Default.Groups
                                    VoiceContextType.AI_PROMPT -> Icons.Default.Psychology
                                    VoiceContextType.JOURNAL -> Icons.Default.HistoryEdu
                                    else -> Icons.Default.Edit
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = M3PrimaryContainer,
                            selectedLabelColor = M3OnPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tone & Language Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = M3Primary, modifier = Modifier.size(14.dp))
                            Text("Tone & Style", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = M3Primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(VoiceTone.entries.toList()) { tone ->
                                Surface(
                                    modifier = Modifier.clickable { onToneSelect(tone) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedTone == tone) M3Primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selectedTone == tone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Text(
                                        text = tone.displayName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = M3Primary, modifier = Modifier.size(14.dp))
                            Text("Translation", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = M3Primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val languages = listOf("English", "Hinglish➔EN", "Spanish➔EN", "Hindi", "French", "German")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(languages) { lang ->
                                Surface(
                                    modifier = Modifier.clickable { onLanguageSelect(lang) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedLanguage == lang) M3Primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selectedLanguage == lang) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Text(
                                        text = lang,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Dictionary & Snippets Status Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = M3PrimaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Spellcheck, contentDescription = null, tint = M3Primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${dictionaryItems.size} Terms & ${snippets.size} Snippet Triggers Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = M3OnPrimaryContainer
                        )
                    }
                    Text(
                        text = "Auto-expanded",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = M3Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Gemini Processing Progress Indicator Card
        if (isProcessing) {
            item {
                GeminiProcessingCard()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Results Section
        if (wisprResult != null) {
            item {
                Text(
                    text = "OUTPUT & POLISHED ARTIFACTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = M3Primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf("Polished Output", "Structured Artifact", "AI Transforms Studio")
                    tabs.forEachIndexed { index, tabTitle ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedOutputTab = index },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedOutputTab == index) M3Primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedOutputTab == index) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(tabTitle, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            if (selectedOutputTab == 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wispr_result_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CONTEXT: ${selectedContext.displayName.uppercase()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = M3Primary
                                )

                                Text(
                                    text = "${wisprResult.latencyMs}ms • ~${wisprResult.tokenCountEstimate} tokens",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (wisprResult.selfCorrectionsFound.isNotEmpty() || wisprResult.appliedSnippets.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (wisprResult.selfCorrectionsFound.isNotEmpty()) {
                                        Surface(
                                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                                                Text("Self-Correction Resolved", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                            }
                                        }
                                    }

                                    if (wisprResult.appliedSnippets.isNotEmpty()) {
                                        Surface(
                                            color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(12.dp))
                                                Text("${wisprResult.appliedSnippets.size} Snippets Expanded", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val displayText = wisprResult.toneRewrittenText ?: wisprResult.formattedText.ifBlank { wisprResult.cleanText }
                            Text(
                                text = displayText,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (isSpeaking) {
                                            onStopSpeaking()
                                        } else {
                                            onSpeakText(displayText)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                        contentDescription = if (isSpeaking) "Stop TTS" else "Read Aloud",
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSpeaking) M3Primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isSpeaking) "Stop Voice" else "Read Aloud", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Clean Transcribed Text", displayText))
                                        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Text", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        onSaveAsNote("CTX-2024-08", true)
                                        Toast.makeText(context, "Saved to Workspace Logs & Calendar!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = M3Primary)
                                ) {
                                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Save", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Log & Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (selectedOutputTab == 1) {
                item {
                    if (wisprResult.structuredMeeting != null) {
                        MeetingNotesCard(notes = wisprResult.structuredMeeting)
                    } else if (!wisprResult.builtAiPrompt.isNullOrBlank()) {
                        AiPromptCard(prompt = wisprResult.builtAiPrompt)
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Action Items Extracted", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = M3Primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                if (wisprResult.actionItems.isNotEmpty()) {
                                    wisprResult.actionItems.forEach { action ->
                                        Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(action, fontSize = 13.sp)
                                        }
                                    }
                                } else {
                                    Text("No specific action items detected.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (selectedOutputTab == 2) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.Transform, contentDescription = null, tint = M3Primary, modifier = Modifier.size(18.dp))
                                Text("AI Transforms Presets", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text("Select any transform to instantly rewrite and restructure the text.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(12.dp))

                            val transformList = VoiceTransform.entries.toList()
                            val chunkedList = transformList.chunked(2)

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (rowTransforms in chunkedList) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (transform in rowTransforms) {
                                            OutlinedButton(
                                                onClick = { onRunTransform(transform, null) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (activeTransform == transform) M3PrimaryContainer else Color.Transparent
                                                )
                                            ) {
                                                Text(transform.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("Command Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = M3Primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customCommandText,
                                    onValueChange = { customCommandText = it },
                                    placeholder = { Text("e.g. Make this sound like an executive memo", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        if (customCommandText.isNotBlank()) {
                                            onRunTransform(VoiceTransform.POLISH, customCommandText)
                                        }
                                    },
                                    enabled = customCommandText.isNotBlank() && !isTransformLoading,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = M3Primary)
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = "Run Command", modifier = Modifier.size(16.dp))
                                }
                            }

                            if (isTransformLoading) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = M3Primary, strokeWidth = 2.dp)
                                    Text("Executing AI transform...", fontSize = 12.sp, color = M3Primary)
                                }
                            } else if (!transformResult.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "TRANSFORMED (${activeTransform?.displayName?.uppercase() ?: "CUSTOM"})",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = M3Primary
                                            )
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Transformed Text", transformResult))
                                                    Toast.makeText(context, "Copied transform!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = transformResult, fontSize = 13.sp, lineHeight = 19.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun MeetingNotesCard(notes: StructuredMeetingNotes) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(notes.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = M3Primary)
                IconButton(
                    onClick = {
                        val fullNotes = buildString {
                            appendLine("# ${notes.title}")
                            appendLine("\n## Executive Summary\n${notes.executiveSummary}")
                            if (notes.timelineHighlights.isNotEmpty()) {
                                appendLine("\n## Timeline & Highlights\n" + notes.timelineHighlights.joinToString("\n") { "• $it" })
                            }
                            appendLine("\n## Key Decisions\n" + notes.keyDecisions.joinToString("\n") { "• $it" })
                            appendLine("\n## Action Items\n" + notes.actionItems.joinToString("\n") { "• $it" })
                            if (notes.risksAndBlockers.isNotEmpty()) {
                                appendLine("\n## Risks & Blockers\n" + notes.risksAndBlockers.joinToString("\n") { "• $it" })
                            }
                            appendLine("\n## Follow-Up Email\n${notes.followUpEmailDraft}")
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Meeting Notes", fullNotes))
                        Toast.makeText(context, "Copied full meeting notes!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Executive Summary", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(notes.executiveSummary, fontSize = 13.sp, lineHeight = 18.sp)

            if (notes.timelineHighlights.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Timeline & Discussion Milestones", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                notes.timelineHighlights.forEach {
                    Text("• $it", fontSize = 13.sp)
                }
            }

            if (notes.keyDecisions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Key Decisions", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                notes.keyDecisions.forEach {
                    Text("• $it", fontSize = 13.sp)
                }
            }

            if (notes.actionItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Action Items & Deliverables", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                notes.actionItems.forEach {
                    Text("• $it", fontSize = 13.sp)
                }
            }

            if (notes.risksAndBlockers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Identified Risks & Blockers", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                notes.risksAndBlockers.forEach {
                    Text("• $it", fontSize = 13.sp)
                }
            }

            if (notes.followUpEmailDraft.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Draft Follow-Up Email", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = notes.followUpEmailDraft,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Export & Share Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val text = com.example.util.TextExportHelper.formatMeetingMinutesText(
                            meetingNotes = notes,
                            voiceResult = null,
                            toneName = "Structured"
                        )
                        com.example.util.TextExportHelper.exportAndShareTextFile(
                            context = context,
                            content = text,
                            fileNamePrefix = "meeting_minutes"
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_meeting_minutes_txt_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export .txt File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val text = com.example.util.TextExportHelper.formatMeetingMinutesText(
                            meetingNotes = notes,
                            voiceResult = null,
                            toneName = "Structured"
                        )
                        com.example.util.TextExportHelper.sharePlainText(
                            context = context,
                            content = text,
                            title = "Share Meeting Summary"
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_meeting_minutes_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = M3Primary)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Minutes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AiPromptCard(prompt: String) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Engineered AI Prompt", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = M3Primary)
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Engineered Prompt", prompt))
                        Toast.makeText(context, "Copied prompt!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = prompt,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun GeminiProcessingCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_progress_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gemini_alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gemini_processing_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = M3PrimaryContainer.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.5.dp, M3Primary.copy(alpha = pulseAlpha))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(52.dp),
                    color = M3Primary,
                    strokeWidth = 3.5.dp
                )
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = M3Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Spatial AI Synthesizing Notes & Actions",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Processing audio & speech transcript with Gemini 3.5 Flash, resolving fillers, and generating structured summaries and action items...",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = M3Primary
            )
        }
    }
}
