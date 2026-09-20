package com.ancient.wenyan.ui.sound

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Studio-Grade Synthesized Sound Effects Engine for Classical Chinese Recitation.
 *
 * Employs physical multi-harmonic synthesis with calibrated ADSR envelopes and organic overtones:
 * - playCorrect(): Uplifting double harmonic chime (C6 -> G6)
 * - playEasy(): Crystalline ascending major triad chime (C6 -> E6 -> G6)
 * - playWrong(): Warm, non-punitive marimba mallet strike (220Hz + 440Hz warm overtones)
 * - playHard(): Dual soft wooden knocks (260Hz -> 220Hz)
 * - playFlip(): Organic tactile paper card whoosh
 * - playClick(): Ultra-crisp sub-5ms tactile micro-switch
 * - playClozeReveal(): Playful water-drop bubble pop when revealing a masked blank
 * - playCelebration(): 4-part orchestral fanfare on set mastery
 * - playStreakIgnite(): Warm kindling chime on streak lighting
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

        // Asynchronously synthesize and load PCM audio to keep UI completely thread-safe
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cacheDir = appContext.cacheDir
                loadSynthesizedSound(cacheDir, SoundType.CORRECT, generateCorrectChime())
                loadSynthesizedSound(cacheDir, SoundType.EASY, generateEasyChime())
                loadSynthesizedSound(cacheDir, SoundType.WRONG, generateWrongTone())
                loadSynthesizedSound(cacheDir, SoundType.HARD, generateHardTone())
                loadSynthesizedSound(cacheDir, SoundType.FLIP, generateFlipWhoosh())
                loadSynthesizedSound(cacheDir, SoundType.CLICK, generateClick())
                loadSynthesizedSound(cacheDir, SoundType.CLOZE_REVEAL, generateClozeReveal())
                loadSynthesizedSound(cacheDir, SoundType.CELEBRATION, generateCelebrationFanfare())
                loadSynthesizedSound(cacheDir, SoundType.STREAK_IGNITE, generateStreakIgnite())
                isLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadSynthesizedSound(dir: File, type: SoundType, wavData: ByteArray) {
        val file = File(dir, "sfx_v2_${type.name.lowercase()}.wav")
        FileOutputStream(file).use { it.write(wavData) }
        val id = soundPool.load(file.absolutePath, 1)
        synchronized(soundIds) {
            soundIds[type] = id
        }
    }

    fun play(type: SoundType, volume: Float = 0.85f) {
        if (!isSoundEnabled || !isLoaded) return
        val id = synchronized(soundIds) { soundIds[type] } ?: return
        soundPool.play(id, volume, volume, 1, 0, 1.0f)
    }

    fun playCorrect() = play(SoundType.CORRECT, 0.85f)
    fun playEasy() = play(SoundType.EASY, 0.90f)
    fun playWrong() = play(SoundType.WRONG, 0.75f)
    fun playHard() = play(SoundType.HARD, 0.70f)
    fun playFlip() = play(SoundType.FLIP, 0.65f)
    fun playClick() = play(SoundType.CLICK, 0.55f)
    fun playClozeReveal() = play(SoundType.CLOZE_REVEAL, 0.80f)
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

        private const val SAMPLE_RATE = 44100

        private fun generateWavHeader(dataSize: Int): ByteArray {
            val totalSize = dataSize + 36
            val buffer = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            buffer.put("RIFF".toByteArray())
            buffer.putInt(totalSize)
            buffer.put("WAVE".toByteArray())
            buffer.put("fmt ".toByteArray())
            buffer.putInt(16) // PCM chunk size
            buffer.putShort(1) // PCM format
            buffer.putShort(1) // Mono
            buffer.putInt(SAMPLE_RATE)
            buffer.putInt(SAMPLE_RATE * 2) // Byte rate (16-bit mono)
            buffer.putShort(2) // Block align
            buffer.putShort(16) // Bits per sample
            buffer.put("data".toByteArray())
            buffer.putInt(dataSize)
            return buffer.array()
        }

        private fun buildWav(pcm: ShortArray): ByteArray {
            val dataSize = pcm.size * 2
            val header = generateWavHeader(dataSize)
            val byteBuffer = ByteBuffer.allocate(header.size + dataSize).order(ByteOrder.LITTLE_ENDIAN)
            byteBuffer.put(header)
            for (sample in pcm) {
                byteBuffer.putShort(sample)
            }
            return byteBuffer.array()
        }

        /**
         * Duolingo/Apple style rising harmonic bell chime (C6 -> G6) with silky smooth decay
         */
        private fun generateCorrectChime(): ByteArray {
            val duration = 0.36f
            val totalSamples = (SAMPLE_RATE * duration).toInt()
            val pcm = ShortArray(totalSamples)

            val splitPoint = (totalSamples * 0.40f).toInt()
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val sample = if (i < splitPoint) {
                    val env = exp(-t * 14.0)
                    (sin(2.0 * PI * 1046.50 * t) * 0.65 +
                     sin(2.0 * PI * 2093.00 * t) * 0.25 +
                     sin(2.0 * PI * 3139.50 * t) * 0.10) * env
                } else {
                    val t2 = (i - splitPoint).toDouble() / SAMPLE_RATE
                    val env = exp(-t2 * 8.5)
                    (sin(2.0 * PI * 1567.98 * t2) * 0.70 +
                     sin(2.0 * PI * 3135.96 * t2) * 0.22 +
                     sin(2.0 * PI * 4703.94 * t2) * 0.08) * env
                }
                pcm[i] = (sample * 23000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            return buildWav(pcm)
        }

        /**
         * Ascending 3-tone crystalline chime (C6 -> E6 -> G6) rewarding easy mastery
         */
        private fun generateEasyChime(): ByteArray {
            val duration = 0.45f
            val totalSamples = (SAMPLE_RATE * duration).toInt()
            val pcm = ShortArray(totalSamples)

            val noteDuration = totalSamples / 3
            val notes = listOf(1046.50, 1318.51, 1567.98)

            for (i in 0 until totalSamples) {
                val noteIdx = (i / noteDuration).coerceAtMost(2)
                val noteT = (i % noteDuration).toDouble() / SAMPLE_RATE
                val env = exp(-noteT * 9.0)
                val freq = notes[noteIdx]
                val sample = (sin(2.0 * PI * freq * noteT) * 0.70 +
                             sin(2.0 * PI * (freq * 2.0) * noteT) * 0.22 +
                             sin(2.0 * PI * (freq * 3.0) * noteT) * 0.08) * env
                pcm[i] = (sample * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            return buildWav(pcm)
        }

        /**
         * Warm, organic Marimba wooden tone (220Hz) - low-pass filtered, non-punitive
         */
        private fun generateWrongTone(): ByteArray {
            val duration = 0.22f
            val totalSamples = (SAMPLE_RATE * duration).toInt()
            val pcm = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * 16.0)
                // Fundamental 220Hz with soft 2nd harmonic
                val sample = (sin(2.0 * PI * 220.0 * t) * 0.82 +
                             sin(2.0 * PI * 440.0 * t) * 0.18) * env
                pcm[i] = (sample * 19000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            return buildWav(pcm)
        }

        /**
         * Dual soft wooden taps (260Hz -> 220Hz) for Hard rating
         */
        private fun generateHardTone(): ByteArray {
            val duration = 0.26f
            val totalSamples = (SAMPLE_RATE * duration).toInt()
            val pcm = ShortArray(totalSamples)

            val splitPoint = totalSamples / 2
            for (i in 0 until totalSamples) {
                val isSecond = i >= splitPoint
                val localI = if (isSecond) i - splitPoint else i
                val t = localI.toDouble() / SAMPLE_RATE
                val freq = if (isSecond) 220.0 else 260.0
                val env = exp(-t * 22.0)
                val sample = sin(2.0 * PI * freq * t) * env
                pcm[i] = (sample * 17000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            return buildWav(pcm)
        }

        /**
         * Tactile paper card flick/whoosh
         */
        private fun generateFlipWhoosh(): ByteArray {
            val duration = 0.11f
            val totalSamples = (SAMPLE_RATE * duration).toInt()
            val pcm = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val progress = i.toFloat() / totalSamples
                val env = sin(progress * PI)
                val noise = (Math.random() * 2.0 - 1.0)
                val pitchSweep = 240.0 + progress * 260.0
                val tone = sin(2.0 * PI * pitchSweep * (i.toDouble() / SAMPLE_RATE))
                val sample = (noise * 0.45 + tone * 0.55) * env
                pcm[i] = (sample * 15000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            return buildWav(pcm)
        }

        /**
         * Crisp sub-5ms mechanical switch click
         */
        private fun generateClick(): ByteArray {
            val duration = 0.025f
            val totalSamples = (SAMPLE_RATE * duration).toInt()
            val pcm = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * 110.0)
                val sample = sin(2.0 * PI * 1200.0 * t) * env
                pcm[i] = (sample * 18000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            return buildWav(pcm)
        }

        /**
         * Playful water-drop bubble chime for masked token reveal
         */
        private fun generateClozeReveal(): ByteArray {
            val duration = 0.14f
            val totalSamples = (SAMPLE_RATE * duration).toInt()
            val pcm = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toFloat() / totalSamples
                val freq = 1200.0 + progress * 600.0
                val env = exp(-t * 24.0)
                val sample = (sin(2.0 * PI * freq * t) * 0.8 +
                             sin(2.0 * PI * freq * 2.0 * t) * 0.2) * env
                pcm[i] = (sample * 21000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            return buildWav(pcm)
        }

        /**
         * Warm kindling chime on streak lighting
         */
        private fun generateStreakIgnite(): ByteArray {
            val duration = 0.32f
            val totalSamples = (SAMPLE_RATE * duration).toInt()
            val pcm = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toFloat() / totalSamples
                val env = sin(progress * PI)
                val sample = (sin(2.0 * PI * (440.0 + progress * 440.0) * t) * 0.65 +
                             sin(2.0 * PI * 1320.0 * t) * 0.35) * env
                pcm[i] = (sample * 20000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            return buildWav(pcm)
        }

        /**
         * Orchestrated 4-note major chord fanfare (C5 -> E5 -> G5 -> C6) with lingering acoustic tail
         */
        private fun generateCelebrationFanfare(): ByteArray {
            val duration = 0.68f
            val totalSamples = (SAMPLE_RATE * duration).toInt()
            val pcm = ShortArray(totalSamples)

            val freqs = listOf(523.25, 659.25, 783.99, 1046.50)
            val noteSamples = (totalSamples * 0.8f / freqs.size).toInt()

            for (i in 0 until totalSamples) {
                val noteIdx = (i / noteSamples).coerceAtMost(freqs.size - 1)
                val noteT = (i % noteSamples).toDouble() / SAMPLE_RATE
                val totalT = i.toDouble() / SAMPLE_RATE
                val env = exp(-noteT * 5.0) * exp(-totalT * 2.2)
                val f = freqs[noteIdx]
                val sample = (sin(2.0 * PI * f * totalT) * 0.65 +
                             sin(2.0 * PI * (f * 2.0) * totalT) * 0.25 +
                             sin(2.0 * PI * (f * 3.0) * totalT) * 0.10) * env
                pcm[i] = (sample * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            return buildWav(pcm)
        }
    }
}
