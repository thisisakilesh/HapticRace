package com.hapticrace.app.race

import android.util.Log
import com.hapticrace.app.haptic.HapticEvent
import com.hapticrace.app.haptic.HapticManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Handles high-precision synchronization between ExoPlayer playback position
 * and the haptic timeline, avoiding duplicate triggers, handling seeks properly,
 * and dispatching telemetry signals for UI HUD effects.
 */
class PlaybackSyncManager(
    private val hapticManager: HapticManager,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val tag = "PlaybackSync"

    // Set of event IDs that have already been triggered in the current forward playback stream
    private val triggeredEventIds = mutableSetOf<String>()

    // Last observed player position
    private var lastPositionMs: Long = -1L

    // Thresholds to detect manual seek vs normal frame tick
    private val seekBackwardThresholdMs = 200L
    private val seekForwardThresholdMs = 400L

    // Active haptic event for telemetry HUD visual indicator
    private val _activeHapticEvent = MutableStateFlow<HapticEvent?>(null)
    val activeHapticEvent: StateFlow<HapticEvent?> = _activeHapticEvent.asStateFlow()

    private var activeClearJob: Job? = null

    /**
     * Explicitly called when user scrubs or seeks to a new timestamp.
     */
    fun onSeekTo(newPositionMs: Long, events: List<HapticEvent>) {
        val clampedNew = newPositionMs.coerceAtLeast(0L)
        val isBackward = lastPositionMs >= 0 && clampedNew < (lastPositionMs - 50L)

        if (isBackward) {
            // Remove events that are at or after new position so they can trigger again
            val toReEnable = events.filter { it.timestampMs >= clampedNew - 30L }.map { it.id }
            triggeredEventIds.removeAll(toReEnable.toSet())
            Log.d(tag, "Seek backward to ${clampedNew}ms: re-enabled ${toReEnable.size} events")
        } else {
            // Forward seek: mark all events prior to new position as consumed so they don't fire en masse
            val skipped = events.filter { it.timestampMs < clampedNew }.map { it.id }
            triggeredEventIds.addAll(skipped)
            Log.d(tag, "Seek forward to ${clampedNew}ms: suppressed ${skipped.size} skipped events")
        }

        lastPositionMs = clampedNew
        _activeHapticEvent.value = null
    }

    /**
     * Called on periodic position updates (e.g. 16ms to 33ms interval) while player is active.
     */
    fun onPlaybackPositionUpdate(
        currentPositionMs: Long,
        isPlaying: Boolean,
        events: List<HapticEvent>
    ) {
        if (!isPlaying || events.isEmpty()) {
            lastPositionMs = currentPositionMs
            return
        }

        // Initialize last position on first tick
        if (lastPositionMs < 0L) {
            lastPositionMs = currentPositionMs
            // Mark any past events if started midway
            val past = events.filter { it.timestampMs < currentPositionMs - 80L }.map { it.id }
            triggeredEventIds.addAll(past)
            return
        }

        // Check for sudden jumps (e.g. user seeked via native player controls or double tapped)
        val delta = currentPositionMs - lastPositionMs

        if (delta < -seekBackwardThresholdMs) {
            // User seeked backward
            Log.d(tag, "Auto-detected backward seek: delta=$delta ms, from $lastPositionMs to $currentPositionMs")
            onSeekTo(currentPositionMs, events)
            return
        } else if (delta > seekForwardThresholdMs) {
            // User jumped forward
            Log.d(tag, "Auto-detected forward seek: delta=$delta ms, from $lastPositionMs to $currentPositionMs")
            onSeekTo(currentPositionMs, events)
            return
        }

        // Normal continuous playback: find events falling in the window (lastPositionMs .. currentPositionMs]
        // Window includes small padding for clock discretization
        val windowStart = lastPositionMs
        val windowEnd = currentPositionMs

        for (event in events) {
            val ts = event.timestampMs
            if (ts in (windowStart + 1)..windowEnd || (windowStart == windowEnd && ts == currentPositionMs)) {
                if (!triggeredEventIds.contains(event.id)) {
                    triggeredEventIds.add(event.id)
                    fireHaptic(event)
                }
            }
        }

        lastPositionMs = currentPositionMs
    }

    private fun fireHaptic(event: HapticEvent) {
        Log.d(tag, "FIRE HAPTIC -> [${event.type}] at ${event.timestampMs}ms (label=${event.label})")
        hapticManager.triggerEvent(event)

        // Telemetry HUD flash
        _activeHapticEvent.value = event
        activeClearJob?.cancel()
        activeClearJob = coroutineScope.launch {
            delay(event.durationMs.coerceAtLeast(200L))
            if (_activeHapticEvent.value?.id == event.id) {
                _activeHapticEvent.value = null
            }
        }
    }

    /**
     * Resets state when playback is reloaded or stopped.
     */
    fun reset() {
        triggeredEventIds.clear()
        lastPositionMs = -1L
        _activeHapticEvent.value = null
        activeClearJob?.cancel()
        hapticManager.cancelAll()
    }
}
