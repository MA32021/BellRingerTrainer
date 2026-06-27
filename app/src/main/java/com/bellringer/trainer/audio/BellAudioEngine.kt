package com.bellringer.trainer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.bellringer.trainer.model.Leash
import com.bellringer.trainer.model.StrikeQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "BellAudioEngine"

class BellAudioEngine(private val context: Context) {

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<String, Int>()
    private var isLoaded = false

    suspend fun initialize(bellFiles: List<String>) = withContext(Dispatchers.IO) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()

        bellFiles.forEach { fileName ->
            try {
                val fd = context.assets.openFd("bells/$fileName")
                val id = soundPool?.load(fd, 1) ?: -1
                val key = fileName.substringBeforeLast(".")
                soundMap[key] = id
                fd.close()
                Log.d(TAG, "Loaded: $key -> $id")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open asset: bells/$fileName", e)
            }
        }
        isLoaded = true
    }

    /**
     * Адаптированный API под ваш существующий вызов из AppViewModel.onEvent()
     */
    fun playPodzvon(leash: Leash, quality: StrikeQuality, velocity: Float) {
        val key = "podzvon_${leash.name.lowercase()}"
        strike(key, quality, velocity)
    }

    fun playZazvon(up: Boolean, quality: StrikeQuality, velocity: Float) {
        val key = if (up) "zazvon_up" else "zazvon_down"
        strike(key, quality, velocity)
    }

    private fun strike(key: String, quality: StrikeQuality, velocity: Float) {
        if (!isLoaded || soundPool == null) return

        val soundId = soundMap[key]
        if (soundId == null || soundId == -1) {
            Log.w(TAG, "Sound not found: $key")
            return
        }

        val volume = (0.3f + 0.7f * velocity.coerceIn(0f, 4f) / 4f).coerceIn(0.1f, 1f)
        // STUCK = короткий глухой звук, CLEAN = полный резонанс
        val rate = if (quality == StrikeQuality.STUCK) 0.7f else 1.0f

        soundPool?.play(soundId, volume, volume, 1, 0, rate)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
        isLoaded = false
    }
}
