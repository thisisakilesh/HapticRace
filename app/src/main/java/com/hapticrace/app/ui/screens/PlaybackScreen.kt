package com.hapticrace.app.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.hapticrace.app.haptic.HapticIntensity
import com.hapticrace.app.haptic.HapticManager
import com.hapticrace.app.race.PlaybackSyncManager
import com.hapticrace.app.ui.components.HapticTelemetryOverlay
import com.hapticrace.app.ui.components.TimelineScrubber
import com.hapticrace.app.ui.components.VideoPlayerView
import com.hapticrace.app.ui.theme.DarkBorder
import com.hapticrace.app.ui.theme.DarkCarbon
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
fun PlaybackScreen(
    videoId: String,
    repository: TimelineRepository,
    hapticManager: HapticManager,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val video = remember(videoId) { repository.getVideoById(videoId) }
    val events = remember(video) { video?.events ?: emptyList() }

    val syncManager = remember { PlaybackSyncManager(hapticManager, coroutineScope) }
    val activeEvent by syncManager.activeHapticEvent.collectAsState()

    val isHapticEnabled by hapticManager.isEnabled.collectAsState()
    val globalIntensity by hapticManager.globalIntensity.collectAsState()

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(video?.durationMs ?: 60000L) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isUserScrubbing by remember { mutableStateOf(false) }

    // ExoPlayer instance configured for smooth playback
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Set video source
    LaunchedEffect(video?.videoUri) {
        val uriStr = video?.videoUri ?: return@LaunchedEffect
        try {
            val mediaItem = MediaItem.fromUri(Uri.parse(uriStr))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            Log.e("PlaybackScreen", "Error loading video: ${e.message}")
        }
    }

    // Player events listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val pDur = exoPlayer.duration
                    if (pDur > 0) durationMs = pDur
                } else if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
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

    // High precision playback monitor loop (20ms interval)
    LaunchedEffect(isPlaying) {
        while (isActive) {
            if (!isUserScrubbing) {
                val pos = exoPlayer.currentPosition
                currentPositionMs = pos
                syncManager.onPlaybackPositionUpdate(pos, isPlaying, events)
            }
            delay(20L)
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying && !isUserScrubbing) {
            delay(3500L)
            isControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isControlsVisible = !isControlsVisible
            }
    ) {
        // 1. FULLSCREEN VIDEO PLAYER
        VideoPlayerView(
            exoPlayer = exoPlayer,
            modifier = Modifier.fillMaxSize(),
            useController = false
        )

        // 2. REAL-TIME HAPTIC TELEMETRY HUD (Visible even when controls are hidden)
        HapticTelemetryOverlay(
            activeEvent = activeEvent,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        )

        // 3. IMMERSIVE OVERLAY CONTROLS (Fades in/out on touch)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                DarkCarbon.copy(alpha = 0.85f),
                                Color.Transparent,
                                Color.Transparent,
                                DarkCarbon.copy(alpha = 0.92f)
                            )
                        )
                    )
            ) {
                // TOP BAR HUD: Exit, Title, Haptic Toggle & Intensity
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onExit,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkCarbon.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Fullscreen",
                                tint = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = video?.videoName ?: "Haptic Playback",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${events.size} HAPTIC EVENTS ARMED",
                                style = MaterialTheme.typography.labelSmall,
                                color = HyperCyan,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Haptic Toggle & Intensity Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Intensity Selector Chips
                        HapticIntensity.entries.forEach { intensity ->
                            FilterChip(
                                selected = globalIntensity == intensity,
                                onClick = { hapticManager.setGlobalIntensity(intensity) },
                                label = { Text(intensity.displayName, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HyperCyan,
                                    selectedLabelColor = DarkCarbon,
                                    containerColor = DarkCarbon.copy(alpha = 0.6f),
                                    labelColor = TextSecondary
                                )
                            )
                        }

                        // Master Haptic ON/OFF
                        Button(
                            onClick = { hapticManager.setEnabled(!isHapticEnabled) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isHapticEnabled) TelemetryGreen else DarkBorder,
                                contentColor = if (isHapticEnabled) DarkCarbon else TextMuted
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHapticEnabled) "HAPTIC ON" else "HAPTIC OFF",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // BOTTOM CONTROLS HUD: Playhead Scrubber, Play/Pause, Rewind/Fast Forward
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    // Timeline Scrubber showing all racing markers
                    TimelineScrubber(
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        events = events,
                        selectedEventId = null,
                        onSeek = { targetMs ->
                            exoPlayer.seekTo(targetMs)
                            syncManager.onSeekTo(targetMs, events)
                        },
                        onSelectEvent = { tappedEvent ->
                            tappedEvent?.let {
                                exoPlayer.seekTo(it.timestampMs)
                                syncManager.onSeekTo(it.timestampMs, events)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Time readout
                        Text(
                            text = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = FontFamily.Monospace,
                            color = HyperCyan,
                            fontSize = 13.sp
                        )

                        // Center playback buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Seek back 5s
                            IconButton(onClick = {
                                val target = (currentPositionMs - 5000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                                syncManager.onSeekTo(target, events)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "-5s",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Play / Pause Primary Button
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                            exoPlayer.seekTo(0L)
                                            syncManager.reset()
                                        }
                                        exoPlayer.play()
                                    }
                                },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(HyperCyan)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = DarkCarbon,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Seek forward 5s
                            IconButton(onClick = {
                                val target = (currentPositionMs + 5000L).coerceAtMost(durationMs)
                                exoPlayer.seekTo(target)
                                syncManager.onSeekTo(target, events)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "+5s",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Exit button shortcut
                        Button(
                            onClick = onExit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkBorder,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("EXIT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
