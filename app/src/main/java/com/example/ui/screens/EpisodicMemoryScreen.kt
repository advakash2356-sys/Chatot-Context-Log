package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.LiveConnectionStatus
import com.example.data.ai.LiveMetrics
import com.example.data.ai.MemoryGuideMessage
import com.example.data.local.EpisodicMemoryEntity
import com.example.ui.theme.ActiveAccent
import com.example.ui.theme.ActiveAccentSubtle
import com.example.ui.theme.ActiveDestructive
import com.example.ui.theme.MonoBackground
import com.example.ui.theme.MonoBorder
import com.example.ui.theme.MonoBorderSubtle
import com.example.ui.theme.MonoSurface
import com.example.ui.theme.MonoSurfaceElevated
import com.example.ui.theme.MonoTextMuted
import com.example.ui.theme.MonoTextSecondary
import com.example.ui.theme.MonoWhite
import com.example.ui.viewmodel.MainAppView
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EpisodicMemoryScreen(
    memories: List<EpisodicMemoryEntity>,
    vaultItemCount: Int,
    isIngesting: Boolean,
    ingestStage: String,
    searchQuery: String,
    selectedLifeStageFilter: String?,
    selectedValenceFilter: String?,
    selectedMemory: EpisodicMemoryEntity?,
    probingGapQuestion: String?,
    probeAnswerText: String,
    isProbingLoading: Boolean,
    inputText: String,
    imageDesc: String,
    inlineDictationTarget: String?,
    // Interactive Memory Exploration Guide parameters
    isGuideSessionActive: Boolean = false,
    guideMessages: List<MemoryGuideMessage> = emptyList(),
    isGuideThinking: Boolean = false,
    isGuideSpeaking: Boolean = false,
    isGuideTtsEnabled: Boolean = true,
    guideInputText: String = "",
    activeExploringMemory: EpisodicMemoryEntity? = null,
    // Phase 4 Live Multimodal parameters
    isLiveMultimodalActive: Boolean = false,
    selectedLiveVoice: String = "Aoede",
    liveConnectionStatus: LiveConnectionStatus = LiveConnectionStatus.DISCONNECTED,
    liveMetrics: LiveMetrics = LiveMetrics(),
    liveMicAmplitude: Float = 0f,
    liveSpeakerAmplitude: Float = 0f,
    onToggleLiveMode: () -> Unit = {},
    onSelectLiveVoice: (String) -> Unit = {},
    onTriggerLiveBargeIn: () -> Unit = {},
    onStartGuideSession: (EpisodicMemoryEntity?) -> Unit = {},
    onSendGuideMessage: (String, String?) -> Unit = { _, _ -> },
    onTriggerSensoryAnchor: (String) -> Unit = {},
    onToggleGuideTts: () -> Unit = {},
    onStopGuideSpeaking: () -> Unit = {},
    onGuideInputChange: (String) -> Unit = {},
    onEndGuideSession: () -> Unit = {},
    onSaveGuideExploration: () -> Unit = {},
    onNavigateToCapture: () -> Unit,
    onNavigateToVault: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectLifeStageFilter: (String?) -> Unit,
    onSelectValenceFilter: (String?) -> Unit,
    onInputTextChange: (String) -> Unit,
    onImageDescChange: (String) -> Unit,
    onIngestMemory: (String?, String?) -> Unit,
    onSelectMemory: (EpisodicMemoryEntity?) -> Unit,
    onStartProbingGap: (String?) -> Unit,
    onProbeAnswerChange: (String) -> Unit,
    onSubmitProbeAnswer: (String, String, String) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onStartInlineDictation: (String, String, (String) -> Unit) -> Unit,
    onStopInlineDictation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isInputExpanded by remember { mutableStateOf(false) }
    var jsonInspectMemory by remember { mutableStateOf<EpisodicMemoryEntity?>(null) }

    // Filter memories based on search query, life stage, and emotional valence
    val filteredMemories = memories.filter { mem ->
        val matchesSearch = searchQuery.isBlank() ||
            mem.narrativeSummary.contains(searchQuery, ignoreCase = true) ||
            mem.rawInputText.contains(searchQuery, ignoreCase = true) ||
            mem.timeframeReferenced.contains(searchQuery, ignoreCase = true) ||
            mem.peopleJson.contains(searchQuery, ignoreCase = true) ||
            mem.locationsJson.contains(searchQuery, ignoreCase = true) ||
            mem.searchKeywordsJson.contains(searchQuery, ignoreCase = true)

        val matchesStage = selectedLifeStageFilter == null ||
            mem.relativeLifeStage.equals(selectedLifeStageFilter, ignoreCase = true)

        val matchesValence = selectedValenceFilter == null ||
            mem.emotionalValence.equals(selectedValenceFilter, ignoreCase = true)

        matchesSearch && matchesStage && matchesValence
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MonoBackground)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top Navigation Switcher [ CAPTURE | VAULT | EPISODIC ]
            MinimalistViewSwitcher(
                activeView = MainAppView.EPISODIC,
                vaultCount = vaultItemCount,
                episodicCount = memories.size,
                onSelectCapture = onNavigateToCapture,
                onSelectVault = onNavigateToVault,
                onSelectEpisodic = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION 0: VOICE MEMORY EXPLORATION GUIDE LAUNCHER BANNER
                item {
                    VoiceMemoryGuideLauncherBanner(
                        isSpeaking = isGuideSpeaking,
                        onLaunch = { onStartGuideSession(null) }
                    )
                }

                // SECTION 1: EPISODIC INGESTION CONSOLE & LIVE CYCLE
                item {
                    EpisodicIngestionCard(
                        inputText = inputText,
                        imageDesc = imageDesc,
                        isExpanded = isInputExpanded || inputText.isNotBlank() || isIngesting,
                        isIngesting = isIngesting,
                        ingestStage = ingestStage,
                        isInlineDictatingText = inlineDictationTarget == "episodic_input_text",
                        isInlineDictatingImage = inlineDictationTarget == "episodic_image_desc",
                        onExpandToggle = { isInputExpanded = !isInputExpanded },
                        onInputTextChange = onInputTextChange,
                        onImageDescChange = onImageDescChange,
                        onIngest = { onIngestMemory(null, null) },
                        onPresetSelect = { presetText, presetImage ->
                            onInputTextChange(presetText)
                            onImageDescChange(presetImage)
                            isInputExpanded = true
                        },
                        onToggleDictateText = {
                            if (inlineDictationTarget == "episodic_input_text") {
                                onStopInlineDictation()
                            } else {
                                onStartInlineDictation("episodic_input_text", inputText, onInputTextChange)
                            }
                        },
                        onToggleDictateImage = {
                            if (inlineDictationTarget == "episodic_image_desc") {
                                onStopInlineDictation()
                            } else {
                                onStartInlineDictation("episodic_image_desc", imageDesc, onImageDescChange)
                            }
                        }
                    )
                }

                // SECTION 2: SEARCH & EPISODIC FILTER STRIP
                item {
                    EpisodicSearchAndFilterBar(
                        searchQuery = searchQuery,
                        selectedLifeStage = selectedLifeStageFilter,
                        selectedValence = selectedValenceFilter,
                        totalCount = memories.size,
                        filteredCount = filteredMemories.size,
                        isInlineDictating = inlineDictationTarget == "episodic_search",
                        onSearchChange = onSearchQueryChange,
                        onSelectLifeStage = onSelectLifeStageFilter,
                        onSelectValence = onSelectValenceFilter,
                        onToggleDictateSearch = {
                            if (inlineDictationTarget == "episodic_search") {
                                onStopInlineDictation()
                            } else {
                                onStartInlineDictation("episodic_search", searchQuery, onSearchQueryChange)
                            }
                        }
                    )
                }

                // SECTION 3: MEMORY NODES TIMELINE LIST
                if (filteredMemories.isEmpty()) {
                    item {
                        EmptyEpisodicVaultView(
                            hasMemories = memories.isNotEmpty(),
                            onSeedSample = {
                                onIngestMemory(
                                    "Late autumn 2021 cabin trip with Mom and Alex in Lake Tahoe. We arrived as the first snowfall hit, lighting the old wood-burning stove while rain and sleet tapped rhythmically on the tin roof. The room smelled of pine needles and hot cocoa as we looked at vintage photo albums.",
                                    "Vintage photo of a red wooden cabin in snowfall with smoke curling from the chimney at dusk."
                                )
                            }
                        )
                    }
                } else {
                    items(filteredMemories, key = { it.id }) { memory ->
                        EpisodicMemoryNodeCard(
                            memory = memory,
                            isSelected = selectedMemory?.id == memory.id,
                            onCardClick = {
                                if (selectedMemory?.id == memory.id) {
                                    onSelectMemory(null)
                                } else {
                                    onSelectMemory(memory)
                                }
                            },
                            onInspectJson = { jsonInspectMemory = memory },
                            onDelete = { onDeleteMemory(memory.id) },
                            onStartProbingGap = onStartProbingGap,
                            onExploreWithGuide = { onStartGuideSession(memory) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        // INTERACTIVE VOICE-FIRST MEMORY EXPLORATION GUIDE MODAL
        if (isGuideSessionActive) {
            MemoryExplorationGuideModal(
                memory = activeExploringMemory,
                messages = guideMessages,
                isThinking = isGuideThinking,
                isSpeaking = isGuideSpeaking,
                isTtsEnabled = isGuideTtsEnabled,
                inputText = guideInputText,
                isDictating = inlineDictationTarget == "guide_input",
                isLiveMode = isLiveMultimodalActive,
                liveStatus = liveConnectionStatus,
                liveMetrics = liveMetrics,
                liveMicAmplitude = liveMicAmplitude,
                liveSpeakerAmplitude = liveSpeakerAmplitude,
                selectedVoice = selectedLiveVoice,
                onToggleLiveMode = onToggleLiveMode,
                onSelectVoice = onSelectLiveVoice,
                onTriggerBargeIn = onTriggerLiveBargeIn,
                onInputChange = onGuideInputChange,
                onSendMessage = onSendGuideMessage,
                onTriggerSensoryAnchor = onTriggerSensoryAnchor,
                onToggleTts = onToggleGuideTts,
                onStopSpeaking = onStopGuideSpeaking,
                onToggleDictation = {
                    if (inlineDictationTarget == "guide_input") {
                        onStopInlineDictation()
                    } else {
                        onStartInlineDictation("guide_input", guideInputText, onGuideInputChange)
                    }
                },
                onDismiss = onEndGuideSession,
                onSaveExploration = onSaveGuideExploration
            )
        }

        // INTERACTIVE CONVERSATIONAL GAP PROBER MODAL
        if (probingGapQuestion != null && selectedMemory != null) {
            ConversationalProbeModal(
                memory = selectedMemory,
                gapQuestion = probingGapQuestion,
                answerText = probeAnswerText,
                isLoading = isProbingLoading,
                isInlineDictating = inlineDictationTarget == "probe_answer",
                onAnswerChange = onProbeAnswerChange,
                onDismiss = { onStartProbingGap(null) },
                onSubmitAnswer = { answer ->
                    onSubmitProbeAnswer(selectedMemory.id, probingGapQuestion, answer)
                },
                onToggleDictate = {
                    if (inlineDictationTarget == "probe_answer") {
                        onStopInlineDictation()
                    } else {
                        onStartInlineDictation("probe_answer", probeAnswerText, onProbeAnswerChange)
                    }
                }
            )
        }

        // JSON INSPECT MODAL
        if (jsonInspectMemory != null) {
            JsonNodeInspectModal(
                memory = jsonInspectMemory!!,
                onDismiss = { jsonInspectMemory = null },
                onCopy = { jsonStr ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("EpisodicMemoryNode JSON", jsonStr))
                    Toast.makeText(context, "EpisodicMemoryNode JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

/**
 * Ingestion Card where users can dictate, type, or load preset multimodal inputs.
 */
@Composable
fun EpisodicIngestionCard(
    inputText: String,
    imageDesc: String,
    isExpanded: Boolean,
    isIngesting: Boolean,
    ingestStage: String,
    isInlineDictatingText: Boolean,
    isInlineDictatingImage: Boolean,
    onExpandToggle: () -> Unit,
    onInputTextChange: (String) -> Unit,
    onImageDescChange: (String) -> Unit,
    onIngest: () -> Unit,
    onPresetSelect: (String, String) -> Unit,
    onToggleDictateText: () -> Unit,
    onToggleDictateImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MonoSurface)
            .border(
                1.dp,
                if (isIngesting) ActiveAccent.copy(alpha = pulseGlow) else MonoBorder,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ActiveAccentSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Episodic Engine",
                        tint = ActiveAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "EPISODIC INGESTION ENGINE",
                        style = TextStyle(
                            color = MonoWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Temporal • Entity Graph • Emotional Tone • Sensory Cues",
                        style = TextStyle(
                            color = MonoTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MonoSurfaceElevated)
                    .clickable { onExpandToggle() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isExpanded) "COLLAPSE" else "+ INGEST NEW",
                    style = TextStyle(
                        color = ActiveAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }

        // Presets when collapsed
        if (!isExpanded && !isIngesting) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Try sample episodic life memories:",
                style = TextStyle(color = MonoTextMuted, fontSize = 11.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    PresetChip(
                        label = "Cabin Snowstorm (Tahoe)",
                        onClick = {
                            onPresetSelect(
                                "Late autumn 2021 cabin trip with Mom and Alex in Lake Tahoe. We arrived as the first snowfall hit, lighting the old wood stove with the rhythmic sound of rain on the tin roof. Smelled like pine needles and cocoa.",
                                "A vintage red wood cabin nestled in snowy pine woods at dusk with warm amber interior light."
                            )
                        }
                    )
                }
                item {
                    PresetChip(
                        label = "College Dorm Move-In",
                        onClick = {
                            onPresetSelect(
                                "Summer 2017 move-in day at the college freshman dorm with Dad. Sweating in the late August afternoon heat, carrying the old acoustic guitar and heavy boxes up four flights of stairs.",
                                "A bustling university quad in golden summer sunlight with students carrying storage bins."
                            )
                        }
                    )
                }
                item {
                    PresetChip(
                        label = "Garage Honda Civic Project",
                        onClick = {
                            onPresetSelect(
                                "Late spring 2023 working on the old 1998 Honda Civic in the backyard garage with brother Mike. Smell of motor oil and hot exhaust while classic rock played quietly on the dusty radio.",
                                "Sunlight streaming through dusty garage windows onto an open car engine bay."
                            )
                        }
                    )
                }
            }
        }

        // Expanded Ingestion Form
        AnimatedVisibility(visible = isExpanded || isIngesting) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                // Multimodal Input 1: Spoken / Text Memory
                Text(
                    text = "RAW MEMORY / AUDIO TRANSCRIPT",
                    style = TextStyle(
                        color = MonoTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MonoBackground)
                        .border(
                            1.dp,
                            if (isInlineDictatingText) ActiveAccent else MonoBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = onInputTextChange,
                            textStyle = TextStyle(
                                color = MonoWhite,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            ),
                            cursorBrush = SolidColor(ActiveAccent),
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp)
                                .testTag("episodic_input_field"),
                            decorationBox = { innerTextField ->
                                if (inputText.isBlank()) {
                                    Text(
                                        text = "Dictate or write a personal memory (e.g. \"Winter 2019 cabin trip with Mom and Alex in Tahoe...\")",
                                        style = TextStyle(color = MonoTextMuted, fontSize = 13.sp, lineHeight = 19.sp)
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onToggleDictateText,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isInlineDictatingText) ActiveAccentSubtle else MonoSurfaceElevated)
                                .testTag("episodic_dictate_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Dictate Memory",
                                tint = if (isInlineDictatingText) ActiveAccent else MonoTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Multimodal Input 2: Visual Cues / Photo Context
                Text(
                    text = "ATTACHED PHOTO CONTEXT / VISUAL DESCRIPTION (OPTIONAL)",
                    style = TextStyle(
                        color = MonoTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MonoBackground)
                        .border(
                            1.dp,
                            if (isInlineDictatingImage) ActiveAccent else MonoBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = imageDesc,
                            onValueChange = onImageDescChange,
                            textStyle = TextStyle(
                                color = MonoWhite,
                                fontSize = 12.sp
                            ),
                            cursorBrush = SolidColor(ActiveAccent),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("episodic_image_field"),
                            decorationBox = { innerTextField ->
                                if (imageDesc.isBlank()) {
                                    Text(
                                        text = "e.g., Red vintage car parked beside rainy lake at dusk",
                                        style = TextStyle(color = MonoTextMuted, fontSize = 12.sp)
                                    )
                                }
                                innerTextField()
                            }
                        )

                        IconButton(
                            onClick = onToggleDictateImage,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isInlineDictatingImage) ActiveAccentSubtle else MonoSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Dictate Photo Context",
                                tint = if (isInlineDictatingImage) ActiveAccent else MonoTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Processing Stage Progress Indicator
                if (isIngesting) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MonoBackground)
                            .border(1.dp, ActiveAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ActiveAccent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = ingestStage.ifBlank { "Ingesting Episodic Memory Node..." },
                                style = TextStyle(
                                    color = ActiveAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Ingest Action Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (inputText.isNotBlank() && !isIngesting) ActiveAccent else MonoSurfaceElevated
                        )
                        .clickable(enabled = inputText.isNotBlank() && !isIngesting) { onIngest() }
                        .padding(vertical = 12.dp)
                        .testTag("episodic_ingest_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (inputText.isNotBlank()) MonoBackground else MonoTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isIngesting) "EXTRACTING EPISODIC NODE..." else "RUN EPISODIC INGESTION",
                            style = TextStyle(
                                color = if (inputText.isNotBlank()) MonoBackground else MonoTextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MonoSurfaceElevated)
            .border(1.dp, MonoBorderSubtle, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(color = MonoWhite, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        )
    }
}

/**
 * Filter and Search Bar for Episodic Vault
 */
@Composable
fun EpisodicSearchAndFilterBar(
    searchQuery: String,
    selectedLifeStage: String?,
    selectedValence: String?,
    totalCount: Int,
    filteredCount: Int,
    isInlineDictating: Boolean,
    onSearchChange: (String) -> Unit,
    onSelectLifeStage: (String?) -> Unit,
    onSelectValence: (String?) -> Unit,
    onToggleDictateSearch: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Search text field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MonoSurface)
                .border(
                    1.dp,
                    if (isInlineDictating) ActiveAccent else MonoBorder,
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MonoTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    textStyle = TextStyle(color = MonoWhite, fontSize = 13.sp),
                    cursorBrush = SolidColor(ActiveAccent),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("episodic_search_field"),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isBlank()) {
                            Text(
                                text = "Search memories by person, location, emotion, keyword...",
                                style = TextStyle(color = MonoTextMuted, fontSize = 13.sp)
                            )
                        }
                        innerTextField()
                    }
                )

                if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = { onSearchChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MonoTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onToggleDictateSearch,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isInlineDictating) ActiveAccentSubtle else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Dictate Search",
                        tint = if (isInlineDictating) ActiveAccent else MonoTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterPill(
                    label = "ALL ($totalCount)",
                    isSelected = selectedLifeStage == null && selectedValence == null,
                    onClick = {
                        onSelectLifeStage(null)
                        onSelectValence(null)
                    }
                )
            }
            item {
                FilterPill(
                    label = "Childhood",
                    isSelected = selectedLifeStage.equals("Childhood", ignoreCase = true),
                    onClick = { onSelectLifeStage("Childhood") }
                )
            }
            item {
                FilterPill(
                    label = "College",
                    isSelected = selectedLifeStage.equals("College", ignoreCase = true),
                    onClick = { onSelectLifeStage("College") }
                )
            }
            item {
                FilterPill(
                    label = "Early Career",
                    isSelected = selectedLifeStage.equals("Early Career", ignoreCase = true),
                    onClick = { onSelectLifeStage("Early Career") }
                )
            }
            item {
                FilterPill(
                    label = "Bittersweet",
                    isSelected = selectedValence.equals("bittersweet", ignoreCase = true),
                    onClick = { onSelectValence("bittersweet") }
                )
            }
            item {
                FilterPill(
                    label = "Melancholic",
                    isSelected = selectedValence.equals("melancholic", ignoreCase = true),
                    onClick = { onSelectValence("melancholic") }
                )
            }
            item {
                FilterPill(
                    label = "Positive",
                    isSelected = selectedValence.equals("positive", ignoreCase = true),
                    onClick = { onSelectValence("positive") }
                )
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) ActiveAccentSubtle else MonoSurface)
            .border(
                1.dp,
                if (isSelected) ActiveAccent else MonoBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = if (isSelected) ActiveAccent else MonoTextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

/**
 * Structured Episodic Memory Node Card
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EpisodicMemoryNodeCard(
    memory: EpisodicMemoryEntity,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onInspectJson: () -> Unit,
    onDelete: () -> Unit,
    onStartProbingGap: (String) -> Unit,
    onExploreWithGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val valenceColor = when (memory.emotionalValence.lowercase()) {
        "positive" -> Color(0xFF66BB6A)
        "bittersweet" -> Color(0xFFFFB74D)
        "melancholic" -> Color(0xFF90CAF9)
        "negative" -> ActiveDestructive
        else -> ActiveAccent
    }

    val people = memory.getPeopleList()
    val locations = memory.getLocationsList()
    val physicalObjects = memory.getPhysicalObjectsList()
    val sensoryCues = memory.getSensoryCuesList()
    val unresolvedGaps = memory.getUnresolvedGapsList()
    val searchKeywords = memory.getSearchKeywordsList()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MonoSurface)
            .border(
                1.dp,
                if (isSelected) ActiveAccent else MonoBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable { onCardClick() }
            .padding(16.dp)
    ) {
        // TOP HEADER: TEMPORAL ANCHOR & EMOTIONAL PROFILE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Temporal Anchor Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = ActiveAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${memory.timeframeReferenced.uppercase()} • ${memory.relativeLifeStage.uppercase()}",
                    style = TextStyle(
                        color = ActiveAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )
            }

            // Emotional Valence Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(valenceColor.copy(alpha = 0.15f))
                    .border(1.dp, valenceColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = memory.primaryTone,
                    style = TextStyle(
                        color = valenceColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // CORE NARRATIVE SUMMARY
        Text(
            text = memory.narrativeSummary,
            style = TextStyle(
                color = MonoWhite,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium
            )
        )

        // SENSORY DETAILS SECTION
        if (sensoryCues.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "SENSORY CUES",
                style = TextStyle(
                    color = MonoTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sensoryCues.forEach { cue ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MonoSurfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MonoTextSecondary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = cue,
                                style = TextStyle(color = MonoTextSecondary, fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        }

        // ENTITIES GRAPH (PEOPLE, LOCATIONS, OBJECTS)
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // People
            people.forEach { p ->
                EntityPill(
                    icon = Icons.Default.Person,
                    label = p,
                    tint = Color(0xFF64B5F6)
                )
            }
            // Locations
            locations.forEach { loc ->
                EntityPill(
                    icon = Icons.Default.Place,
                    label = loc,
                    tint = Color(0xFF81C784)
                )
            }
            // Objects
            physicalObjects.forEach { obj ->
                EntityPill(
                    icon = Icons.Default.Star,
                    label = obj,
                    tint = Color(0xFFFFB74D)
                )
            }
        }

        // UNRESOLVED GAPS & CONVERSATIONAL PROBES
        if (unresolvedGaps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MonoBackground)
                    .border(1.dp, MonoBorderSubtle, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = ActiveAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONVERSATIONAL EXPLORATION PROBES (${unresolvedGaps.size})",
                        style = TextStyle(
                            color = ActiveAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                unresolvedGaps.forEach { gap ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MonoSurfaceElevated)
                            .clickable { onStartProbingGap(gap) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• $gap",
                            style = TextStyle(
                                color = MonoWhite,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ActiveAccentSubtle)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ANSWER",
                                style = TextStyle(
                                    color = ActiveAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MonoBorderSubtle, thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // FOOTER: TIMESTAMP & ACTIONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Node ID: ${memory.id.take(14)}",
                style = TextStyle(
                    color = MonoTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Explore with Voice Guide Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ActiveAccentSubtle)
                        .border(1.dp, ActiveAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable { onExploreWithGuide() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ActiveAccent,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VOICE GUIDE",
                            style = TextStyle(
                                color = ActiveAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Inspect Raw JSON Button
                IconButton(
                    onClick = onInspectJson,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Inspect JSON",
                        tint = MonoTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Memory",
                        tint = MonoTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EntityPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = TextStyle(
                    color = tint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/**
 * Interactive Conversational Probe Modal allowing user to clarify ambiguities and enrich memory node.
 */
@Composable
fun ConversationalProbeModal(
    memory: EpisodicMemoryEntity,
    gapQuestion: String,
    answerText: String,
    isLoading: Boolean,
    isInlineDictating: Boolean,
    onAnswerChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmitAnswer: (String) -> Unit,
    onToggleDictate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(MonoSurface)
                .border(1.dp, ActiveAccent, RoundedCornerShape(16.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = ActiveAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MEMORY EXPLORATION PROBE",
                        style = TextStyle(
                            color = MonoWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MonoTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // The Unresolved Question
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MonoSurfaceElevated)
                    .border(1.dp, MonoBorderSubtle, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = gapQuestion,
                    style = TextStyle(
                        color = MonoWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 19.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Answer Input Box
            Text(
                text = "YOUR CLARIFICATION / MEMORY DETAIL",
                style = TextStyle(
                    color = MonoTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MonoBackground)
                    .border(
                        1.dp,
                        if (isInlineDictating) ActiveAccent else MonoBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    BasicTextField(
                        value = answerText,
                        onValueChange = onAnswerChange,
                        textStyle = TextStyle(color = MonoWhite, fontSize = 13.sp, lineHeight = 19.sp),
                        cursorBrush = SolidColor(ActiveAccent),
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .testTag("probe_answer_field"),
                        decorationBox = { innerTextField ->
                            if (answerText.isBlank()) {
                                Text(
                                    text = "Speak or type the missing detail (e.g. \"It was uncle Dave driving the second car...\")",
                                    style = TextStyle(color = MonoTextMuted, fontSize = 13.sp, lineHeight = 19.sp)
                                )
                            }
                            innerTextField()
                        }
                    )

                    IconButton(
                        onClick = onToggleDictate,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isInlineDictating) ActiveAccentSubtle else MonoSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Dictate Answer",
                            tint = if (isInlineDictating) ActiveAccent else MonoTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (answerText.isNotBlank() && !isLoading) ActiveAccent else MonoSurfaceElevated)
                    .clickable(enabled = answerText.isNotBlank() && !isLoading) {
                        onSubmitAnswer(answerText)
                    }
                    .padding(vertical = 12.dp)
                    .testTag("probe_submit_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MonoBackground,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "ENRICH EPISODIC MEMORY",
                        style = TextStyle(
                            color = if (answerText.isNotBlank()) MonoBackground else MonoTextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Strict Output Schema JSON Node Inspector Modal
 */
@Composable
fun JsonNodeInspectModal(
    memory: EpisodicMemoryEntity,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    val jsonString = remember(memory) {
        val root = JSONObject().apply {
            put("\$schema", "http://json-schema.org/draft-07/schema#")
            put("title", "EpisodicMemoryNode")
            put("type", "object")
            put("memory_id", memory.id)
            put("temporal_anchor", JSONObject().apply {
                put("timestamp_recorded", memory.isoTimestamp)
                put("timeframe_referenced", memory.timeframeReferenced)
                put("relative_life_stage", memory.relativeLifeStage)
            })
            put("entities", JSONObject().apply {
                put("people", org.json.JSONArray(memory.peopleJson))
                put("locations", org.json.JSONArray(memory.locationsJson))
                put("physical_objects", org.json.JSONArray(memory.physicalObjectsJson))
            })
            put("emotional_profile", JSONObject().apply {
                put("primary_tone", memory.primaryTone)
                put("emotional_valence", memory.emotionalValence)
                put("notable_shifts", memory.notableShifts)
            })
            put("sensory_cues", org.json.JSONArray(memory.sensoryCuesJson))
            put("narrative_summary", memory.narrativeSummary)
            put("unresolved_gaps", org.json.JSONArray(memory.unresolvedGapsJson))
            put("search_keywords", org.json.JSONArray(memory.searchKeywordsJson))
        }
        root.toString(2)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(16.dp))
                .background(MonoSurface)
                .border(1.dp, MonoBorder, RoundedCornerShape(16.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = ActiveAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EPISODIC NODE JSON SCHEMA",
                        style = TextStyle(
                            color = MonoWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MonoTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MonoBackground)
                    .border(1.dp, MonoBorderSubtle, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                LazyColumn {
                    item {
                        Text(
                            text = jsonString,
                            style = TextStyle(
                                color = Color(0xFF81D4FA),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ActiveAccent)
                    .clickable { onCopy(jsonString) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = MonoBackground,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COPY JSON OBJECT",
                        style = TextStyle(
                            color = MonoBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Empty Vault State
 */
@Composable
fun EmptyEpisodicVaultView(
    hasMemories: Boolean,
    onSeedSample: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MonoSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MonoTextSecondary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (hasMemories) "No memories match current filter" else "No Episodic Memories Ingested Yet",
            style = TextStyle(
                color = MonoWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Record or write a personal narrative with sensory details, dates, people, and locations to index your memory graph.",
            style = TextStyle(
                color = MonoTextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        )

        if (!hasMemories) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ActiveAccentSubtle)
                    .border(1.dp, ActiveAccent, RoundedCornerShape(10.dp))
                    .clickable { onSeedSample() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ActiveAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LOAD SAMPLE LAKE TAHOE MEMORY",
                        style = TextStyle(
                            color = ActiveAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Top Launcher Banner for Voice Memory Exploration Guide
 */
@Composable
fun VoiceMemoryGuideLauncherBanner(
    isSpeaking: Boolean,
    onLaunch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MonoSurface)
            .border(1.dp, ActiveAccent.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .clickable { onLaunch() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ActiveAccentSubtle)
                        .border(1.dp, ActiveAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ActiveAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "VOICE MEMORY GUIDE",
                            style = TextStyle(
                                color = MonoWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ActiveAccentSubtle)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SOCRATIC",
                                style = TextStyle(
                                    color = ActiveAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Unpack past recordings, photos, and sensory cues in voice conversation.",
                        style = TextStyle(
                            color = MonoTextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ActiveAccent)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "EXPLORE",
                    style = TextStyle(
                        color = MonoBackground,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                )
            }
        }
    }
}

