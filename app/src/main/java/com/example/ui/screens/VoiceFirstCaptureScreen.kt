package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.audio.AudioCaptureState
import com.example.ui.components.Ambient3DBackground
import com.example.ui.components.FastTactileWaveform
import com.example.ui.components.TactileRecordButton

/**
 * VoiceFirstCaptureScreen is the instant, zero-friction primary capture surface.
 * Integrates an ambient 3D reactive shader canvas, floating frosted glass capsules,
 * realtime audio waveform visualizer, and skeuomorphic tactile hardware button.
 */
@Composable
fun VoiceFirstCaptureScreen(
    captureState: AudioCaptureState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    selectedLanguage: String = "Hinglish",
    onSelectLanguage: (String) -> Unit = {},
    interimTranscript: String = "",
    onQuickNoteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isRecording = captureState is AudioCaptureState.Recording
    val currentAmp = if (captureState is AudioCaptureState.Recording) captureState.amplitude else 0.08f
    val currentSec = if (captureState is AudioCaptureState.Recording) captureState.durationSeconds else 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .testTag("voice_first_capture_screen")
    ) {
        // LAYER 0: Reactive 3D Ambient Shader Mesh (Synced directly with capture amplitude)
        Ambient3DBackground(
            isRecording = isRecording,
            amplitude = currentAmp,
            modifier = Modifier.zIndex(0f)
        )

        // LAYER 1 & 2: Foreground Interface Grid
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .zIndex(5f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP FLOATING GLASS BAR (Z-Layer 5) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(6f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Floating Frosted AES-256 Security Capsule
                Box(
                    modifier = Modifier
                        .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.6f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.65f))
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isRecording) Color(0xFFEF4444) else Color(0xFF10B981),
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isRecording) "LIVE STREAM AES-256" else "ON-DEVICE ZERO-CLOUD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                // Floating Frosted Language Switcher Capsule
                Row(
                    modifier = Modifier
                        .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.6f))
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.65f))
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("EN", "HINGLISH", "HI").forEach { langCode ->
                        val isSelected = selectedLanguage.equals(langCode, ignoreCase = true) ||
                                (langCode == "EN" && selectedLanguage.startsWith("Eng", ignoreCase = true)) ||
                                (langCode == "HI" && selectedLanguage.equals("Hindi", ignoreCase = true))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))
                                        )
                                    } else {
                                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .clickable { onSelectLanguage(langCode) }
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = langCode,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) Color(0xFF020617) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // --- CENTER STAGE: DIGITAL TIME TRACKER & LIVE WAVEFORM (Z-Layer 4) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .zIndex(4f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Frosted Status Capsule
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isRecording) Color(0xFFEF4444).copy(alpha = 0.18f)
                            else Color.White.copy(alpha = 0.06f)
                        )
                        .border(
                            1.dp,
                            if (isRecording) Color(0xFFEF4444).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = if (isRecording) "● RECORDING SPEECH & DIARIZING" else "VOICE-FIRST INSTANT ENGINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = if (isRecording) Color(0xFFFCA5A5) else Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // High-Contrast Giant Timer
                Text(
                    text = String.format("%02d:%02d", currentSec / 60, currentSec % 60),
                    fontSize = 66.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    color = Color.White
                )

                // Optional Interim Live Transcript Streaming Banner
                AnimatedVisibility(
                    visible = isRecording && interimTranscript.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp, bottom = 4.dp)
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.7f))
                            .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = interimTranscript,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Reactive Canvas Waveform Equalizer
                FastTactileWaveform(
                    amplitude = currentAmp,
                    isRecording = isRecording,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // --- BOTTOM SECTION: 3D TACTILE HARDWARE BUTTON (Z-Layer 10) ---
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

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Automatic Audio Wipe Post-Transcription • Room AES-256",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}
