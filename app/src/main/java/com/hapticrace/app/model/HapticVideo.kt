package com.hapticrace.app.model

import com.hapticrace.app.haptic.HapticEvent
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Data model representing a video project with its synchronized haptic timeline.
 */
@Serializable
data class HapticVideo(
    val id: String = UUID.randomUUID().toString(),
    val videoUri: String,
    val videoName: String,
    val durationMs: Long,
    val thumbnailUri: String? = null,
    val events: List<HapticEvent> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val isDemo: Boolean = false
) {
    /**
     * Formats duration in MM:SS
     */
    fun formattedDuration(): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Returns events sorted by timestamp
     */
    val sortedEvents: List<HapticEvent>
        get() = events.sortedBy { it.timestampMs }
}
