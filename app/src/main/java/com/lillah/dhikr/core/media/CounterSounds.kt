package com.lillah.dhikr.core.media

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Feedback tones are synthesised at startup rather than shipped as audio files. It keeps the APK
 * free of binary assets, guarantees the sounds work with no network or storage, and lets the
 * timbre be tuned in code.
 *
 * Every AudioTrack call is wrapped: a device that refuses to allocate a track should cost the
 * user a sound, never a crash.
 */
class CounterSounds {

    private val tick: AudioTrack? by lazy { buildTrack(renderTick()) }
    private val chime: AudioTrack? by lazy { buildTrack(renderChime()) }

    fun playTick(enabled: Boolean = true) {
        if (enabled) play(tick)
    }

    fun playChime(enabled: Boolean = true) {
        if (enabled) play(chime)
    }

    fun release() {
        runCatching { tick?.release() }
        runCatching { chime?.release() }
    }

    private fun play(track: AudioTrack?) {
        val t = track ?: return
        runCatching {
            if (t.playState != AudioTrack.PLAYSTATE_STOPPED) t.stop()
            t.reloadStaticData()
            t.play()
        }
    }

    /** A short, soft wooden knock: two decaying partials, no attack transient to speak of. */
    private fun renderTick(): ShortArray {
        val durationSeconds = 0.055
        val count = (SAMPLE_RATE * durationSeconds).toInt()
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t * 95.0)
            val body = sin(2 * PI * 780.0 * t) * 0.62
            val overtone = sin(2 * PI * 1_560.0 * t) * 0.22
            val value = (body + overtone) * envelope * 0.42
            samples[i] = (value * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        applyFade(samples)
        return samples
    }

    /** Three rising notes for a completed round — brief, warm, and easy to hear only once. */
    private fun renderChime(): ShortArray {
        val notes = doubleArrayOf(783.99, 1046.50, 1318.51)
        val noteSeconds = 0.20
        val spacingSeconds = 0.11
        val totalSeconds = spacingSeconds * (notes.size - 1) + noteSeconds
        val count = (SAMPLE_RATE * totalSeconds).toInt()
        val samples = ShortArray(count)

        notes.forEachIndexed { index, frequency ->
            val offset = (SAMPLE_RATE * spacingSeconds * index).toInt()
            val noteCount = (SAMPLE_RATE * noteSeconds).toInt()
            for (i in 0 until noteCount) {
                val position = offset + i
                if (position >= count) break
                val t = i.toDouble() / SAMPLE_RATE
                val attack = (1 - exp(-t * 220.0))
                val envelope = attack * exp(-t * 9.0)
                val value = sin(2 * PI * frequency * t) * envelope * 0.24
                val mixed = samples[position] + (value * Short.MAX_VALUE).toInt()
                samples[position] = mixed
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        applyFade(samples)
        return samples
    }

    /** Zero the last few milliseconds so the track cannot end on a click. */
    private fun applyFade(samples: ShortArray) {
        val fade = minOf(220, samples.size)
        for (i in 0 until fade) {
            val index = samples.size - fade + i
            val gain = 1f - (i.toFloat() / fade)
            samples[index] = (samples[index] * gain).toInt().toShort()
        }
    }

    private fun buildTrack(samples: ShortArray): AudioTrack? = runCatching {
        val sizeInBytes = samples.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(sizeInBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        track
    }.getOrNull()

    private companion object {
        const val SAMPLE_RATE = 44_100
    }
}
