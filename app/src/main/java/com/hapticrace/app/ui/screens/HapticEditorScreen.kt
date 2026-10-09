package com.hapticrace.app.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.material3.TextButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hapticrace.app.data.TimelineRepository
import com.hapticrace.app.haptic.HapticEvent
import com.hapticrace.app.haptic.HapticEventType
import com.hapticrace.app.haptic.HapticIntensity
import com.hapticrace.app.haptic.HapticManager
import com.hapticrace.app.model.HapticVideo
import com.hapticrace.app.race.PlaybackSyncManager
import com.hapticrace.app.race.TimelineEvent
import com.hapticrace.app.race.VideoTimeline
import com.hapticrace.app.ui.components.EventBadge
import com.hapticrace.app.ui.components.HapticTelemetryOverlay
import com.hapticrace.app.ui.components.TimelineScrubber
import com.hapticrace.app.ui.components.VideoPlayerView
import com.hapticrace.app.ui.theme.ApexCrimson
import com.hapticrace.app.ui.theme.DarkBorder
import com.hapticrace.app.ui.theme.DarkCarbon
import com.hapticrace.app.ui.theme.DarkGraphite
import com.hapticrace.app.ui.theme.DarkSurface
import com.hapticrace.app.ui.theme.DarkSurfaceElevated
import com.hapticrace.app.ui.theme.HyperCyan
import com.hapticrace.app.ui.theme.TachometerAmber
import com.hapticrace.app.ui.theme.TelemetryGreen
import com.hapticrace.app.ui.theme.TextMuted
import com.hapticrace.app.ui.theme.TextPrimary
import com.hapticrace.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HapticEditorScreen(
    videoId: String,
    repository: TimelineRepository,
    hapticManager: HapticManager,
    onBack: () -> Unit,
    onLaunchPlayback: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val initialVideo = remember(videoId) { repository.getVideoById(videoId) }
    var currentVideo by remember { mutableStateOf(initialVideo) }

    val timeline = remember(videoId) {
        VideoTimeline(initialVideo?.events ?: emptyList())
    }
    val events by timeline.eventsFlow.collectAsState()

    // Sync manager for live preview while scrubbing/playing inside editor
    val syncManager = remember { PlaybackSyncManager(hapticManager, coroutineScope) }
    val activeFiringEvent by syncManager.activeHapticEvent.collectAsState()

    // Player state
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(initialVideo?.durationMs ?: 60000L) }
    var selectedEvent by remember { mutableStateOf<HapticEvent?>(null) }
    var showEventListSheet by remember { mutableStateOf(false) }

    // Media3 ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Initialize player source
    LaunchedEffect(currentVideo?.videoUri) {
        val uriStr = currentVideo?.videoUri ?: return@LaunchedEffect
        try {
            val mediaItem = MediaItem.fromUri(Uri.parse(uriStr))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        } catch (e: Exception) {
            Log.e("HapticEditor", "Failed to load video source: ${e.message}")
        }
    }

    // Player listeners
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val playerDur = exoPlayer.duration
                    if (playerDur > 0) {
                        durationMs = playerDur
                    }
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
            syncManager.reset()
        }
    }

    // High precision position tracking & sync loop while editing
    LaunchedEffect(isPlaying) {
        while (isActive) {
            val pos = exoPlayer.currentPosition
            currentPositionMs = pos
            syncManager.onPlaybackPositionUpdate(pos, isPlaying, events)
            delay(25L)
        }
    }

    // Save timeline changes to repository
    fun persistTimeline() {
        val video = currentVideo ?: return
        val updated = video.copy(events = events, durationMs = durationMs)
        currentVideo = updated
        repository.saveVideo(updated)
    }

    // Add event at current playback timestamp
    fun addEventAtCurrentTime(type: HapticEventType) {
        val newEvent = HapticEvent(
            timestampMs = currentPositionMs,
            type = type,
            intensity = type.defaultIntensity,
            durationMs = type.defaultDurationMs,
            label = "${type.title} @ ${formatTimeShort(currentPositionMs)}"
        )
        timeline.addEvent(newEvent)
        selectedEvent = newEvent
        persistTimeline()

        // Give immediate sensory feedback when user hits an event button
        hapticManager.triggerEvent(newEvent)

        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = "Added ${type.title} at ${formatTimeShort(currentPositionMs)}"
            )
        }
    }

    Scaffold(
        containerColor = DarkCarbon,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCarbon),
                title = {
                    Column {
                        Text(
                            text = currentVideo?.videoName ?: "Haptic Editor",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${events.size} EVENTS MAPPED",
                            style = MaterialTheme.typography.labelSmall,
                            color = HyperCyan,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Test Race / Launch immersive playback
                    Button(
                        onClick = {
                            persistTimeline()
                            onLaunchPlayback(videoId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HyperCyan, contentColor = DarkCarbon),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.SportsScore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RACE (PLAY)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. VIDEO PREVIEW PLAYER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                VideoPlayerView(
                    exoPlayer = exoPlayer,
                    modifier = Modifier.fillMaxSize(),
                    useController = false
                )

                // Real-time telemetry pulse HUD
                HapticTelemetryOverlay(
                    activeEvent = activeFiringEvent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )

                // Current time code overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkCarbon.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${formatTimeWithMillis(currentPositionMs)} / ${formatTime(durationMs)}",
                        color = HyperCyan,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }

            // 2. VIDEO PLAYBACK CONTROLS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkGraphite)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Seek back 1s
                    IconButton(onClick = {
                        val newPos = (currentPositionMs - 1000L).coerceAtLeast(0L)
                        exoPlayer.seekTo(newPos)
                        syncManager.onSeekTo(newPos, events)
                    }) {
                        Icon(imageVector = Icons.Default.FastRewind, contentDescription = "-1s", tint = TextSecondary)
                    }

                    // Play / Pause
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(HyperCyan)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = DarkCarbon
                        )
                    }

                    // Seek forward 1s
                    IconButton(onClick = {
                        val newPos = (currentPositionMs + 1000L).coerceAtMost(durationMs)
                        exoPlayer.seekTo(newPos)
                        syncManager.onSeekTo(newPos, events)
                    }) {
                        Icon(imageVector = Icons.Default.FastForward, contentDescription = "+1s", tint = TextSecondary)
                    }

                    // Restart
                    IconButton(onClick = {
                        exoPlayer.seekTo(0L)
                        syncManager.reset()
                    }) {
                        Icon(imageVector = Icons.Default.Replay, contentDescription = "Restart", tint = TextMuted)
                    }
                }

                // Micro frame jump buttons for precise cueing
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        val newPos = (currentPositionMs - 100L).coerceAtLeast(0L)
                        exoPlayer.seekTo(newPos)
                        syncManager.onSeekTo(newPos, events)
                    }) {
                        Text("-100ms", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }
                    TextButton(onClick = {
                        val newPos = (currentPositionMs + 100L).coerceAtMost(durationMs)
                        exoPlayer.seekTo(newPos)
                        syncManager.onSeekTo(newPos, events)
                    }) {
                        Text("+100ms", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. HAPTIC TIMELINE CANVAS
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HAPTIC TIMELINE",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Tap or scrub track to seek • Tap pins to edit",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TimelineScrubber(
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    events = events,
                    selectedEventId = selectedEvent?.id,
                    onSeek = { seekMs ->
                        exoPlayer.seekTo(seekMs)
                        syncManager.onSeekTo(seekMs, events)
                    },
                    onSelectEvent = { event ->
                        selectedEvent = event
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. ADD HAPTIC EVENT BUTTONS
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "RECORD EVENT AT CURRENT POSITION [${formatTimeShort(currentPositionMs)}]",
                    style = MaterialTheme.typography.labelSmall,
                    color = HyperCyan,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Presets Grid / Flow
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EventPresetButton(
                        type = HapticEventType.ENGINE,
                        color = TimelineEvent.getColorForType(HapticEventType.ENGINE),
                        onClick = { addEventAtCurrentTime(HapticEventType.ENGINE) }
                    )
                    EventPresetButton(
                        type = HapticEventType.GEAR_SHIFT,
                        color = TimelineEvent.getColorForType(HapticEventType.GEAR_SHIFT),
                        onClick = { addEventAtCurrentTime(HapticEventType.GEAR_SHIFT) }
                    )
                    EventPresetButton(
                        type = HapticEventType.BRAKE,
                        color = TimelineEvent.getColorForType(HapticEventType.BRAKE),
                        onClick = { addEventAtCurrentTime(HapticEventType.BRAKE) }
                    )
                    EventPresetButton(
                        type = HapticEventType.KERB,
                        color = TimelineEvent.getColorForType(HapticEventType.KERB),
                        onClick = { addEventAtCurrentTime(HapticEventType.KERB) }
                    )
                    EventPresetButton(
                        type = HapticEventType.IMPACT,
                        color = TimelineEvent.getColorForType(HapticEventType.IMPACT),
                        onClick = { addEventAtCurrentTime(HapticEventType.IMPACT) }
                    )
                    EventPresetButton(
                        type = HapticEventType.CUSTOM,
                        color = TimelineEvent.getColorForType(HapticEventType.CUSTOM),
                        onClick = { addEventAtCurrentTime(HapticEventType.CUSTOM) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. SELECTED EVENT INSPECTOR & EDITOR
            selectedEvent?.let { event ->
                EventInspectorCard(
                    event = event,
                    onUpdateEvent = { updated ->
                        timeline.updateEvent(updated)
                        selectedEvent = updated
                        persistTimeline()
                    },
                    onDeleteEvent = {
                        timeline.removeEvent(event.id)
                        selectedEvent = null
                        persistTimeline()
                    },
                    onPreviewHaptic = {
                        hapticManager.triggerEvent(event)
                    },
                    onClose = { selectedEvent = null },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 6. EVENT LIST STRIP (Chronological review)
            if (events.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "TIMELINE EVENTS (${events.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(events, key = { it.id }) { item ->
                            val isSel = item.id == selectedEvent?.id
                            Card(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedEvent = item
                                        exoPlayer.seekTo(item.timestampMs)
                                        syncManager.onSeekTo(item.timestampMs, events)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) DarkSurfaceElevated else DarkGraphite
                                ),
                                border = if (isSel) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(HyperCyan, HyperCyan))) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    EventBadge(type = item.type)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.formattedTimestampShort(),
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun EventPresetButton(
    type: HapticEventType,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color
        ),
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .height(44.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "+ ${type.title}",
            style = MaterialTheme.typography.labelLarge,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EventInspectorCard(
    event: HapticEvent,
    onUpdateEvent: (HapticEvent) -> Unit,
    onDeleteEvent: () -> Unit,
    onPreviewHaptic: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = TimelineEvent.getColorForType(event.type)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGraphite),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(color, DarkBorder)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EventBadge(type = event.type)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EDIT EVENT @ ${event.formattedTimestamp()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Micro-tune Timestamp Controls
            Text(
                text = "FINE-TUNE POSITION",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onUpdateEvent(event.copy(timestampMs = (event.timestampMs - 200L).coerceAtLeast(0L))) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("-200ms", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { onUpdateEvent(event.copy(timestampMs = (event.timestampMs - 50L).coerceAtLeast(0L))) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("-50ms", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { onUpdateEvent(event.copy(timestampMs = event.timestampMs + 50L)) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("+50ms", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { onUpdateEvent(event.copy(timestampMs = event.timestampMs + 200L)) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("+200ms", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Intensity Selector
            Text(
                text = "INTENSITY LEVEL",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HapticIntensity.entries.forEach { intensity ->
                    FilterChip(
                        selected = event.intensity == intensity,
                        onClick = { onUpdateEvent(event.copy(intensity = intensity)) },
                        label = { Text(intensity.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color,
                            selectedLabelColor = DarkCarbon
                        )
                    )
                }
            }

            // Custom Duration slider (for CUSTOM type or customizable presets)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "DURATION: ${event.durationMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Slider(
                value = event.durationMs.toFloat(),
                onValueChange = { newDuration ->
                    onUpdateEvent(event.copy(durationMs = newDuration.toLong()))
                },
                valueRange = 20f..800f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = DarkBorder
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Preview & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPreviewHaptic,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.2f), contentColor = color)
                ) {
                    Icon(imageVector = Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PREVIEW VIBE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDeleteEvent,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ApexCrimson.copy(alpha = 0.2f), contentColor = ApexCrimson)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DELETE EVENT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun formatTimeShort(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (millis % 1000) / 100
    return String.format("%02d:%02d.%d", minutes, seconds, tenths)
}

private fun formatTimeWithMillis(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val ms = millis % 1000
    return String.format("%02d:%02d.%03d", minutes, seconds, ms)
}
