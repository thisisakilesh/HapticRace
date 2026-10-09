package com.hapticrace.app.haptic

import kotlinx.serialization.Serializable

/**
 * Vibration intensity levels for haptic presets and custom events.
 */
@Serializable
enum class HapticIntensity(
    val displayName: String,
    val amplitudeScale: Float,
    val defaultAmplitude: Int
) {
    LOW(
        displayName = "LOW",
        amplitudeScale = 0.5f,
        defaultAmplitude = 80
    ),
    MEDIUM(
        displayName = "MEDIUM",
        amplitudeScale = 0.8f,
        defaultAmplitude = 170
    ),
    HIGH(
        displayName = "HIGH",
        amplitudeScale = 1.0f,
        defaultAmplitude = 255
    );

    /**
     * Scales an amplitude value (1..255) according to this intensity setting.
     */
    fun scaleAmplitude(baseAmplitude: Int): Int {
        val scaled = (baseAmplitude * amplitudeScale).toInt()
        return scaled.coerceIn(1, 255)
    }
}
