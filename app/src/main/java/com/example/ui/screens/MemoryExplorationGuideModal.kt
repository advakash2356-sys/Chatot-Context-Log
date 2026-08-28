package com.example.ui.screens

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiMultimodalLiveClient
import com.example.data.ai.GuideSender
import com.example.data.ai.LiveConnectionStatus
import com.example.data.ai.LiveMetrics
import com.example.data.ai.MemoryExplorationGuideEngine
import com.example.data.ai.MemoryGuideMessage
import com.example.data.ai.ToolCallInfo
import com.example.data.local.EpisodicMemoryEntity
import com.example.ui.theme.ActiveAccent
import com.example.ui.theme.ActiveAccentSubtle
import com.example.ui.theme.MonoBackground
import com.example.ui.theme.MonoBorder
import com.example.ui.theme.MonoBorderSubtle
import com.example.ui.theme.MonoSurface
import com.example.ui.theme.MonoSurfaceElevated
import com.example.ui.theme.MonoTextMuted
import com.example.ui.theme.MonoTextSecondary
import com.example.ui.theme.MonoWhite

enum class ExplorationMode {
    MULTIMODAL_LIVE_WS,
    SOCRATIC_GUIDE_REST
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryExplorationGuideModal(
    memory: EpisodicMemoryEntity?,
    messages: List<MemoryGuideMessage>,
    isThinking: Boolean,
    isSpeaking: Boolean,
    isTtsEnabled: Boolean,
    inputText: String,
    isDictating: Boolean,
    // Phase 4 Live Multimodal parameters
    isLiveMode: Boolean = false,
    liveStatus: LiveConnectionStatus = LiveConnectionStatus.DISCONNECTED,
    liveMetrics: LiveMetrics = LiveMetrics(),
    liveMicAmplitude: Float = 0f,
    liveSpeakerAmplitude: Float = 0f,
    selectedVoice: String = "Aoede",
    onToggleLiveMode: () -> Unit = {},
    onSelectVoice: (String) -> Unit = {},
    onTriggerBargeIn: () -> Unit = {},
    onInputChange: (String) -> Unit,
    onSendMessage: (String, String?) -> Unit,
    onTriggerSensoryAnchor: (String) -> Unit,
    onToggleTts: () -> Unit,
    onStopSpeaking: () -> Unit,
    onToggleDictation: () -> Unit,
    onDismiss: () -> Unit,
    onSaveExploration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var showToolSchemaDialog by remember { mutableStateOf(false) }
    var activeMode by remember { mutableStateOf(if (isLiveMode) ExplorationMode.MULTIMODAL_LIVE_WS else ExplorationMode.SOCRATIC_GUIDE_REST) }

    LaunchedEffect(isLiveMode) {
        activeMode = if (isLiveMode) ExplorationMode.MULTIMODAL_LIVE_WS else ExplorationMode.SOCRATIC_GUIDE_REST
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable { /* prevent click-through */ },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxSize(0.94f)
                .clip(RoundedCornerShape(18.dp))
                .background(MonoSurface)
                .border(1.dp, ActiveAccent.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            // HEADER BAR: VOICE GUIDE IDENTITY, TOOL SCHEMA VIEWER & CONTROLS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing Voice Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (activeMode == ExplorationMode.MULTIMODAL_LIVE_WS) Color(0xFF00E5FF) else ActiveAccent,
                                        ActiveAccent.copy(alpha = 0.3f)
                                    )
                                )
                            )
                            .border(1.dp, if (activeMode == ExplorationMode.MULTIMODAL_LIVE_WS) Color(0xFF00E5FF) else ActiveAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSpeaking || liveSpeakerAmplitude > 0.05f) Icons.Default.GraphicEq else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MonoBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (activeMode == ExplorationMode.MULTIMODAL_LIVE_WS) "GEMINI MULTIMODAL LIVE" else "MEMORY EXPLORATION GUIDE",
                                style = TextStyle(
                                    color = MonoWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Status Dot
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            liveStatus == LiveConnectionStatus.STREAMING_AUDIO || isSpeaking -> Color(0xFF66BB6A)
                                            liveStatus == LiveConnectionStatus.CONNECTED_READY -> Color(0xFF00E5FF)
                                            liveStatus == LiveConnectionStatus.INTERRUPTED -> Color(0xFFFFB300)
                                            else -> ActiveAccent
                                        }
                                    )
                            )
                        }

                        Text(
                            text = if (memory != null) "Anchored: ${memory.timeframeReferenced.ifBlank { "Personal Recall" }}" else "Bidirectional 16k/24k Live Voice + Tool Calling",
                            style = TextStyle(
                                color = MonoTextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Controls: Mode switch, Tool Schema Viewer, TTS Mute Toggle, Stop Voice, Close
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Function Calling Schema Inspector Button
                    IconButton(
                        onClick = { showToolSchemaDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MonoSurfaceElevated)
                            .border(1.dp, MonoBorderSubtle, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "View Function Calling Schemas",
                            tint = ActiveAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onToggleTts,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isTtsEnabled) ActiveAccentSubtle else MonoSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = if (isTtsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = if (isTtsEnabled) "Voice Speech Enabled" else "Voice Speech Muted",
                            tint = if (isTtsEnabled) ActiveAccent else MonoTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (isSpeaking || isLiveMode) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                onStopSpeaking()
                                onTriggerBargeIn()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE57373).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFFE57373), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Interrupt / Barge-In",
                                tint = Color(0xFFE57373),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Exploration Guide",
                            tint = MonoTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // MODE SELECTOR: MULTIMODAL LIVE (WEBSOCKET) VS TURN-BASED REST
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MonoBackground)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeMode == ExplorationMode.MULTIMODAL_LIVE_WS) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.Transparent)
                        .border(1.dp, if (activeMode == ExplorationMode.MULTIMODAL_LIVE_WS) Color(0xFF00E5FF) else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable {
                            activeMode = ExplorationMode.MULTIMODAL_LIVE_WS
                            if (!isLiveMode) onToggleLiveMode()
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = if (activeMode == ExplorationMode.MULTIMODAL_LIVE_WS) Color(0xFF00E5FF) else MonoTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MULTIMODAL LIVE (WebSocket)",
                            style = TextStyle(
                                color = if (activeMode == ExplorationMode.MULTIMODAL_LIVE_WS) Color(0xFF00E5FF) else MonoTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeMode == ExplorationMode.SOCRATIC_GUIDE_REST) ActiveAccentSubtle else Color.Transparent)
                        .border(1.dp, if (activeMode == ExplorationMode.SOCRATIC_GUIDE_REST) ActiveAccent else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable {
                            activeMode = ExplorationMode.SOCRATIC_GUIDE_REST
                            if (isLiveMode) onToggleLiveMode()
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = if (activeMode == ExplorationMode.SOCRATIC_GUIDE_REST) ActiveAccent else MonoTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SOCRATIC GUIDE (Turn-Based)",
                            style = TextStyle(
                                color = if (activeMode == ExplorationMode.SOCRATIC_GUIDE_REST) ActiveAccent else MonoTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // PHASE 4: MULTIMODAL LIVE STREAMING HUD & WAVEFORM
            if (activeMode == ExplorationMode.MULTIMODAL_LIVE_WS) {
                LiveMultimodalStreamHud(
                    liveStatus = liveStatus,
                    liveMetrics = liveMetrics,
                    micAmplitude = liveMicAmplitude,
                    speakerAmplitude = liveSpeakerAmplitude,
                    selectedVoice = selectedVoice,
                    onSelectVoice = onSelectVoice,
                    onTriggerBargeIn = onTriggerBargeIn
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // VERIFICATION TEST CASES SHORTCUT ROW (Test Case A, B, C)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE GROUNDING TESTS:",
                    style = TextStyle(
                        color = ActiveAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        QuickTestChip(
                            label = "Test A: Snow Ride",
                            onClick = { onSendMessage("Do you remember anything about that ride I took up north when it started snowing?", null) }
                        )
                    }
                    item {
                        QuickTestChip(
                            label = "Test B: Mother Moving",
                            onClick = { onSendMessage("I was thinking about the conversations I used to have with my mother before moving.", null) }
                        )
                    }
                    item {
                        QuickTestChip(
                            label = "Test C: Chai Stall Grounding",
                            onClick = { onSendMessage("Tell me about the roadside chai stall on the mountain pass.", null) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // SOCRATIC SENSORY ANCHOR PROMPT SHORTCUTS
            Text(
                text = "SOCRATIC SENSORY ANCHORS",
                style = TextStyle(
                    color = MonoTextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    SensoryAnchorChip(
                        label = "Setting & Weather",
                        onClick = { onTriggerSensoryAnchor("What was the weather like, and what did the physical room or landscape look like?") }
                    )
                }
                item {
                    SensoryAnchorChip(
                        label = "Sounds & Music",
                        onClick = { onTriggerSensoryAnchor("What sounds, songs, or background atmosphere do you recall hearing?") }
                    )
                }
                item {
                    SensoryAnchorChip(
                        label = "People & Expressions",
                        onClick = { onTriggerSensoryAnchor("Who else was there with you, and how did they react or look in that moment?") }
                    )
                }
                item {
                    SensoryAnchorChip(
                        label = "Emotional Shift",
                        onClick = { onTriggerSensoryAnchor("How did you feel right before this happened, and what shifted inside you afterwards?") }
                    )
                }
                item {
                    SensoryAnchorChip(
                        label = "Era & Life Stage",
                        onClick = { onTriggerSensoryAnchor("Where were you living at the time, and what phase of life were you going through?") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MonoBorderSubtle, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // CONVERSATION STREAM WITH TOOL CALL BADGES
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    GuideMessageBubble(message = msg)
                }

                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MonoSurfaceElevated)
                                .border(1.dp, MonoBorderSubtle, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = ActiveAccent,
                                strokeWidth = 1.8.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Guide is consulting archive & reflecting...",
                                style = TextStyle(
                                    color = ActiveAccent,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // INPUT & SPOKEN ACTION BAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MonoBackground)
                    .border(
                        1.dp,
                        if (isDictating || liveMicAmplitude > 0.1f) ActiveAccent else MonoBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        textStyle = TextStyle(
                            color = MonoWhite,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        cursorBrush = SolidColor(ActiveAccent),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                            .testTag("guide_input_text_field"),
                        decorationBox = { innerTextField ->
                            if (inputText.isBlank() && !isDictating) {
                                Text(
                                    text = if (activeMode == ExplorationMode.MULTIMODAL_LIVE_WS) "Mic streaming live to Gemini (Speak or type)..." else "Speak or type your reflection...",
                                    style = TextStyle(color = MonoTextMuted, fontSize = 12.5.sp)
                                )
                            }
                            innerTextField()
                        }
                    )

                    // Dictation Mic Button
                    IconButton(
                        onClick = onToggleDictation,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isDictating) ActiveAccentSubtle else MonoSurfaceElevated)
                            .testTag("guide_dictate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Dictation",
                            tint = if (isDictating) ActiveAccent else MonoTextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send Message Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText, null)
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) ActiveAccent else MonoSurfaceElevated)
                            .testTag("guide_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Reflection",
                            tint = if (inputText.isNotBlank()) MonoBackground else MonoTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // FOOTER ACTION: SAVE AS ENRICHED EPISODIC MEMORY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reflections and resolved gaps write directly to memory node.",
                    style = TextStyle(color = MonoTextMuted, fontSize = 10.5.sp)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ActiveAccentSubtle)
                        .border(1.dp, ActiveAccent, RoundedCornerShape(8.dp))
                        .clickable { onSaveExploration() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = ActiveAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "COMMIT INSIGHTS",
                            style = TextStyle(
                                color = ActiveAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }

        // TOOL SCHEMA INSPECTOR DIALOG
        if (showToolSchemaDialog) {
            ToolSchemaInspectorDialog(onDismiss = { showToolSchemaDialog = false })
        }
    }
}

/**
 * LiveMultimodalStreamHud
 * Displays the real-time bidirectional status, 16k mic / 24k speaker waveforms, latency tracker, and voice picker.
 */
@Composable
fun LiveMultimodalStreamHud(
    liveStatus: LiveConnectionStatus,
    liveMetrics: LiveMetrics,
    micAmplitude: Float,
    speakerAmplitude: Float,
    selectedVoice: String,
    onSelectVoice: (String) -> Unit,
    onTriggerBargeIn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MonoBackground)
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // HUD Top Row: Model & Latency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "gemini-2.0-flash-exp (Stateful WebSocket)",
                            style = TextStyle(
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }

                // Latency Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = Color(0xFF66BB6A),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (liveMetrics.timeToFirstChunkMs > 0) "${liveMetrics.timeToFirstChunkMs}ms TTFB" else "~320ms TTFB",
                        style = TextStyle(
                            color = Color(0xFF66BB6A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            // Dual Waveform Visualizer: Mic (16kHz) & Speaker (24kHz)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MonoSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Mic 16kHz visualizer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "16k MIC IN",
                        style = TextStyle(
                            color = Color(0xFF81C784),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    LivePcmBarWaveform(
                        amplitude = micAmplitude,
                        color = Color(0xFF81C784),
                        barCount = 7
                    )
                }

                // Center Barge-in notice
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE57373).copy(alpha = 0.15f))
                        .clickable { onTriggerBargeIn() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "⚡ BARGE-IN ACTIVE",
                        style = TextStyle(
                            color = Color(0xFFE57373),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Speaker 24kHz visualizer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LivePcmBarWaveform(
                        amplitude = speakerAmplitude,
                        color = Color(0xFFBA68C8),
                        barCount = 7
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "24k AUDIO OUT",
                        style = TextStyle(
                            color = Color(0xFFBA68C8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            // Voice Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VOICE:",
                    style = TextStyle(
                        color = MonoTextMuted,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(GeminiMultimodalLiveClient.AVAILABLE_VOICES) { voice ->
                        val isSel = voice.equals(selectedVoice, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSel) ActiveAccent else MonoSurfaceElevated)
                                .clickable { onSelectVoice(voice) }
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = voice,
                                style = TextStyle(
                                    color = if (isSel) MonoBackground else MonoWhite,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LivePcmBarWaveform(
    amplitude: Float,
    color: Color,
    barCount: Int = 6
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pcm_wave")
    val animVal by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_anim"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(20.dp)
    ) {
        for (i in 0 until barCount) {
            val scaleFactor = ((i + 1).toFloat() / barCount.toFloat())
            val barHeight = ((amplitude * 18.dp.value * scaleFactor) + (if (amplitude > 0.05f) animVal * 6f else 3f)).coerceIn(3f, 20f)

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Quick Test Chip for Phase 3/4 grounding tests.
 */
@Composable
fun QuickTestChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MonoSurfaceElevated)
            .border(1.dp, MonoBorderSubtle, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = MonoWhite,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * Socratic Sensory Anchor prompt shortcut chip.
 */
@Composable
fun SensoryAnchorChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ActiveAccentSubtle)
            .border(1.dp, ActiveAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ActiveAccent,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * Message bubble with tool execution badge and sensory cue indicators.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuideMessageBubble(message: MemoryGuideMessage) {
    val isUser = message.sender == GuideSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isUser) "YOU" else "VOICE GUIDE",
                style = TextStyle(
                    color = if (isUser) MonoTextMuted else ActiveAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            )
        }

        // Message Body
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    )
                )
                .background(if (isUser) MonoSurfaceElevated else MonoSurface)
                .border(
                    1.dp,
                    if (isUser) MonoBorder else ActiveAccent.copy(alpha = 0.5f),
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Tool Call Badges
                if (message.executedTools.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        message.executedTools.forEach { tool ->
                            LiveToolExecutionBadge(tool = tool)
                        }
                    }
                }

                Text(
                    text = message.text,
                    style = TextStyle(
                        color = MonoWhite,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )
                )
            }
        }
    }
}

/**
 * Tool Execution Badge showing tool name, arguments, and result summary.
 */
@Composable
fun LiveToolExecutionBadge(tool: ToolCallInfo) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (tool.toolName == "retrieve_memories") Icons.Default.Search else Icons.Default.Construction,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "TOOL: ${tool.toolName}",
                style = TextStyle(
                    color = Color(0xFF38BDF8),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "(${tool.resultSummary})",
                style = TextStyle(
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp
                )
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Args: ${tool.argsJson}",
                style = TextStyle(
                    color = MonoTextMuted,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}

/**
 * Tool Schema Inspector Dialog displaying schemas and System Instructions.
 */
@Composable
fun ToolSchemaInspectorDialog(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable { /* prevent dismissal */ },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(14.dp))
                .background(MonoSurface)
                .border(1.dp, ActiveAccent, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI STUDIO FUNCTION CALLING SCHEMAS",
                    style = TextStyle(
                        color = MonoWhite,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MonoWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MonoBorderSubtle, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "TOOL 1: retrieve_memories",
                        style = TextStyle(
                            color = ActiveAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MonoBackground)
                            .border(1.dp, MonoBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = MemoryExplorationGuideEngine.RETRIEVE_MEMORIES_SCHEMA_JSON,
                            style = TextStyle(
                                color = MonoWhite,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        )
                    }
                }

                item {
                    Text(
                        text = "TOOL 2: update_memory_node",
                        style = TextStyle(
                            color = ActiveAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MonoBackground)
                            .border(1.dp, MonoBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = MemoryExplorationGuideEngine.UPDATE_MEMORY_NODE_SCHEMA_JSON,
                            style = TextStyle(
                                color = MonoWhite,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        )
                    }
                }

                item {
                    Text(
                        text = "GEMINI MULTIMODAL LIVE WS TRANSPORT (PHASE 4)",
                        style = TextStyle(
                            color = Color(0xFF00E5FF),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MonoBackground)
                            .border(1.dp, MonoBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = """- Protocol: Stateful Bidirectional WebSocket
- Endpoint: wss://generativelanguage.googleapis.com/ws/...BidiGenerateContent
- Model: models/gemini-2.0-flash-exp
- Input Audio: 16,000 Hz, 16-bit PCM Mono Little-Endian (512-1024 bytes)
- Output Audio: 24,000 Hz, 16-bit PCM Mono Little-Endian (Streaming AudioTrack)
- Latency Budget: ~300ms-500ms TTFB
- Interruption: Native serverContent.interrupted -> flush AudioTrack""",
                            style = TextStyle(
                                color = MonoTextSecondary,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
