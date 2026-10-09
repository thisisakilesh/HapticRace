package com.hapticrace.app.haptic

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a single haptic feedback event placed on the video timeline.
 */
@Serializable
data class HapticEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long,
    val type: HapticEventType,
    val intensity: HapticIntensity = type.defaultIntensity,
    val durationMs: Long = type.defaultDurationMs,
    val label: String = type.title
) {
    /**
     * Helper to format timestamp as MM:SS.mmm
     */
    fun formattedTimestamp(): String {
        val totalSeconds = timestampMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val millis = timestampMs % 1000
        return String.format("%02d:%02d.%03d", minutes, seconds, millis)
    }

    /**
     * Compact timestamp format MM:SS.s
     */
    fun formattedTimestampShort(): String {
        val totalSeconds = timestampMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val tenths = (timestampMs % 1000) / 100
        return String.format("%02d:%02d.%d", minutes, seconds, tenths)
    }
}
