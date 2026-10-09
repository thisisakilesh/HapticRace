package com.hapticrace.app.haptic

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.Vibrator
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dedicated engine managing hardware vibration feedback, safety throttling,
 * device capability detection, and global intensity scaling.
 */
class HapticManager(private val context: Context) {

    private val tag = "HapticManager"
    private val vibrator: Vibrator? = HapticCapabilities.getSystemVibrator(context)
    val capabilities: HapticCapabilities = HapticCapabilities.detect(context)

    // Master toggles & settings
    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _globalIntensity = MutableStateFlow(HapticIntensity.MEDIUM)
    val globalIntensity: StateFlow<HapticIntensity> = _globalIntensity.asStateFlow()

    // Last trigger timestamp to throttle hardware vibration calls and protect motor
    private var lastTriggerTimeMs: Long = 0L
    private val minThrottleIntervalMs: Long = 35L

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        if (!enabled) {
            cancelAll()
        }
    }

    fun setGlobalIntensity(intensity: HapticIntensity) {
        _globalIntensity.value = intensity
    }

    /**
     * Triggers a haptic event.
     * Thread-safe, rate-limited, and hardware-safe.
     */
    fun triggerEvent(event: HapticEvent) {
        if (!_isEnabled.value) return
        if (!capabilities.hasVibrator || vibrator == null) {
            Log.d(tag, "Vibration requested for ${event.type.name} but hardware vibrator not available")
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastTriggerTimeMs < minThrottleIntervalMs) {
            Log.d(tag, "Vibration throttled to protect hardware (${now - lastTriggerTimeMs}ms since last)")
            return
        }
        lastTriggerTimeMs = now

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = HapticPattern.createVibrationEffect(
                    event = event,
                    globalIntensity = _globalIntensity.value,
                    hasAmplitudeControl = capabilities.hasAmplitudeControl
                )
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val pattern = HapticPattern.getLegacyPattern(event.type, event.durationMs)
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to execute vibration effect for ${event.type.name}: ${e.message}")
        }
    }

    /**
     * Preview an event (e.g., when editing or selecting a preset)
     */
    fun previewEvent(type: HapticEventType, intensity: HapticIntensity = HapticIntensity.MEDIUM, durationMs: Long = type.defaultDurationMs) {
        val dummyEvent = HapticEvent(
            timestampMs = 0L,
            type = type,
            intensity = intensity,
            durationMs = durationMs
        )
        triggerEvent(dummyEvent)
    }

    /**
     * Immediately stops any ongoing vibration.
     */
    fun cancelAll() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(tag, "Error cancelling vibration: ${e.message}")
        }
    }
}
