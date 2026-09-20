package com.ancient.wenyan.ui.sound

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Modern Tactile Haptic Feedback Engine for Classical Chinese Recitation App.
 *
 * Provides calibrated, subtle, and delightful tactile sensations matching Duolingo & iOS Taptic Engine:
 * - tapLight(): 12ms subtle micro-tick for tabs, buttons, chips
 * - cardFlip(): 18ms crisp flick when rotating the flashcard
 * - successPulse(): Harmonic double pulse for Good / Easy ratings
 * - warningThud(): Gentle muted pulse for Again / Hard ratings (informative, non-punitive)
 * - clozePop(): Bouncy micro-click when unveiling / masking cloze tokens
 * - celebrationFanfare(): Triumphant rhythmic sequence upon finishing a study set
 * - streakIgnite(): Warm tactile swell when kindling the streak flame
 */
class HapticManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wenyan_haptic_prefs", Context.MODE_PRIVATE)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isHapticEnabled: Boolean
        get() = prefs.getBoolean("haptic_feedback_enabled", true)
        set(value) {
            prefs.edit().putBoolean("haptic_feedback_enabled", value).apply()
        }

    /**
     * Micro-tick for navigation tabs, selector chips, and regular buttons
     */
    fun tapLight() {
        if (!isHapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(12)
            }
        } catch (_: Exception) {}
    }

    /**
     * Crisp tactile flick when a flashcard flips in 3D
     */
    fun cardFlip() {
        if (!isHapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(18)
            }
        } catch (_: Exception) {}
    }

    /**
     * Double tactile confirmation pulse for Good / Easy ratings
     */
    fun successPulse() {
        if (!isHapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 18, 50, 26)
                val amplitudes = intArrayOf(0, 160, 0, 220)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 18, 50, 26), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Soft, muted pulse for Again / Hard ratings without punitive harshness
     */
    fun warningThud() {
        if (!isHapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 25, 60, 20)
                val amplitudes = intArrayOf(0, 140, 0, 110)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 25, 60, 20), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Bouncy micro-click when unveiling a masked token in cloze recitation
     */
    fun clozePop() {
        if (!isHapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10)
            }
        } catch (_: Exception) {}
    }

    /**
     * Kindling pulse for streak flame activation
     */
    fun streakIgnite() {
        if (!isHapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 20, 40, 35)
                val amplitudes = intArrayOf(0, 120, 0, 200)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 20, 40, 35), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Rhythmic fanfare pattern for recitation set completion
     */
    fun celebrationFanfare() {
        if (!isHapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 35, 60, 35, 60, 45, 80, 90)
                val amplitudes = intArrayOf(0, 150, 0, 180, 0, 210, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 35, 60, 35, 60, 45, 80, 90), -1)
            }
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile
        private var instance: HapticManager? = null

        fun getInstance(context: Context): HapticManager {
            return instance ?: synchronized(this) {
                instance ?: HapticManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
