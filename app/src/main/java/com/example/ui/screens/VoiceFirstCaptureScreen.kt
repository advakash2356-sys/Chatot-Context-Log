package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.audio.AudioCaptureState
import com.example.ui.components.Ambient3DBackground
import com.example.ui.components.FastTactileWaveform
import com.example.ui.components.Spatial3DCard
import com.example.ui.components.SpatialHologramCore
import com.example.ui.components.TactileRecordButton
import com.example.ui.theme.AcidGreen
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.HapticFeedbackManager

enum class GenZVibe(val label: String, val emoji: String) {
    RAW("Original", "🎙️"),
    TLDR("Summary", "📝"),
    GEN_Z("Bullet Points", "📌"),
    HINGLISH("Hinglish", "🇮🇳"),
    TWEET("Takeaway", "✨")
}

@Composable
fun VoiceFirstCaptureScreen(
    captureState: AudioCaptureState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    selectedLanguage: String = "English",
    onSelectLanguage: (String) -> Unit = {},
    interimTranscript: String = "",
    lastSavedTranscript: String = "",
    onSaveToMemories: (text: String, title: String) -> Unit = { _, _ -> },
    onCreateTaskFromText: (String) -> Unit = {},
    onPromptVoiceQuery: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isRecording = captureState is AudioCaptureState.Recording
    val currentAmp = if (captureState is AudioCaptureState.Recording) captureState.amplitude else 0.08f
    val currentSec = if (captureState is AudioCaptureState.Recording) captureState.durationSeconds else 0
    val context = LocalContext.current
    val hapticManager = remember { HapticFeedbackManager.getInstance(context) }

    var activeVibe by remember { mutableStateOf(GenZVibe.RAW) }
    var copiedToClipboard by remember { mutableStateOf(false) }
    var quickTextInput by remember { mutableStateOf("") }

    val activeText = if (isRecording && interimTranscript.isNotBlank()) {
        interimTranscript
    } else if (lastSavedTranscript.isNotBlank()) {
        lastSavedTranscript
    } else {
        ""
    }

    fun getTransformedText(raw: String, vibe: GenZVibe): String {
        if (raw.isBlank()) return raw
        return when (vibe) {
            GenZVibe.RAW -> raw
            GenZVibe.TLDR -> "📝 Summary:\n• $raw\n\n🎯 Next Step: Action items extracted & queued."
            GenZVibe.GEN_Z -> "📌 Key Points:\n• ${raw.replace(". ", "\n• ")}"
            GenZVibe.HINGLISH -> "Bhai suno: $raw — scene sorted hai! 🚀"
            GenZVibe.TWEET -> "✨ Takeaway:\n$raw"
        }
    }

    val displayText = getTransformedText(activeText, activeVibe)

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Voice Transcript", text)
        clipboard.setPrimaryClip(clip)
        copiedToClipboard = true
        hapticManager.triggerSelection()
        Toast.makeText(context, "Copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
    }

    fun shareToWhatsApp(text: String) {
        hapticManager.triggerSelection()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }, "Share note via...")
            context.startActivity(chooser)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("voice_first_capture_screen")
    ) {
        // LAYER 0: Reactive 3D Ambient Shader Mesh
        Ambient3DBackground(
            isRecording = isRecording,
            amplitude = currentAmp,
            modifier = Modifier.zIndex(0f)
        )

        // LAYER 1 & 2: Foreground Interface
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
                .zIndex(5f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP STATUS & LANGUAGE SELECTOR BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(6f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Capsule
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.75f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isRecording) Color(0xFFEF4444) else AcidGreen,
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isRecording) "RECORDING NOW" else "READY TO RECORD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRecording) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)
                        )
                    }
                }

                // Language Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.75f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf("EN" to "English", "HINGLISH" to "Hinglish", "HI" to "Hindi").forEach { (code, label) ->
                        val isSelected = selectedLanguage.equals(label, ignoreCase = true) ||
                                selectedLanguage.equals(code, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) ElectricCyan else Color.Transparent
                                )
                                .clickable {
                                    hapticManager.triggerSelection()
                                    onSelectLanguage(label)
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = code,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF020617) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- CENTER 3D SPATIAL HOLOGRAPHIC ORB & TIMER ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(175.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SpatialHologramCore(
                        amplitude = currentAmp,
                        isInteracting = isRecording,
                        colorAccent = if (isRecording) Color(0xFFEF4444) else ElectricCyan,
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%02d:%02d", currentSec / 60, currentSec % 60),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isRecording) "SPEAKING" else "TAP MIC",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isRecording) NeonAmber else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                FastTactileWaveform(
                    amplitude = currentAmp,
                    isRecording = isRecording,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // --- QUICK TYPED NOTE INPUT (Zero friction typed capture) ---
            if (!isRecording && activeText.isBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickTextInput,
                        onValueChange = { quickTextInput = it },
                        placeholder = { Text("Or type a quick note...", color = TextMuted, fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_text_note_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    if (quickTextInput.isNotBlank()) {
                        Button(
                            onClick = {
                                if (quickTextInput.isNotBlank()) {
                                    hapticManager.triggerSelection()
                                    onSaveToMemories(quickTextInput, "Note ${System.currentTimeMillis() % 10000}")
                                    Toast.makeText(context, "Note saved! 💾", Toast.LENGTH_SHORT).show()
                                    quickTextInput = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF07090E)
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // --- TRANSCRIPT & ACTIONS CARD ---
            if (activeText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Spatial3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    accentColor = ElectricCyan
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Vibe Selector Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GenZVibe.values().forEach { vibe ->
                                val isVibeSelected = activeVibe == vibe
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isVibeSelected) ElectricCyan else CyberSurfaceVariant
                                        )
                                        .border(1.dp, if (isVibeSelected) ElectricCyan else GlassBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            hapticManager.triggerSelection()
                                            activeVibe = vibe
                                        }
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "${vibe.emoji} ${vibe.label}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isVibeSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isVibeSelected) Color(0xFF07090E) else TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Text Area
                        Text(
                            text = displayText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextPrimary,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4 Clear Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Save Note
                            Button(
                                onClick = {
                                    hapticManager.triggerSelection()
                                    onSaveToMemories(displayText, "Voice Note ${System.currentTimeMillis() % 10000}")
                                    Toast.makeText(context, "Saved to Notes! 💾", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonViolet,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // 2. Add as Task
                            Button(
                                onClick = {
                                    hapticManager.triggerSelection()
                                    onCreateTaskFromText(displayText)
                                    Toast.makeText(context, "Added to Tasks! ✅", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AcidGreen,
                                    contentColor = Color(0xFF020617)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Task", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // 3. Copy
                            Button(
                                onClick = { copyToClipboard(displayText) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberSurfaceVariant,
                                    contentColor = ElectricCyan
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(if (copiedToClipboard) "Copied" else "Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // 4. Share
                            Button(
                                onClick = { shareToWhatsApp(displayText) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF25D366),
                                    contentColor = Color(0xFF020617)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Quick Inspiration Chips
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "QUICK IDEAS TO CAPTURE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "💡 Brainstorm new project idea",
                            "📅 Daily standup notes",
                            "✅ To-do list for this afternoon",
                            "📊 Meeting key takeaways"
                        ).forEach { prompt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CyberSurface)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        hapticManager.triggerSelection()
                                        onPromptVoiceQuery(prompt)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = prompt,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- BOTTOM SECTION: 3D TACTILE HARDWARE BUTTON ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(10f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TactileRecordButton(
                    isRecording = isRecording,
                    onClick = {
                        if (isRecording) onStopRecording() else onStartRecording()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isRecording) "Tap button to stop recording" else "Tap button to start recording",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isRecording) NeonAmber else Color(0xFF94A3B8)
                )
            }
        }
    }
}
