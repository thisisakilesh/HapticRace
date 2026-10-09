package com.hapticrace.app.race

import androidx.compose.ui.graphics.Color
import com.hapticrace.app.haptic.HapticEvent
import com.hapticrace.app.haptic.HapticEventType

/**
 * UI helper representation of a timeline event with associated UI colors and markers.
 */
data class TimelineEvent(
    val event: HapticEvent,
    val isSelected: Boolean = false,
    val isTriggeringNow: Boolean = false
) {
    val type: HapticEventType get() = event.type
    val timestampMs: Long get() = event.timestampMs
    val id: String get() = event.id

    companion object {
        fun getColorForType(type: HapticEventType): Color {
            return when (type) {
                HapticEventType.ENGINE -> Color(0xFF00E5FF)      // Neon Cyan
                HapticEventType.GEAR_SHIFT -> Color(0xFFFF9100)  // Racing Amber
                HapticEventType.BRAKE -> Color(0xFFFF1744)       // Racing Crimson
                HapticEventType.KERB -> Color(0xFFFFEA00)        // Curbside Yellow
                HapticEventType.IMPACT -> Color(0xFFE040FB)      // Shock Magenta
                HapticEventType.CUSTOM -> Color(0xFF00E676)      // Telemetry Green
            }
        }
    }
}
