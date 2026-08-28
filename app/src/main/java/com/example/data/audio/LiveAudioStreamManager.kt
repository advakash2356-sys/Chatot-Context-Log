package com.example.data.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * LiveAudioStreamManager
 * Manages raw low-latency PCM audio streaming for Gemini Multimodal Live API.
 * 
 * - Input: 16,000 Hz, 16-bit PCM, Mono Little-Endian (512-1024 byte chunks, ~32-64ms)
 * - Output: 24,000 Hz, 16-bit PCM, Mono Little-Endian streaming AudioTrack
 * - Interruption (Barge-in): Immediately flushes AudioTrack buffer when user speaks or server signals.
 */
class LiveAudioStreamManager(
    private val context: Context
) {
    companion object {
        const val INPUT_SAMPLE_RATE = 16000
        const val OUTPUT_SAMPLE_RATE = 24000
        const val CHUNK_SIZE_BYTES = 1024 // ~32ms slice at 16kHz 16-bit mono
    }

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordingJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _micAmplitude = MutableStateFlow(0f)
    val micAmplitude: StateFlow<Float> = _micAmplitude.asStateFlow()

    private val _speakerAmplitude = MutableStateFlow(0f)
    val speakerAmplitude: StateFlow<Float> = _speakerAmplitude.asStateFlow()

    private val _isBargeInActive = MutableStateFlow(false)
    val isBargeInActive: StateFlow<Boolean> = _isBargeInActive.asStateFlow()

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                OUTPUT_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(OUTPUT_SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            audioTrack = AudioTrack(
                audioAttributes,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e("LiveAudioStreamManager", "Failed to initialize AudioTrack", e)
        }
    }

    /**
     * Starts capturing 16kHz PCM audio and emits chunks to onPcmChunk.
     */
    @SuppressLint("MissingPermission")
    fun startCapture(
        scope: CoroutineScope,
        onPcmChunk: suspend (ByteArray) -> Unit
    ) {
        if (_isRecording.value) return

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                INPUT_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = (minBufferSize * 2).coerceAtLeast(CHUNK_SIZE_BYTES * 2)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                INPUT_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("LiveAudioStreamManager", "AudioRecord initialization failed")
                return
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(CHUNK_SIZE_BYTES)
                while (isActive && _isRecording.value) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readBytes > 0) {
                        val chunk = buffer.copyOf(readBytes)
                        calculateMicAmplitude(chunk)
                        
                        // Check if mic volume crosses threshold to trigger local barge-in flush
                        val currentAmp = _micAmplitude.value
                        if (currentAmp > 0.25f && _isPlaying.value) {
                            flushSpeakerBuffer("User speech detected (local barge-in)")
                        }

                        onPcmChunk(chunk)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LiveAudioStreamManager", "Error starting audio capture", e)
            _isRecording.value = false
        }
    }

    /**
     * Stops audio capture.
     */
    fun stopCapture() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w("LiveAudioStreamManager", "Error releasing AudioRecord", e)
        }
        audioRecord = null
        _micAmplitude.value = 0f
    }

    /**
     * Plays a 24kHz raw PCM chunk received from Gemini Live.
     */
    fun writeSpeakerPcm(pcm24k: ByteArray) {
        if (pcm24k.isEmpty()) return
        try {
            if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                initAudioTrack()
            }
            if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.play()
            }

            _isPlaying.value = true
            calculateSpeakerAmplitude(pcm24k)

            audioTrack?.write(pcm24k, 0, pcm24k.size)
        } catch (e: Exception) {
            Log.e("LiveAudioStreamManager", "Error writing audio chunk to AudioTrack", e)
        }
    }

    /**
     * Instantly flushes and clears the speaker buffer (Barge-in / Interruption).
     */
    fun flushSpeakerBuffer(reason: String = "Interrupted") {
        try {
            Log.d("LiveAudioStreamManager", "Flushing AudioTrack buffer: $reason")
            _isBargeInActive.value = true
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
            _isPlaying.value = false
            _speakerAmplitude.value = 0f
            _isBargeInActive.value = false
        } catch (e: Exception) {
            Log.w("LiveAudioStreamManager", "Error flushing AudioTrack", e)
        }
    }

    fun release() {
        stopCapture()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.w("LiveAudioStreamManager", "Error releasing AudioTrack", e)
        }
        audioTrack = null
    }

    private fun calculateMicAmplitude(pcm: ByteArray) {
        if (pcm.size < 2) return
        var sum = 0.0
        val numShorts = pcm.size / 2
        for (i in 0 until numShorts) {
            val sample = (pcm[i * 2 + 1].toInt() shl 8) or (pcm[i * 2].toInt() and 0xFF)
            sum += sample * sample
        }
        val rms = sqrt(sum / numShorts)
        val normalized = (rms / 10000.0).toFloat().coerceIn(0f, 1f)
        _micAmplitude.value = normalized
    }

    private fun calculateSpeakerAmplitude(pcm: ByteArray) {
        if (pcm.size < 2) return
        var sum = 0.0
        val numShorts = pcm.size / 2
        for (i in 0 until numShorts) {
            val sample = (pcm[i * 2 + 1].toInt() shl 8) or (pcm[i * 2].toInt() and 0xFF)
            sum += sample * sample
        }
        val rms = sqrt(sum / numShorts)
        val normalized = (rms / 12000.0).toFloat().coerceIn(0f, 1f)
        _speakerAmplitude.value = normalized
    }
}
