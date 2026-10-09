package com.hapticrace.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.hapticrace.app.haptic.HapticEvent
import com.hapticrace.app.race.TimelineEvent
import com.hapticrace.app.ui.theme.DarkBorder
import com.hapticrace.app.ui.theme.DarkCarbon
import com.hapticrace.app.ui.theme.DarkSurface
import com.hapticrace.app.ui.theme.HyperCyan
import kotlin.math.abs

/**
 * Interactive horizontal timeline scrubber displaying event markers,
 * current playhead position, time graduations, and drag/tap to seek or select.
 */
@Composable
fun TimelineScrubber(
    currentPositionMs: Long,
    durationMs: Long,
    events: List<HapticEvent>,
    selectedEventId: String?,
    onSeek: (Long) -> Unit,
    onSelectEvent: (HapticEvent?) -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveDuration = durationMs.coerceAtLeast(1000L)
    val trackHeight = 48.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCarbon)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .pointerInput(effectiveDuration, events) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    if (width > 0) {
                        val fraction = (offset.x / width).coerceIn(0f, 1f)
                        val tappedTimeMs = (fraction * effectiveDuration).toLong()

                        // Check if tapped directly on an existing event marker (within 24px tolerance)
                        val hitEvent = events.minByOrNull { event ->
                            val eventX = (event.timestampMs.toFloat() / effectiveDuration) * width
                            abs(eventX - offset.x)
                        }

                        if (hitEvent != null) {
                            val eventX = (hitEvent.timestampMs.toFloat() / effectiveDuration) * width
                            if (abs(eventX - offset.x) < 28f) {
                                onSelectEvent(hitEvent)
                                onSeek(hitEvent.timestampMs)
                                return@detectTapGestures
                            }
                        }

                        // Otherwise seek to position
                        onSeek(tappedTimeMs)
                        onSelectEvent(null)
                    }
                }
            }
            .pointerInput(effectiveDuration) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val width = size.width.toFloat()
                        if (width > 0) {
                            val fraction = (offset.x / width).coerceIn(0f, 1f)
                            onSeek((fraction * effectiveDuration).toLong())
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val width = size.width.toFloat()
                        if (width > 0) {
                            val fraction = (change.position.x / width).coerceIn(0f, 1f)
                            onSeek((fraction * effectiveDuration).toLong())
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height

            // 1. Background Grid / Graduation lines
            val divisionCount = 10
            for (i in 0..divisionCount) {
                val x = (i.toFloat() / divisionCount) * width
                drawLine(
                    color = DarkBorder.copy(alpha = 0.5f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
            }

            // 2. Playback progress fill
            val progressFraction = (currentPositionMs.toFloat() / effectiveDuration).coerceIn(0f, 1f)
            drawRect(
                color = HyperCyan.copy(alpha = 0.12f),
                topLeft = Offset(0f, 0f),
                size = Size(progressFraction * width, height)
            )

            // 3. Center line
            drawLine(
                color = DarkBorder,
                start = Offset(0f, height / 2f),
                end = Offset(width, height / 2f),
                strokeWidth = 1.5f
            )

            // 4. Render Event Markers
            for (event in events) {
                val eventFraction = (event.timestampMs.toFloat() / effectiveDuration).coerceIn(0f, 1f)
                val markerX = eventFraction * width
                val eventColor = TimelineEvent.getColorForType(event.type)
                val isSelected = event.id == selectedEventId

                // Vertical event marker tick
                drawLine(
                    color = eventColor,
                    start = Offset(markerX, 6f),
                    end = Offset(markerX, height - 6f),
                    strokeWidth = if (isSelected) 3.5f else 2f
                )

                // Event marker diamond / pin head
                val pinRadius = if (isSelected) 7.5f else 5.5f
                drawCircle(
                    color = eventColor,
                    radius = pinRadius,
                    center = Offset(markerX, height / 2f)
                )

                if (isSelected) {
                    // Selection halo
                    drawCircle(
                        color = Color.White,
                        radius = 2.5f,
                        center = Offset(markerX, height / 2f)
                    )
                }
            }

            // 5. Render Current Playhead (Vertical Neon Cyan cursor)
            val playheadX = progressFraction * width
            drawLine(
                color = HyperCyan,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, height),
                strokeWidth = 3f
            )

            // Playhead triangle pointer at the top
            drawCircle(
                color = HyperCyan,
                radius = 5f,
                center = Offset(playheadX, 5f)
            )
            drawCircle(
                color = Color.White,
                radius = 2f,
                center = Offset(playheadX, 5f)
            )
        }
    }
}
