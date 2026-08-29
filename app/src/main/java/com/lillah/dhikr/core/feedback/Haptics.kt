package com.lillah.dhikr.core.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Deliberately restrained. A tap gets the shortest pulse the hardware can produce; only round
 * completions and unlocked milestones get a pattern. Every call is a no-op when the user has
 * turned haptics off, so callers never have to branch.
 */
class Haptics(context: Context) {

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    private val hasVibrator: Boolean = vibrator?.hasVibrator() == true

    fun tick(enabled: Boolean = true) {
        if (!enabled) return
        oneShot(durationMillis = 12, amplitude = 60)
    }

    fun roundComplete(enabled: Boolean = true) {
        if (!enabled) return
        pattern(longArrayOf(0, 22, 60, 34), intArrayOf(0, 140, 0, 200))
    }

    fun milestone(enabled: Boolean = true) {
        if (!enabled) return
        pattern(
            longArrayOf(0, 18, 45, 18, 45, 40),
            intArrayOf(0, 110, 0, 160, 0, 220),
        )
    }

    fun undo(enabled: Boolean = true) {
        if (!enabled) return
        oneShot(durationMillis = 18, amplitude = 45)
    }

    private fun oneShot(durationMillis: Long, amplitude: Int) {
        val device = vibrator?.takeIf { hasVibrator } ?: return
        runCatching {
            val safeAmplitude = if (device.hasAmplitudeControl()) {
                amplitude.coerceIn(1, 255)
            } else {
                VibrationEffect.DEFAULT_AMPLITUDE
            }
            device.vibrate(VibrationEffect.createOneShot(durationMillis, safeAmplitude))
        }
    }

    private fun pattern(timings: LongArray, amplitudes: IntArray) {
        val device = vibrator?.takeIf { hasVibrator } ?: return
        runCatching {
            val effect = if (device.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            } else {
                VibrationEffect.createWaveform(timings, -1)
            }
            device.vibrate(effect)
        }
    }
}
