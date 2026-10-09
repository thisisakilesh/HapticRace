package com.hapticrace.app.data

import android.content.Context
import android.util.Log
import com.hapticrace.app.haptic.HapticEvent
import com.hapticrace.app.haptic.HapticEventType
import com.hapticrace.app.haptic.HapticIntensity
import com.hapticrace.app.model.HapticVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Local storage repository for HapticRace video timelines.
 * Uses JSON files in app-internal storage.
 */
class TimelineRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val tag = "TimelineRepo"
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val storageFile = File(context.filesDir, "haptic_videos_registry.json")
    private val _videos = MutableStateFlow<List<HapticVideo>>(emptyList())
    val videos: StateFlow<List<HapticVideo>> = _videos.asStateFlow()

    init {
        scope.launch {
            loadVideos()
        }
    }

    private suspend fun loadVideos() = withContext(Dispatchers.IO) {
        try {
            if (storageFile.exists()) {
                val content = storageFile.readText()
                val list = json.decodeFromString<List<HapticVideo>>(content)
                _videos.value = list
                Log.d(tag, "Loaded ${list.size} saved haptic videos")
            } else {
                // Initialize with built-in racing demo track
                val demoVideo = createDemoTrack()
                _videos.value = listOf(demoVideo)
                persistVideos(_videos.value)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to load saved videos: ${e.message}", e)
            val demo = createDemoTrack()
            _videos.value = listOf(demo)
        }
    }

    private suspend fun persistVideos(list: List<HapticVideo>) = withContext(Dispatchers.IO) {
        try {
            val content = json.encodeToString(list)
            storageFile.writeText(content)
            Log.d(tag, "Persisted ${list.size} videos to disk")
        } catch (e: Exception) {
            Log.e(tag, "Failed to save videos: ${e.message}", e)
        }
    }

    fun saveVideo(video: HapticVideo) {
        scope.launch {
            val current = _videos.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.id == video.id }
            val updatedVideo = video.copy(lastModifiedAt = System.currentTimeMillis())

            if (existingIndex >= 0) {
                current[existingIndex] = updatedVideo
            } else {
                current.add(0, updatedVideo)
            }

            _videos.value = current
            persistVideos(current)
        }
    }

    fun deleteVideo(videoId: String) {
        scope.launch {
            val current = _videos.value.filter { it.id != videoId }
            _videos.value = current
            persistVideos(current)
        }
    }

    fun getVideoById(id: String): HapticVideo? {
        return _videos.value.find { it.id == id }
    }

    /**
     * Creates an initial motorsport demo video track with pre-timed haptics
     * matching the user's example sequence:
     * 00:04.20 -> ENGINE
     * 00:07.85 -> GEAR_SHIFT
     * 00:12.40 -> BRAKE
     * 00:14.10 -> KERB
     * 00:18.60 -> IMPACT
     * plus additional race telemetry events through 35 seconds.
     */
    private fun createDemoTrack(): HapticVideo {
        val demoEvents = listOf(
            HapticEvent(
                timestampMs = 4200L,
                type = HapticEventType.ENGINE,
                intensity = HapticIntensity.LOW,
                durationMs = 250L,
                label = "Grid Launch Revs"
            ),
            HapticEvent(
                timestampMs = 7850L,
                type = HapticEventType.GEAR_SHIFT,
                intensity = HapticIntensity.HIGH,
                durationMs = 45L,
                label = "Upshift 2nd Gear"
            ),
            HapticEvent(
                timestampMs = 12400L,
                type = HapticEventType.BRAKE,
                intensity = HapticIntensity.MEDIUM,
                durationMs = 180L,
                label = "Turn 1 Heavy Brake"
            ),
            HapticEvent(
                timestampMs = 14100L,
                type = HapticEventType.KERB,
                intensity = HapticIntensity.MEDIUM,
                durationMs = 240L,
                label = "Apex Kerb Strike"
            ),
            HapticEvent(
                timestampMs = 18600L,
                type = HapticEventType.IMPACT,
                intensity = HapticIntensity.HIGH,
                durationMs = 320L,
                label = "Sidepod Contact"
            ),
            HapticEvent(
                timestampMs = 22100L,
                type = HapticEventType.GEAR_SHIFT,
                intensity = HapticIntensity.HIGH,
                durationMs = 45L,
                label = "Corner Exit Upshift"
            ),
            HapticEvent(
                timestampMs = 25500L,
                type = HapticEventType.KERB,
                intensity = HapticIntensity.HIGH,
                durationMs = 260L,
                label = "Chicane Rumble"
            ),
            HapticEvent(
                timestampMs = 30000L,
                type = HapticEventType.ENGINE,
                intensity = HapticIntensity.MEDIUM,
                durationMs = 300L,
                label = "Main Straight Rev Limiter"
            )
        )

        return HapticVideo(
            id = "demo_motorsport_sprint",
            videoUri = "asset:///demo_race.mp4",
            videoName = "Apex Sprint GP (Demo Showcase)",
            durationMs = 35000L,
            thumbnailUri = null,
            events = demoEvents,
            isDemo = true
        )
    }
}
