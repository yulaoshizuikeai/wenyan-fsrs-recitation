package com.ancient.wenyan.ui.sound

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import com.ancient.wenyan.R

/**
 * Studio-Grade Open-Source (CC0) Audio Engine for Classical Chinese Recitation.
 *
 * Utilizes preloaded uncompressed WAV audio buffers in Android's native SoundPool
 * for zero-latency, lag-free acoustic feedback:
 * - playCorrect(): Crisp uplifting confirmation chime (Kenney confirmation_001)
 * - playEasy(): Crystal multi-tone celebration chime (Kenney confirmation_002)
 * - playWrong(): Gentle, non-punitive UI error tap (Kenney error_001)
 * - playHard(): Low, muffled wooden feedback (Kenney error_004)
 * - playFlip(): Clean tactile pluck/snap on card flip (Kenney pluck_001)
 * - playClick(): Sub-10ms ultra-crisp tactile click (Kenney click_001)
 * - playClozeReveal(): Satisfying water-drop bubble pop (Kenney drop_001)
 * - playCelebration(): Triumphant lesson completion fanfare (Kenney maximize_001)
 * - playStreakIgnite(): Resonant kindling chime on streak lighting (Kenney maximize_007)
 */
class SoundEffectManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("wenyan_sound_prefs", Context.MODE_PRIVATE)

    private val soundPool: SoundPool
    private val soundIds = mutableMapOf<SoundType, Int>()
    @Volatile
    private var isLoaded = false

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean("sound_effects_enabled", true)
        set(value) {
            prefs.edit().putBoolean("sound_effects_enabled", value).apply()
        }

    enum class SoundType {
        CORRECT,
        EASY,
        WRONG,
        HARD,
        FLIP,
        CLICK,
        CLOZE_REVEAL,
        CELEBRATION,
        STREAK_IGNITE
    }

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                isLoaded = true
            }
        }

        // Direct, zero-latency pre-loading of studio CC0 audio from app resources
        try {
            soundIds[SoundType.CORRECT] = soundPool.load(appContext, R.raw.sfx_correct, 1)
            soundIds[SoundType.EASY] = soundPool.load(appContext, R.raw.sfx_easy, 1)
            soundIds[SoundType.WRONG] = soundPool.load(appContext, R.raw.sfx_wrong, 1)
            soundIds[SoundType.HARD] = soundPool.load(appContext, R.raw.sfx_hard, 1)
            soundIds[SoundType.FLIP] = soundPool.load(appContext, R.raw.sfx_flip, 1)
            soundIds[SoundType.CLICK] = soundPool.load(appContext, R.raw.sfx_click, 1)
            soundIds[SoundType.CLOZE_REVEAL] = soundPool.load(appContext, R.raw.sfx_cloze_reveal, 1)
            soundIds[SoundType.CELEBRATION] = soundPool.load(appContext, R.raw.sfx_celebration, 1)
            soundIds[SoundType.STREAK_IGNITE] = soundPool.load(appContext, R.raw.sfx_streak_ignite, 1)
            isLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun play(type: SoundType, volume: Float = 0.85f) {
        if (!isSoundEnabled) return
        val id = synchronized(soundIds) { soundIds[type] } ?: return
        soundPool.play(id, volume, volume, 1, 0, 1.0f)
    }

    fun playCorrect() = play(SoundType.CORRECT, 0.85f)
    fun playEasy() = play(SoundType.EASY, 0.90f)
    fun playWrong() = play(SoundType.WRONG, 0.75f)
    fun playHard() = play(SoundType.HARD, 0.70f)
    fun playFlip() = play(SoundType.FLIP, 0.65f)
    fun playClick() = play(SoundType.CLICK, 0.60f)
    fun playClozeReveal() = play(SoundType.CLOZE_REVEAL, 0.85f)
    fun playCelebration() = play(SoundType.CELEBRATION, 0.95f)
    fun playStreakIgnite() = play(SoundType.STREAK_IGNITE, 0.85f)

    companion object {
        @Volatile
        private var instance: SoundEffectManager? = null

        fun getInstance(context: Context): SoundEffectManager {
            return instance ?: synchronized(this) {
                instance ?: SoundEffectManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
