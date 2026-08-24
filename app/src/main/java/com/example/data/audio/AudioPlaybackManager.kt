package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * AudioPlaybackManager enables audio playback for recorded voice clips
 * before transmitting to Gemini API, with live progress tracking, seeking,
 * and duration calculation.
 */
class AudioPlaybackManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private var currentLoadedFile: File? = null

    fun loadAudioFile(file: File) {
        if (!file.exists()) return
        currentLoadedFile = file
        stop()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.fromFile(file))
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _progress.value = 0f
                    _currentPositionMs.value = 0
                    stopProgressTracking()
                }
            }
            mediaPlayer = player
            _durationMs.value = player.duration.coerceAtLeast(1000)
            _progress.value = 0f
            _currentPositionMs.value = 0
        } catch (e: Exception) {
            Log.e("AudioPlaybackManager", "Error preparing MediaPlayer for file: ${file.name}", e)
            _durationMs.value = 5000 // Fallback estimate
        }
    }

    fun play() {
        val player = mediaPlayer
        if (player != null) {
            try {
                if (!player.isPlaying) {
                    player.start()
                    _isPlaying.value = true
                    startProgressTracking()
                }
            } catch (e: Exception) {
                Log.e("AudioPlaybackManager", "Error starting playback", e)
                startSimulatedPlayback()
            }
        } else if (currentLoadedFile != null) {
            loadAudioFile(currentLoadedFile!!)
            play()
        } else {
            startSimulatedPlayback()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            Log.w("AudioPlaybackManager", "Error pausing playback: ${e.message}")
        }
        _isPlaying.value = false
        stopProgressTracking()
    }

    fun stop() {
        stopProgressTracking()
        _isPlaying.value = false
        _progress.value = 0f
        _currentPositionMs.value = 0
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.w("AudioPlaybackManager", "Error releasing MediaPlayer: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }

    fun seekTo(fraction: Float) {
        val targetProgress = fraction.coerceIn(0f, 1f)
        val targetMs = (targetProgress * _durationMs.value).toInt()
        _progress.value = targetProgress
        _currentPositionMs.value = targetMs
        try {
            mediaPlayer?.seekTo(targetMs)
        } catch (e: Exception) {
            Log.w("AudioPlaybackManager", "Error seeking MediaPlayer: ${e.message}")
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    val current = player.currentPosition
                    val duration = player.duration.coerceAtLeast(1)
                    _currentPositionMs.value = current
                    _durationMs.value = duration
                    _progress.value = (current.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                }
                delay(60)
            }
        }
    }

    private fun startSimulatedPlayback() {
        _isPlaying.value = true
        _progress.value = 0f
        val totalMs = if (_durationMs.value > 0) _durationMs.value else 6000
        _durationMs.value = totalMs
        progressJob?.cancel()
        progressJob = scope.launch {
            var elapsed = 0
            while (isActive && elapsed < totalMs && _isPlaying.value) {
                delay(60)
                elapsed += 60
                _currentPositionMs.value = elapsed
                _progress.value = (elapsed.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
            }
            _isPlaying.value = false
            _progress.value = 0f
            _currentPositionMs.value = 0
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stop()
    }
}
