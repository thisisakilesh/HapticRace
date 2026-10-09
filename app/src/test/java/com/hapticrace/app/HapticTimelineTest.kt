package com.hapticrace.app

import com.hapticrace.app.haptic.HapticEvent
import com.hapticrace.app.haptic.HapticEventType
import com.hapticrace.app.haptic.HapticIntensity
import com.hapticrace.app.haptic.HapticPattern
import com.hapticrace.app.race.VideoTimeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticTimelineTest {

    @Test
    fun testEventCreationAndFormatting() {
        val event = HapticEvent(
            timestampMs = 4200L,
            type = HapticEventType.ENGINE,
            intensity = HapticIntensity.LOW,
            durationMs = 250L,
            label = "Grid Revs"
        )

        assertEquals("00:04.200", event.formattedTimestamp())
        assertEquals("00:04.2", event.formattedTimestampShort())
        assertEquals(HapticEventType.ENGINE, event.type)
        assertEquals(HapticIntensity.LOW, event.intensity)
    }

    @Test
    fun testIntensityAmplitudeScaling() {
        val lowScaled = HapticIntensity.LOW.scaleAmplitude(255)
        val medScaled = HapticIntensity.MEDIUM.scaleAmplitude(255)
        val highScaled = HapticIntensity.HIGH.scaleAmplitude(255)

        assertTrue(lowScaled < medScaled)
        assertTrue(medScaled <= highScaled)
        assertEquals(255, highScaled)
    }

    @Test
    fun testVideoTimelineAddAndSort() {
        val timeline = VideoTimeline()

        val e1 = HapticEvent(timestampMs = 12000L, type = HapticEventType.BRAKE)
        val e2 = HapticEvent(timestampMs = 4000L, type = HapticEventType.ENGINE)
        val e3 = HapticEvent(timestampMs = 7000L, type = HapticEventType.GEAR_SHIFT)

        timeline.addEvent(e1)
        timeline.addEvent(e2)
        timeline.addEvent(e3)

        val sorted = timeline.events
        assertEquals(3, sorted.size)
        assertEquals(4000L, sorted[0].timestampMs)
        assertEquals(7000L, sorted[1].timestampMs)
        assertEquals(12000L, sorted[2].timestampMs)
    }

    @Test
    fun testVideoTimelineRemoveAndUpdate() {
        val timeline = VideoTimeline()
        val e1 = HapticEvent(id = "test-1", timestampMs = 5000L, type = HapticEventType.KERB)
        timeline.addEvent(e1)

        val updated = e1.copy(intensity = HapticIntensity.HIGH, durationMs = 300L)
        timeline.updateEvent(updated)
        assertEquals(HapticIntensity.HIGH, timeline.getEventById("test-1")?.intensity)
        assertEquals(300L, timeline.getEventById("test-1")?.durationMs)

        timeline.removeEvent("test-1")
        assertEquals(0, timeline.events.size)
    }

    @Test
    fun testLegacyVibrationPatterns() {
        val enginePattern = HapticPattern.getLegacyPattern(HapticEventType.ENGINE, 200L)
        assertNotNull(enginePattern)
        assertTrue(enginePattern.isNotEmpty())

        val impactPattern = HapticPattern.getLegacyPattern(HapticEventType.IMPACT, 300L)
        assertNotNull(impactPattern)
        assertTrue(impactPattern.size >= 4)
    }
}
