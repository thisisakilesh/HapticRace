package com.hapticrace.app.haptic

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Encapsulates device vibration hardware capability detection.
 */
data class HapticCapabilities(
    val hasVibrator: Boolean,
    val hasAmplitudeControl: Boolean,
    val isApi26OrHigher: Boolean,
    val isApi31OrHigher: Boolean,
    val androidVersion: Int,
    val statusText: String,
    val detailedStatus: String
) {
    companion object {
        fun detect(context: Context): HapticCapabilities {
            val vibrator = getSystemVibrator(context)
            val hasVib = vibrator?.hasVibrator() ?: false
            val hasAmp = vibrator?.hasAmplitudeControl() ?: false
            val apiLevel = Build.VERSION.SDK_INT
            val is26 = apiLevel >= Build.VERSION_CODES.O
            val is31 = apiLevel >= Build.VERSION_CODES.S

            val statusText = if (hasVib) {
                "Haptic feedback available"
            } else {
                "Haptic feedback unavailable"
            }

            val detailedStatus = when {
                !hasVib -> "No vibration hardware detected on this device. Video playback remains available."
                hasAmp -> "High-fidelity haptic feedback with continuous amplitude modulation is supported."
                else -> "Standard haptic feedback supported (amplitude scaling simulated via pulsed duty cycles)."
            }

            return HapticCapabilities(
                hasVibrator = hasVib,
                hasAmplitudeControl = hasAmp,
                isApi26OrHigher = is26,
                isApi31OrHigher = is31,
                androidVersion = apiLevel,
                statusText = statusText,
                detailedStatus = detailedStatus
            )
        }

        @Suppress("DEPRECATION")
        internal fun getSystemVibrator(context: Context): Vibrator? {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
