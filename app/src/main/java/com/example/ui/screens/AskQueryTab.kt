package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BriefingDossierEntity
import com.example.data.local.ContextNoteEntity
import com.example.ui.components.Spatial3DCard
import com.example.ui.components.SpatialHologramCore
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AskQuerySection {
    MEMORY_QUERY,
    PRE_MEETING_BRIEFINGS,
    PROMPT_EXPORT_BRIDGE
}

@Composable
fun AskQueryTab(
    notes: List<ContextNoteEntity>,
    dossiers: List<BriefingDossierEntity>,
    ragQuery: String,
    onRagQueryChange: (String) -> Unit,
    ragAnswer: String?,
    isGeneratingRag: Boolean,
    onAskRag: (String) -> Unit,
    onGenerateBriefing: (String) -> Unit,
    onDeleteBriefing: (String) -> Unit,
    onGeneratePromptExport: suspend (String, String) -> String,
    onSpeakAnswer: (String) -> Unit,
    isSpeaking: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedSection by remember { mutableStateOf(AskQuerySection.MEMORY_QUERY) }

    // Pre-Meeting Briefing target contact state
    var briefingContactTarget by remember { mutableStateOf("") }
    var isGeneratingBriefingLocal by remember { mutableStateOf(false) }

    // Prompt Export states
    var targetLlm by remember { mutableStateOf("Claude 3.5 Sonnet") }
    var promptTaskCustom by remember { mutableStateOf("Synthesize all recent meetings, decisions made, and outstanding action items into an executive status report.") }
    var generatedExportPrompt by remember { mutableStateOf("") }
    var isBuildingPrompt by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        // Top Header Tabs with 3D Holographic HUD
        Surface(
            color = CyberSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Ask AI Assistant",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ElectricCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Smart Search", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                            }
                        }
                        Text(
                            text = "Ask questions across your ${notes.size} notes & recordings",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    // Mini 3D Holographic Orb in header
                    Box(modifier = Modifier.size(46.dp)) {
                        SpatialHologramCore(
                            amplitude = if (isGeneratingRag || isSpeaking) 0.8f else 0.15f,
                            isInteracting = isSpeaking,
                            colorAccent = if (isSpeaking) AcidGreen else ElectricCyan,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = selectedSection.ordinal,
                    containerColor = CyberSurfaceVariant,
                    contentColor = ElectricCyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedSection == AskQuerySection.MEMORY_QUERY,
                        onClick = { selectedSection = AskQuerySection.MEMORY_QUERY },
                        text = { Text("Ask Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedSection == AskQuerySection.MEMORY_QUERY) ElectricCyan else TextSecondary) }
                    )
                    Tab(
                        selected = selectedSection == AskQuerySection.PRE_MEETING_BRIEFINGS,
                        onClick = { selectedSection = AskQuerySection.PRE_MEETING_BRIEFINGS },
                        text = { Text("Meeting Prep", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedSection == AskQuerySection.PRE_MEETING_BRIEFINGS) ElectricCyan else TextSecondary) }
                    )
                    Tab(
                        selected = selectedSection == AskQuerySection.PROMPT_EXPORT_BRIDGE,
                        onClick = { selectedSection = AskQuerySection.PROMPT_EXPORT_BRIDGE },
                        text = { Text("Export Prompts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedSection == AskQuerySection.PROMPT_EXPORT_BRIDGE) ElectricCyan else TextSecondary) }
                    )
                }
            }
        }

        // Section Content
        when (selectedSection) {
            AskQuerySection.MEMORY_QUERY -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 3D Spatial Query Input Card
                    item {
                        Spatial3DCard(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = ElectricCyan
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Ask anything about your past conversations",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = ragQuery,
                                    onValueChange = onRagQueryChange,
                                    placeholder = { Text("e.g., What did Alex quote for Apex Cloud infrastructure?", fontSize = 13.sp, color = TextMuted) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("rag_query_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricCyan,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = CyberSurfaceVariant,
                                        unfocusedContainerColor = CyberSurfaceVariant
                                    ),
                                    minLines = 2
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Quick prompt chips
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = CyberSurfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                            modifier = Modifier.clickable {
                                                onRagQueryChange("What commitments did I make this week?")
                                            }
                                        ) {
                                            Text(
                                                "Commitments?",
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                color = ElectricCyan
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = CyberSurfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                            modifier = Modifier.clickable {
                                                onRagQueryChange("Summary of legal and patent clauses discussed?")
                                            }
                                        ) {
                                            Text(
                                                "Patents & Legal?",
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                color = NeonViolet
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { onAskRag(ragQuery) },
                                        enabled = ragQuery.isNotBlank() && !isGeneratingRag,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF07090E)),
                                        modifier = Modifier.testTag("rag_submit_btn")
                                    ) {
                                        if (isGeneratingRag) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF07090E), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Query AI", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3D Spatial RAG Answer Card
                    if (ragAnswer != null || isGeneratingRag) {
                        item {
                            Spatial3DCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rag_answer_card"),
                                accentColor = NeonViolet
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                                            Text("Spatial 3D Synthesis", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                                        }

                                        if (ragAnswer != null) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = { onSpeakAnswer(ragAnswer) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.VolumeUp, contentDescription = "Read aloud", modifier = Modifier.size(18.dp), tint = AcidGreen)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("AI Answer", ragAnswer))
                                                        Toast.makeText(context, "Answer copied to clipboard!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp), tint = TextSecondary)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (isGeneratingRag) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ElectricCyan)
                                            Text("Projecting 3D vector embeddings and synthesizing answer...", fontSize = 13.sp, color = TextSecondary)
                                        }
                                    } else if (ragAnswer != null) {
                                        Text(
                                            text = ragAnswer,
                                            fontSize = 14.sp,
                                            lineHeight = 21.sp,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Recent Memories Context Reference
                    item {
                        Text(
                            text = "ACTIVE LOCAL KNOWLEDGE BASE (${notes.size} SESSIONS)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    items(notes.take(4)) { note ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = note.title.ifBlank { note.matterCode },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Speakers: ${note.participants} • ${note.durationSeconds}s",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE6F4EA))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text("Indexed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF137333))
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            AskQuerySection.PRE_MEETING_BRIEFINGS -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Generate Briefing Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = M3Primary, modifier = Modifier.size(18.dp))
                                    Text("Generate Pre-Meeting Briefing", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }

                                Text(
                                    text = "Enter a person, client, or topic to instantly compile past commitments, decisions, and agenda priorities.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )

                                OutlinedTextField(
                                    value = briefingContactTarget,
                                    onValueChange = { briefingContactTarget = it },
                                    placeholder = { Text("e.g. Alex, Sarah, Vanguard Legal, Apex Cloud", fontSize = 13.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Preset chips
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("Alex", "Sarah", "Vanguard", "Apex").forEach { preset ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.clickable { briefingContactTarget = preset }
                                            ) {
                                                Text(
                                                    preset,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (briefingContactTarget.isNotBlank()) {
                                                onGenerateBriefing(briefingContactTarget)
                                            }
                                        },
                                        enabled = briefingContactTarget.isNotBlank(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = M3Primary)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Generate Dossier", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Existing Dossiers
                    item {
                        Text(
                            text = "PRE-MEETING DOSSIERS (${dossiers.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (dossiers.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.outline)
                                    Text("No Pre-Meeting Dossiers Generated", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Type a contact name above to assemble an executive briefing.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        items(dossiers, key = { it.id }) { dossier ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = dossier.title,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Generated ${SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date(dossier.generatedAt))}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val fullDossier = """
[PRE-MEETING DOSSIER: ${dossier.targetPersonOrTopic}]
Executive Summary:
${dossier.executiveSummary}

Key Decisions Made:
${dossier.keyDecisions}

Open Action Items:
${dossier.openActionItems}
                                                    """.trimIndent()
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Dossier", fullDossier))
                                                    Toast.makeText(context, "Dossier copied to clipboard!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                                            }

                                            IconButton(
                                                onClick = { onDeleteBriefing(dossier.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Executive Summary
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Executive Briefing", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = M3Primary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(dossier.executiveSummary, fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Key Decisions
                                    if (dossier.keyDecisions.isNotBlank()) {
                                        Text("Key Decisions: ${dossier.keyDecisions}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF137333))
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    // Open Action Items
                                    if (dossier.openActionItems.isNotBlank()) {
                                        Text("Open Commitments: ${dossier.openActionItems}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE37400))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            AskQuerySection.PROMPT_EXPORT_BRIDGE -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = M3Primary, modifier = Modifier.size(18.dp))
                                    Text("Inject Memories into External LLMs", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }

                                Text(
                                    text = "Package encrypted memory transcripts and diarised summaries into structured XML/Markdown prompt templates for Claude, ChatGPT, or custom agents.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text("Target LLM Format:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Claude 3.5 Sonnet", "ChatGPT (GPT-4o)", "Custom Agent").forEach { llm ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (targetLlm == llm) M3PrimaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { targetLlm = llm }
                                        ) {
                                            Text(
                                                text = llm,
                                                fontSize = 11.sp,
                                                fontWeight = if (targetLlm == llm) FontWeight.Bold else FontWeight.Normal,
                                                color = if (targetLlm == llm) M3Primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Task Prompt for External LLM:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = promptTaskCustom,
                                    onValueChange = { promptTaskCustom = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    minLines = 2
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isBuildingPrompt = true
                                            generatedExportPrompt = onGeneratePromptExport(targetLlm, promptTaskCustom)
                                            isBuildingPrompt = false
                                        }
                                    },
                                    enabled = !isBuildingPrompt,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = M3Primary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isBuildingPrompt) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Build Formatted Context Prompt")
                                    }
                                }
                            }
                        }
                    }

                    // Prompt Preview & Copy
                    if (generatedExportPrompt.isNotBlank()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Ready-to-Paste LLM Prompt", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = M3Primary)
                                        Button(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("LLM Prompt", generatedExportPrompt))
                                                Toast.makeText(context, "Copied prompt with memory context!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = M3Primary)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy to Clipboard", fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = generatedExportPrompt,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(12.dp),
                                            maxLines = 15
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}
