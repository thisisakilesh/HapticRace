package com.hapticrace.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hapticrace.app.data.TimelineRepository
import com.hapticrace.app.haptic.HapticEventType
import com.hapticrace.app.haptic.HapticIntensity
import com.hapticrace.app.haptic.HapticManager
import com.hapticrace.app.model.HapticVideo
import com.hapticrace.app.ui.components.EventBadge
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: TimelineRepository,
    hapticManager: HapticManager,
    onCreateClick: () -> Unit,
    onEditVideo: (String) -> Unit,
    onPlayVideo: (String) -> Unit
) {
    val videos by repository.videos.collectAsState()
    val isHapticEnabled by hapticManager.isEnabled.collectAsState()
    val globalIntensity by hapticManager.globalIntensity.collectAsState()

    var showHardwareDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<HapticVideo?>(null) }

    Scaffold(
        containerColor = DarkCarbon,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCarbon),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(HyperCyan, TachometerAmber)))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkCarbon),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Logo",
                                    tint = HyperCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "HAPTIC RACE",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "SYNCHRONIZED TELEMETRY PLAYER",
                                style = MaterialTheme.typography.labelSmall,
                                color = HyperCyan,
                                fontSize = 9.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showHardwareDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Hardware Info",
                            tint = if (hapticManager.capabilities.hasVibrator) TelemetryGreen else ApexCrimson
                        )
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hardware Status Card
            item {
                HardwareStatusBar(
                    capabilities = hapticManager.capabilities,
                    onClick = { showHardwareDialog = true }
                )
            }

            // 2. Primary Actions Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkGraphite),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(DarkBorder, HyperCyan.copy(alpha = 0.4f))))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "CREATE IMMERSIVE TRACK",
                            style = MaterialTheme.typography.labelLarge,
                            color = HyperCyan,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Import Any Video & Map Haptic Telemetry",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add precise vibration events at exact timestamps for engine rumble, gear shifts, apex kerbs, and impacts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onCreateClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HyperCyan,
                                contentColor = DarkCarbon
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "IMPORT LOCAL VIDEO",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // 3. Section Header: My Haptic Videos
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MY HAPTIC VIDEOS",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${videos.size} TRACKS",
                        style = MaterialTheme.typography.labelSmall,
                        color = HyperCyan
                    )
                }
            }

            // 4. Video Items List
            if (videos.isEmpty()) {
                item {
                    EmptyVideosCard(onCreateClick = onCreateClick)
                }
            } else {
                items(videos, key = { it.id }) { video ->
                    HapticVideoCard(
                        video = video,
                        onEdit = { onEditVideo(video.id) },
                        onPlay = { onPlayVideo(video.id) },
                        onDelete = { videoToDelete = video }
                    )
                }
            }
        }
    }

    // Hardware Details Dialog
    if (showHardwareDialog) {
        AlertDialog(
            onDismissRequest = { showHardwareDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Text(
                    text = "HAPTIC HARDWARE STATUS",
                    style = MaterialTheme.typography.titleMedium,
                    color = HyperCyan,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = hapticManager.capabilities.statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (hapticManager.capabilities.hasVibrator) TelemetryGreen else ApexCrimson,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = hapticManager.capabilities.detailedStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Android API: ${hapticManager.capabilities.androidVersion}\n" +
                                "• Amplitude Control: ${if (hapticManager.capabilities.hasAmplitudeControl) "Yes (HD Linear)" else "Basic"}\n" +
                                "• Vibrator Hardware: ${if (hapticManager.capabilities.hasVibrator) "Detected" else "Unavailable"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        hapticManager.previewEvent(HapticEventType.GEAR_SHIFT, HapticIntensity.HIGH)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HyperCyan, contentColor = DarkCarbon)
                ) {
                    Text("TEST PULSE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHardwareDialog = false }) {
                    Text("CLOSE", color = TextSecondary)
                }
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Text(
                    text = "HAPTIC SETTINGS",
                    style = MaterialTheme.typography.titleMedium,
                    color = HyperCyan,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Haptic Feedback", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Enable vibration synchronization", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isHapticEnabled,
                            onCheckedChange = { hapticManager.setEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkCarbon,
                                checkedTrackColor = HyperCyan
                            )
                        )
                    }

                    Column {
                        Text("Global Intensity", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HapticIntensity.entries.forEach { intensity ->
                                FilterChip(
                                    selected = globalIntensity == intensity,
                                    onClick = {
                                        hapticManager.setGlobalIntensity(intensity)
                                        hapticManager.previewEvent(HapticEventType.GEAR_SHIFT, intensity)
                                    },
                                    label = { Text(intensity.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = HyperCyan,
                                        selectedLabelColor = DarkCarbon
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("DONE", color = HyperCyan)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            containerColor = DarkSurfaceElevated,
            title = { Text("Delete Track?", color = TextPrimary) },
            text = { Text("Are you sure you want to delete '${video.videoName}'? This will remove its haptic timeline.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteVideo(video.id)
                        videoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ApexCrimson, contentColor = Color.White)
                ) {
                    Text("DELETE")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun HardwareStatusBar(
    capabilities: com.hapticrace.app.haptic.HapticCapabilities,
    onClick: () -> Unit
) {
    val isAvail = capabilities.hasVibrator
    val statusColor = if (isAvail) TelemetryGreen else ApexCrimson

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkGraphite)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = capabilities.statusText,
                color = statusColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
        Icon(
            imageVector = Icons.Default.Vibration,
            contentDescription = null,
            tint = statusColor.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun HapticVideoCard(
    video: HapticVideo,
    onEdit: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGraphite),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkBorder, DarkSurfaceElevated)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = video.videoName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        if (video.isDemo) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TachometerAmber.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DEMO",
                                    color = TachometerAmber,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Duration: ${video.formattedDuration()} • ${video.events.size} Haptic Events",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Preview event badges preview
            if (video.events.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val previewEvents = video.sortedEvents.take(4)
                    previewEvents.forEach { event ->
                        EventBadge(type = event.type, label = "${event.formattedTimestampShort()} ${event.type.name}")
                    }
                    if (video.events.size > 4) {
                        Text(
                            text = "+${video.events.size - 4} more",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HyperCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(HyperCyan.copy(alpha = 0.5f), HyperCyan)))
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("EDIT TRACK", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HyperCyan, contentColor = DarkCarbon)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PLAY RACE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmptyVideosCard(onCreateClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkBorder, DarkBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "NO HAPTIC TRACKS SAVED",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Import a video to create your first haptic timeline.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = HyperCyan, contentColor = DarkCarbon)
            ) {
                Text("IMPORT VIDEO")
            }
        }
    }
}
