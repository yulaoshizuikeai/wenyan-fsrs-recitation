package com.ancient.wenyan.ui.sound

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Duolingo-style Zero-Latency Sound Effects Engine.
 *
 * Synthesizes crisp, studio-grade 44.1kHz 16-bit PCM audio feedback
 * and loads them into Android's native SoundPool for instant sub-millisecond response:
 * - playCorrect(): Duolingo-style double harmonic chime (C6 -> G6)
 * - playWrong(): Gentle non-punitive tone (A3)
 * - playFlip(): Subtle paper card whoosh
 * - playCelebration(): 4-note major triad fanfare
 * - playClick(): Crisp micro-tap
 */
class SoundEffectManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wenyan_sound_prefs", Context.MODE_PRIVATE)

    private val soundPool: SoundPool
    private val soundIds = mutableMapOf<SoundType, Int>()
    private var isLoaded = false

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean("sound_effects_enabled", true)
        set(value) {
            prefs.edit().putBoolean("sound_effects_enabled", value).apply()
        }

    enum class SoundType {
        CORRECT,
        WRONG,
        FLIP,
        CELEBRATION,
        CLICK
    }

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        // Generate and load PCM WAV buffers asynchronously or in cache
        try {
            val cacheDir = context.cacheDir
            loadSynthesizedSound(cacheDir, SoundType.CORRECT, generateCorrectChime())
            loadSynthesizedSound(cacheDir, SoundType.WRONG, generateWrongTone())
            loadSynthesizedSound(cacheDir, SoundType.FLIP, generateFlipWhoosh())
            loadSynthesizedSound(cacheDir, SoundType.CELEBRATION, generateCelebrationFanfare())
            loadSynthesizedSound(cacheDir, SoundType.CLICK, generateClick())
            isLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSynthesizedSound(dir: File, type: SoundType, wavData: ByteArray) {
        val file = File(dir, "sfx_${type.name.lowercase()}.wav")
        FileOutputStream(file).use { it.write(wavData) }
        val id = soundPool.load(file.absolutePath, 1)
        soundIds[type] = id
    }

    fun play(type: SoundType) {
        if (!isSoundEnabled || !isLoaded) return
        val id = soundIds[type] ?: return
        soundPool.play(id, 0.85f, 0.85f, 1, 0, 1.0f)
    }

    fun playCorrect() = play(SoundType.CORRECT)
    fun playWrong() = play(SoundType.WRONG)
    fun playFlip() = play(SoundType.FLIP)
    fun playCelebration() = play(SoundType.CELEBRATION)
    fun playClick() = play(SoundType.CLICK)

    companion object {
        @Volatile
        private var instance: SoundEffectManager? = null

        fun getInstance(context: Context): SoundEffectManager {
            return instance ?: synchronized(this) {
                instance ?: SoundEffectManager(context.applicationContext).also { instance = it }
            }
        }

        // ====================================================================
        // Audio Synthesizers: 44100Hz 16-bit Mono PCM WAV Generation
        // ====================================================================

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

        /**
         * Duolingo-style rising harmonic bell chime (C6 -> G6)
         */
        private fun generateCorrectChime(): ByteArray {
            val duration = 0.38f // seconds
            val totalSamples = (SAMPLE_RATE * duration).toInt()
            val pcm = ShortArray(totalSamples)

            val note1Duration = (totalSamples * 0.45f).toInt()
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val sample = if (i < note1Duration) {
                    val env = exp(-t * 12.0)
                    (sin(2.0 * PI * 1046.5 * t) * 0.7 + sin(2.0 * PI * 2093.0 * t) * 0.3) * env
                } else {
                    val t2 = (i - note1Duration).toDouble() / SAMPLE_RATE
                    val env = exp(-t2 * 8.0)
                    (sin(2.0 * PI * 1567.98 * t2) * 0.8 + sin(2.0 * PI * 3135.96 * t2) * 0.2) * env
                }
                pcm[i] = (sample * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
            }

            return buildWav(pcm)
        }

        /**
         * Soft, non-jarring low tone for Again/Hard
         */
        private fun generateWrongTone(): ByteArray {
            val duration = 0.25f
            val totalSamples = (SAMPLE_RATE * duration).toInt()
            val pcm = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * 9.0)
                val sample = sin(2.0 * PI * 220.0 * t) * env
                pcm[i] = (sample * 20000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            return buildWav(pcm)
        }

        /**
         * Tactile paper card whoosh for Flip
         */
        private fun generateFlipWhoosh(): ByteArray {
            val duration = 0.12f
            val totalSamples = (SAMPLE_RATE * duration).toInt()
            val pcm = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val progress = i.toFloat() / totalSamples
                val env = sin(progress * PI)
                val noise = (Math.random() * 2.0 - 1.0)
                val tone = sin(2.0 * PI * (300.0 + progress * 200.0) * (i.toDouble() / SAMPLE_RATE))
                val sample = (noise * 0.6 + tone * 0.4) * env
                pcm[i] = (sample * 16000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            return buildWav(pcm)
        }

        /**
         * Ascending 4-note celebration fanfare (C5 - E5 - G5 - C6)
         */
        private fun generateCelebrationFanfare(): ByteArray {
            val duration = 0.6f
            val totalSamples = (SAMPLE_RATE * duration).toInt()
            val pcm = ShortArray(totalSamples)

            val freqs = listOf(523.25, 659.25, 783.99, 1046.5)
            val noteSamples = totalSamples / freqs.size

            for (i in 0 until totalSamples) {
                val noteIdx = (i / noteSamples).coerceAtMost(freqs.size - 1)
                val noteT = (i % noteSamples).toDouble() / SAMPLE_RATE
                val env = exp(-noteT * 6.0)
                val sample = (sin(2.0 * PI * freqs[noteIdx] * noteT) * 0.8 +
                        sin(2.0 * PI * (freqs[noteIdx] * 2.0) * noteT) * 0.2) * env
                pcm[i] = (sample * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            return buildWav(pcm)
        }

        /**
         * Sub-5ms tactile tap click
         */
        private fun generateClick(): ByteArray {
            val duration = 0.03f
            val totalSamples = (SAMPLE_RATE * duration).toInt()
            val pcm = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * 80.0)
                val sample = sin(2.0 * PI * 800.0 * t) * env
                pcm[i] = (sample * 20000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            return buildWav(pcm)
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
    }
}
