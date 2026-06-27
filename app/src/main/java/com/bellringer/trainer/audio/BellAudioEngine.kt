package com.bellringer.trainer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.bellringer.trainer.model.Leash
import com.bellringer.trainer.model.StrikeQuality
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Programmatic bell synthesis using AudioTrack in low-latency mode.
 * CLEAN = additive partials + long exponential decay (2.5 s).
 * STUCK = truncated inharmonic thud (~130 ms) without resonance.
 */
class BellAudioEngine {
    private val sampleRate = 44100

    // Fundamental frequencies (Hz): Podzvon lower → higher.
    private val podzvonF = mapOf(
        Leash.L1_10 to 196f, Leash.L2_11 to 247f,
        Leash.L3_01 to 294f, Leash.L4_02 to 392f
    )

    fun playPodzvon(leash: Leash, quality: StrikeQuality, velocity: Float) {
        val f = podzvonF[leash] ?: 262f
        playTone(listOf(f), quality, velocity)
    }

    fun playZazvon(up: Boolean, quality: StrikeQuality, velocity: Float) {
        // up → bells 1 & 4 ; down → bells 2 & 3
        val freqs = if (up) listOf(523f, 880f) else listOf(587f, 698f)
        playTone(freqs, quality, velocity)
    }

    private fun playTone(fundamentals: List<Float>, quality: StrikeQuality, velocity: Float) {
        val gain = (0.25f + velocity.coerceIn(0f, 4f) / 4f * 0.7f).coerceIn(0.1f, 1f)
        val durSec = if (quality == StrikeQuality.CLEAN) 2.5f else 0.13f
        val decay = if (quality == StrikeQuality.CLEAN) 2.2f else 30f
        val n = (sampleRate * durSec).toInt()
        val buf = ShortArray(n)

        // Bell partials (inharmonic) relative to fundamental.
        val partials = if (quality == StrikeQuality.CLEAN)
            listOf(0.5f to 0.2f, 1f to 1f, 2f to 0.6f, 2.7f to 0.4f, 4.2f to 0.25f)
        else
            listOf(1f to 1f, 2.4f to 0.5f) // dull, fewer partials

        for (i in 0 until n) {
            val t = i / sampleRate.toFloat()
            val env = exp(-decay * t)
            var s = 0f
            for (f0 in fundamentals) for ((mult, amp) in partials) {
                s += amp * sin(2.0 * PI * f0 * mult * t).toFloat()
            }
            s = s / (fundamentals.size * partials.size) * env * gain
            buf[i] = (s * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
        }
        playBuffer(buf)
    }

    private fun playBuffer(buf: ShortArray) {
        thread(isDaemon = true) {
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val fmt = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val track = AudioTrack(
                attr, fmt, buf.size * 2,
                AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            track.write(buf, 0, buf.size)
            track.play()
            // release after playback
            Thread.sleep((buf.size * 1000L / sampleRate) + 50)
            track.release()
        }
    }
}