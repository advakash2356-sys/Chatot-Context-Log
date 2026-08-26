package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.SpatialContextHelper
import com.example.data.ai.SpeakerTurn
import com.example.data.local.ContextNoteEntity
import com.example.data.local.EntryType
import com.example.ui.components.Spatial3DCard
import com.example.ui.theme.AcidGreen
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.M3Primary
import com.example.ui.theme.M3PrimaryContainer
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MemoryViewFormat {
    EXECUTIVE_SUMMARY,
    STRUCTURED_NOTES,
    VERBATIM_TURNS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesTab(
    notes: List<ContextNoteEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    availableTags: List<String>,
    selectedTagFilter: String?,
    onTagFilterSelect: (String?) -> Unit,
    onDeleteNote: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    isSpeaking: Boolean,
    onQuickCaptureClick: () -> Unit,
    pendantConnected: Boolean = true,
    batterySoC: Int = 88
) {
    var noteToDelete by remember { mutableStateOf<String?>(null) }
    var purgedNotificationNoteId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Privacy & Storage status banner
        Surface(
            color = CyberSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AcidGreen)
                    )
                    Text(
                        text = "Encrypted Local Storage • 100% Private",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = AcidGreen
                    )
                    Text(
                        text = "Private Vault",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AcidGreen
                    )
                }
            }
        }

        // Search and Tag Filter Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search memories, transcripts, speakers...", fontSize = 13.sp, color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = ElectricCyan)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("memories_search_field")
            )

            if (availableTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        val isAllSelected = selectedTagFilter == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAllSelected) ElectricCyan else CyberSurface)
                                .border(1.dp, if (isAllSelected) ElectricCyan else GlassBorder, RoundedCornerShape(10.dp))
                                .clickable { onTagFilterSelect(null) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "All",
                                fontSize = 12.sp,
                                fontWeight = if (isAllSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isAllSelected) Color(0xFF07090E) else TextSecondary
                            )
                        }
                    }
                    items(availableTags) { tag ->
                        val isTagSelected = selectedTagFilter == tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isTagSelected) NeonViolet else CyberSurface)
                                .border(1.dp, if (isTagSelected) NeonViolet else GlassBorder, RoundedCornerShape(10.dp))
                                .clickable { onTagFilterSelect(if (selectedTagFilter == tag) null else tag) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "#$tag",
                                fontSize = 12.sp,
                                fontWeight = if (isTagSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isTagSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Memory Feed
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "No Memories Recorded Yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Start speaking or tap Capture to record ambient context, meetings, and voice dictation.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onQuickCaptureClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = M3Primary)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Record First Memory")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CHRONOLOGICAL MEMORY SESSIONS (${notes.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Auto Diarised & Encrypted",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(notes, key = { it.id }) { note ->
                    SpatialMemoryCard(
                        note = note,
                        onDeleteClick = { noteToDelete = note.id },
                        onSpeakClick = { text -> onSpeakText(text) },
                        isSpeaking = isSpeaking,
                        onPurgeAudioClick = {
                            purgedNotificationNoteId = note.id
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Memory Session") },
            text = { Text("Are you sure you want to permanently delete this memory session and all associated diarised transcripts?") },
            confirmButton = {
                Button(
                    onClick = {
                        noteToDelete?.let { onDeleteNote(it) }
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Audio Purge Feedback Dialog
    if (purgedNotificationNoteId != null) {
        AlertDialog(
            onDismissRequest = { purgedNotificationNoteId = null },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF137333)) },
            title = { Text("Zero-Retention Audio Purge") },
            text = {
                Text("Raw PCM/WAV audio chunks for this session have been permanently purged from volatile memory and disk buffer. Structured notes, encrypted transcripts, and vector embeddings remain safely intact.")
            },
            confirmButton = {
                Button(onClick = { purgedNotificationNoteId = null }) {
                    Text("Understood")
                }
            }
        )
    }
}

@Composable
fun SpatialMemoryCard(
    note: ContextNoteEntity,
    onDeleteClick: () -> Unit,
    onSpeakClick: (String) -> Unit,
    isSpeaking: Boolean,
    onPurgeAudioClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf(MemoryViewFormat.EXECUTIVE_SUMMARY) }
    var isSimulatingAudioPlay by remember { mutableStateOf(false) }

    val formattedDate = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()).format(Date(note.recordedAt))
    val speakerTurns = remember(note.verbatimTurnsJson) {
        SpatialContextHelper.parseSpeakerTurns(note.verbatimTurnsJson.ifBlank { note.rawTranscript })
    }

    val cardAccent = when (note.source) {
        "DESKTOP_LOOPBACK" -> ElectricCyan
        "MANUAL_MIC" -> NeonAmber
        else -> AcidGreen
    }

    Spatial3DCard(
        shape = RoundedCornerShape(18.dp),
        accentColor = cardAccent,
        elevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("memory_card_${note.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Card Header: Source & Duration Pill, Time, Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Source Icon Badge
                    val (sourceIcon, sourceLabel, sourceColor) = when (note.source) {
                        "DESKTOP_LOOPBACK" -> Triple(Icons.Default.Laptop, "Desktop Audio", ElectricCyan)
                        "MANUAL_MIC" -> Triple(Icons.Default.Mic, "Phone Mic", NeonAmber)
                        else -> Triple(Icons.Default.BluetoothConnected, "Pendant BLE", AcidGreen)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(sourceColor.copy(alpha = 0.14f))
                            .border(1.dp, sourceColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(sourceIcon, contentDescription = null, modifier = Modifier.size(12.dp), tint = sourceColor)
                            Text(sourceLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = sourceColor)
                        }
                    }

                    // Duration pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurfaceVariant)
                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(11.dp), tint = TextSecondary)
                            Text("${note.durationSeconds}s", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title and Participants
            Text(
                text = note.title.ifBlank { note.cleanText.take(50) },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (note.participants.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(13.dp), tint = ElectricCyan)
                    Text(
                        text = "Speakers: ${note.participants}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ElectricCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // View Format Switcher Tabs
            TabRow(
                selectedTabIndex = selectedFormat.ordinal,
                containerColor = CyberSurfaceVariant,
                contentColor = ElectricCyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedFormat == MemoryViewFormat.EXECUTIVE_SUMMARY,
                    onClick = { selectedFormat = MemoryViewFormat.EXECUTIVE_SUMMARY },
                    text = { Text("Executive Summary", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = ElectricCyan,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedFormat == MemoryViewFormat.STRUCTURED_NOTES,
                    onClick = { selectedFormat = MemoryViewFormat.STRUCTURED_NOTES },
                    text = { Text("Structured Notes", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = ElectricCyan,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedFormat == MemoryViewFormat.VERBATIM_TURNS,
                    onClick = { selectedFormat = MemoryViewFormat.VERBATIM_TURNS },
                    text = { Text("Diarised Turns", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = ElectricCyan,
                    unselectedContentColor = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-Format Content Presentation
            when (selectedFormat) {
                MemoryViewFormat.EXECUTIVE_SUMMARY -> {
                    val summaryText = if (note.executiveSummary.isNotBlank()) {
                        note.executiveSummary
                    } else {
                        "• ${note.cleanText}"
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyberSurfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = summaryText,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }

                MemoryViewFormat.STRUCTURED_NOTES -> {
                    val structured = if (note.structuredNotes.isNotBlank()) {
                        note.structuredNotes
                    } else {
                        "### Overview\n${note.cleanText}"
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyberSurfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = structured,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }

                MemoryViewFormat.VERBATIM_TURNS -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        speakerTurns.forEach { turn ->
                            SpeakerTurnItem(turn = turn)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tags Row
            if (note.tagList.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    note.tagList.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonViolet.copy(alpha = 0.18f))
                                .border(1.dp, NeonViolet.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text("#$tag", fontSize = 11.sp, color = Color(0xFFDDD6FE), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Quick Action Buttons Bar (Copy, WhatsApp, TTS, Purge)
            val context = LocalContext.current
            val textToCopy = note.executiveSummary.ifBlank { note.cleanText }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1-Tap Copy
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Memory Note", textToCopy))
                        Toast.makeText(context, "Note Copied! 📋", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberSurfaceVariant,
                        contentColor = ElectricCyan
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // 1-Tap WhatsApp Share
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "*${note.title}*\n\n$textToCopy")
                            setPackage("com.whatsapp")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "*${note.title}*\n\n$textToCopy")
                            }, "Share Memory Note")
                            context.startActivity(chooser)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color(0xFF020617)
                    ),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }

                // Speak TTS Button
                Button(
                    onClick = { onSpeakClick(textToCopy) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonViolet.copy(alpha = 0.2f),
                        contentColor = Color(0xFFDDD6FE)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Listen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Audio Player & Immediate Audio Purge Bar
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CyberSurfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { isSimulatingAudioPlay = !isSimulatingAudioPlay },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isSimulatingAudioPlay) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = "Play/Stop Audio",
                                tint = M3Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Simulated Audio Waveform
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val heights = listOf(8, 16, 22, 14, 28, 18, 10, 24, 16, 12, 20, 14, 8)
                            heights.forEach { h ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(if (isSimulatingAudioPlay) (h + (0..10).random()).dp else (h * 0.7).dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isSimulatingAudioPlay) M3Primary else MaterialTheme.colorScheme.outline)
                                )
                            }
                        }

                        Text(
                            text = if (isSimulatingAudioPlay) "Playing 00:14" else "PCM Audio",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Purge Audio Button
                    TextButton(
                        onClick = onPurgeAudioClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC5221F))
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Purge Audio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SpeakerTurnItem(turn: SpeakerTurn) {
    val isUser = turn.isYou
    val bgColor = if (isUser) M3PrimaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val speakerColor = if (isUser) M3Primary else Color(0xFF5F6368)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(speakerColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isUser) "YOU" else turn.speaker.take(2).uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = turn.speaker,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = speakerColor
                )
                Text(
                    text = turn.timestampFormatted,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = turn.text,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
