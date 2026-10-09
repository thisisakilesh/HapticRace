package com.hapticrace.app.haptic

import android.os.Build
import android.os.VibrationEffect
import androidx.annotation.RequiresApi

/**
 * Encapsulates timing arrays, amplitude arrays, and VibrationEffect creation
 * for each racing haptic preset.
 */
object HapticPattern {

    /**
     * Builds an Android VibrationEffect for the given event and global intensity setting.
     * Takes amplitude control capability into account.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createVibrationEffect(
        event: HapticEvent,
        globalIntensity: HapticIntensity,
        hasAmplitudeControl: Boolean
    ): VibrationEffect {
        // Effective intensity: scale event intensity by global intensity
        val combinedScale = (event.intensity.amplitudeScale * globalIntensity.amplitudeScale).coerceIn(0.1f, 1.0f)

        return when (event.type) {
            HapticEventType.ENGINE -> {
                // Subtle engine rumble pulses
                val baseAmp = (65 * combinedScale).toInt().coerceIn(1, 255)
                val timings = longArrayOf(0, 45, 25, 45, 25, 45)
                val amplitudes = intArrayOf(0, baseAmp, 0, (baseAmp * 0.8f).toInt().coerceAtLeast(1), 0, (baseAmp * 0.6f).toInt().coerceAtLeast(1))
                buildWaveform(timings, amplitudes, hasAmplitudeControl)
            }

            HapticEventType.GEAR_SHIFT -> {
                // Short snappy punch
                val baseAmp = (255 * combinedScale).toInt().coerceIn(1, 255)
                val duration = event.durationMs.coerceIn(20L, 80L)
                if (hasAmplitudeControl) {
                    VibrationEffect.createOneShot(duration, baseAmp)
                } else {
                    VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
                }
            }

            HapticEventType.BRAKE -> {
                // Firm deceleration drag
                val baseAmp = (190 * combinedScale).toInt().coerceIn(1, 255)
                val duration = event.durationMs.coerceIn(80L, 350L)
                if (hasAmplitudeControl) {
                    // Two-phase decel ramp
                    val timings = longArrayOf(0, duration / 2, duration / 2)
                    val amplitudes = intArrayOf(0, (baseAmp * 0.7f).toInt().coerceAtLeast(1), baseAmp)
                    buildWaveform(timings, amplitudes, true)
                } else {
                    VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
                }
            }

            HapticEventType.KERB -> {
                // Rapid flutter vibration simulating track rumble strip / kerb
                val baseAmp = (175 * combinedScale).toInt().coerceIn(1, 255)
                val timings = longArrayOf(0, 28, 22, 28, 22, 28, 22, 28)
                val amplitudes = intArrayOf(0, baseAmp, 0, (baseAmp * 0.9f).toInt().coerceAtLeast(1), 0, baseAmp, 0, (baseAmp * 0.85f).toInt().coerceAtLeast(1))
                buildWaveform(timings, amplitudes, hasAmplitudeControl)
            }

            HapticEventType.IMPACT -> {
                // Violent primary collision + aftershock pulses
                val primaryAmp = (255 * combinedScale).toInt().coerceIn(1, 255)
                val secondaryAmp = (180 * combinedScale).toInt().coerceIn(1, 255)
                val timings = longArrayOf(0, 90, 35, 120, 30, 60)
                val amplitudes = intArrayOf(0, primaryAmp, 0, secondaryAmp, 0, (secondaryAmp * 0.6f).toInt().coerceAtLeast(1))
                buildWaveform(timings, amplitudes, hasAmplitudeControl)
            }

            HapticEventType.CUSTOM -> {
                // User-configured duration and intensity
                val baseAmp = (event.intensity.defaultAmplitude * globalIntensity.amplitudeScale).toInt().coerceIn(1, 255)
                val duration = event.durationMs.coerceIn(20L, 1000L)
                if (hasAmplitudeControl) {
                    VibrationEffect.createOneShot(duration, baseAmp)
                } else {
                    VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
                }
            }
        }
    }

    /**
     * Builds waveform with amplitudes if supported, otherwise timings-only fallback.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildWaveform(
        timings: LongArray,
        amplitudes: IntArray,
        hasAmplitudeControl: Boolean
    ): VibrationEffect {
        return if (hasAmplitudeControl) {
            try {
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            } catch (_: Exception) {
                // Fallback to timing pattern if device driver fails
                VibrationEffect.createWaveform(timings, -1)
            }
        } else {
            VibrationEffect.createWaveform(timings, -1)
        }
    }

    /**
     * Legacy timing array for pre-Oreo devices or basic fallback
     */
    fun getLegacyPattern(event: HapticEventType, durationMs: Long): LongArray {
        return when (event) {
            HapticEventType.ENGINE -> longArrayOf(0, 40, 30, 40, 30, 40)
            HapticEventType.GEAR_SHIFT -> longArrayOf(0, 45)
            HapticEventType.BRAKE -> longArrayOf(0, durationMs.coerceAtLeast(120L))
            HapticEventType.KERB -> longArrayOf(0, 25, 20, 25, 20, 25, 20, 25)
            HapticEventType.IMPACT -> longArrayOf(0, 90, 40, 110, 30, 50)
            HapticEventType.CUSTOM -> longArrayOf(0, durationMs)
        }
    }
}
