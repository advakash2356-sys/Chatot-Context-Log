package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * HapticFeedbackManager provides fine-grained, tactile vibrational signatures across the app.
 * Provides distinct tactile pulses for:
 * - Recording Start: Crisp rising click/pulse confirming mic is active.
 * - Recording Stop: Heavy mechanical release pulse confirming capture completion.
 * - Segment Processed: Micro-tick informing user of incoming speech recognition without screen gaze.
 * - Selection/Touch: Snappy tactile feedback for buttons and chips.
 * - Error/Alert: Distinct double-buzz pattern for interruptions or focus loss.
 */
class HapticFeedbackManager(private val context: Context) {

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        Log.w(TAG, "Unable to initialize vibrator service: ${e.message}")
        null
    }

    /**
     * Trigger a distinct, crisp start-of-recording double pulse / strong click.
     * Tells the user tactilely that the microphone is hot and listening.
     */
    fun triggerRecordingStart() {
        try {
            vibrator?.let { v ->
                if (!v.hasVibrator()) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 30, 35, 55),
                            intArrayOf(0, 180, 0, 255),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(longArrayOf(0, 30, 35, 55), -1)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Haptics start pulse error: ${e.message}")
        }
    }

    /**
     * Trigger a distinct, heavy mechanical release pulse when recording ends.
     * Confirms the capture has completed and processing has started.
     */
    fun triggerRecordingStop() {
        try {
            vibrator?.let { v ->
                if (!v.hasVibrator()) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 45, 25, 20),
                            intArrayOf(0, 240, 0, 110),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(longArrayOf(0, 45, 25, 20), -1)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Haptics stop pulse error: ${e.message}")
        }
    }

    /**
     * Trigger a subtle micro-tick when a transcription segment or partial text chunk is processed.
     * Gives real-time tactile cadence without being intrusive.
     */
    fun triggerSegmentProcessed() {
        try {
            vibrator?.let { v ->
                if (!v.hasVibrator()) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(12, 90))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(12)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Haptics segment pulse error: ${e.message}")
        }
    }

    /**
     * Trigger a fast tactile click for UI buttons, language chips, or tabs.
     */
    fun triggerSelection() {
        try {
            vibrator?.let { v ->
                if (!v.hasVibrator()) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(8, 70))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(8)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Haptics selection error: ${e.message}")
        }
    }

    /**
     * Trigger a double-buzz pulse for error or audio focus loss.
     */
    fun triggerError() {
        try {
            vibrator?.let { v ->
                if (!v.hasVibrator()) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 70, 70, 70),
                            intArrayOf(0, 255, 0, 255),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(longArrayOf(0, 70, 70, 70), -1)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Haptics error pulse error: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "HapticFeedbackManager"

        @Volatile
        private var INSTANCE: HapticFeedbackManager? = null

        fun getInstance(context: Context): HapticFeedbackManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HapticFeedbackManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
