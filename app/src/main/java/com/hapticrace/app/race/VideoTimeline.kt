package com.hapticrace.app.race

import com.hapticrace.app.haptic.HapticEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the collection and manipulation of haptic events for a video.
 */
class VideoTimeline(initialEvents: List<HapticEvent> = emptyList()) {

    private val _eventsFlow = MutableStateFlow(initialEvents.sortedBy { it.timestampMs })
    val eventsFlow: StateFlow<List<HapticEvent>> = _eventsFlow.asStateFlow()

    val events: List<HapticEvent>
        get() = _eventsFlow.value

    fun setEvents(newEvents: List<HapticEvent>) {
        _eventsFlow.value = newEvents.sortedBy { it.timestampMs }
    }

    fun addEvent(event: HapticEvent) {
        val current = _eventsFlow.value.toMutableList()
        current.add(event)
        _eventsFlow.value = current.sortedBy { it.timestampMs }
    }

    fun removeEvent(id: String) {
        val current = _eventsFlow.value.toMutableList()
        current.removeAll { it.id == id }
        _eventsFlow.value = current
    }

    fun updateEvent(updated: HapticEvent) {
        val current = _eventsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            current[index] = updated
            _eventsFlow.value = current.sortedBy { it.timestampMs }
        }
    }

    fun moveEvent(id: String, newTimestampMs: Long) {
        val current = _eventsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = current[index]
            current[index] = existing.copy(timestampMs = newTimestampMs.coerceAtLeast(0L))
            _eventsFlow.value = current.sortedBy { it.timestampMs }
        }
    }

    fun getEventById(id: String): HapticEvent? {
        return _eventsFlow.value.find { it.id == id }
    }

    fun getEventsInRange(startMs: Long, endMs: Long): List<HapticEvent> {
        return _eventsFlow.value.filter { it.timestampMs in startMs..endMs }
    }
}
