package com.ajrpachon.chatapp.ui.chat

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// ── Audio helpers ─────────────────────────────────────────────────────────────

/** Downsamples a variable-length amplitude history into a fixed number of bars by averaging
 *  each bucket, so a real recording's waveform always renders at a consistent bar count. */
private fun resampleToBars(samples: List<Float>, barCount: Int): List<Float> {
    if (samples.size <= barCount) {
        return samples + List(barCount - samples.size) { samples.lastOrNull() ?: 0f }
    }
    val bucketSize = samples.size.toFloat() / barCount
    return List(barCount) { i ->
        val start = (i * bucketSize).toInt()
        val end = ((i + 1) * bucketSize).toInt().coerceAtLeast(start + 1).coerceAtMost(samples.size)
        val bucket = samples.subList(start, end)
        (bucket.average().toFloat() * 3f).coerceIn(0.2f, 1f)
    }
}

/** Formats a millisecond duration as `m:ss` (e.g. `1:05`). */
internal fun formatAudioDuration(ms: Int): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

// ── Recording indicator ───────────────────────────────────────────────────────

@Composable
internal fun RecordingBar(
    durationMs: Long,
    amplitudeHistory: List<Float>,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        val barColor = MaterialTheme.colorScheme.error
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(36.dp),
        ) {
            val barW = 2.dp.toPx()
            val gap = 1.5.dp.toPx()
            val step = barW + gap
            val minH = 4.dp.toPx()
            val maxH = size.height
            // More/thinner bars, right-anchored: the newest sample always lands at the right
            // edge and older ones scroll off the left, instead of clipping the newest bar once
            // the fixed-size history overflows the canvas.
            val maxBars = (size.width / step).toInt().coerceAtLeast(1)
            val visible = amplitudeHistory.takeLast(maxBars)
            val startX = size.width - visible.size * step
            visible.forEachIndexed { i, amp ->
                val h = (minH + amp * (maxH - minH)).coerceIn(minH, maxH)
                val top = (maxH - h) / 2f
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(startX + i * step, top),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = formatAudioDuration(durationMs.toInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        IconButton(onClick = onStop) {
            Icon(
                Icons.Default.Stop,
                contentDescription = "Detener grabación",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ── Audio preview (after recording) ──────────────────────────────────────────

@Composable
internal fun AudioPreviewBar(
    filePath: String,
    amplitudeHistory: List<Float>,
    isUploading: Boolean,
    onDiscard: () -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LocalAudioPlayer(
            filePath = filePath,
            amplitudeHistory = amplitudeHistory,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDiscard, enabled = !isUploading) {
            Icon(Icons.Default.Delete, contentDescription = "Descartar audio")
        }
        if (isUploading) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp).padding(8.dp))
        } else {
            IconButton(onClick = onSend) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar audio")
            }
        }
    }
}

// ── Audio players ─────────────────────────────────────────────────────────────

@Composable
internal fun LocalAudioPlayer(filePath: String, amplitudeHistory: List<Float> = emptyList(), modifier: Modifier = Modifier) {
    var isPrepared by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentMs by remember { mutableStateOf(0) }
    var durationMs by remember { mutableStateOf(0) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    val playerRef = remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(filePath) {
        val mp = MediaPlayer()
        playerRef.value = mp
        runCatching {
            mp.setDataSource(filePath)
            mp.prepare()
            isPrepared = true
            durationMs = mp.duration
            mp.setOnCompletionListener { p -> p.seekTo(0); isPlaying = false; currentMs = 0 }
        }
        onDispose { mp.release(); playerRef.value = null }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentMs = playerRef.value?.currentPosition ?: 0
            delay(100)
        }
    }

    AudioPlayerRow(
        modifier = modifier,
        isPrepared = isPrepared,
        isPlaying = isPlaying,
        currentMs = currentMs,
        durationMs = durationMs,
        onToggle = {
            val mp = playerRef.value ?: return@AudioPlayerRow
            if (!isPrepared) return@AudioPlayerRow
            if (mp.isPlaying) { mp.pause(); isPlaying = false }
            else { mp.start(); isPlaying = true }
        },
        waveformSeed = filePath.hashCode(),
        realWaveform = amplitudeHistory,
        playbackSpeed = playbackSpeed,
        onSpeedChange = { speed ->
            playbackSpeed = speed
            playerRef.value?.let { mp ->
                mp.playbackParams = mp.playbackParams.setSpeed(speed)
            }
        },
    )
}

@Composable
internal fun RemoteAudioPlayer(url: String, modifier: Modifier = Modifier) {
    var isPrepared by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentMs by remember { mutableStateOf(0) }
    var durationMs by remember { mutableStateOf(0) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    val playerRef = remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(url) {
        val mp = MediaPlayer()
        playerRef.value = mp
        runCatching {
            mp.setDataSource(url)
            mp.setOnPreparedListener { p -> isPrepared = true; durationMs = p.duration }
            mp.setOnCompletionListener { p -> p.seekTo(0); isPlaying = false; currentMs = 0 }
            mp.prepareAsync()
        }
        onDispose { mp.release(); playerRef.value = null }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentMs = playerRef.value?.currentPosition ?: 0
            delay(100)
        }
    }

    AudioPlayerRow(
        modifier = modifier,
        isPrepared = isPrepared,
        isPlaying = isPlaying,
        currentMs = currentMs,
        durationMs = durationMs,
        onToggle = {
            val mp = playerRef.value ?: return@AudioPlayerRow
            if (!isPrepared) return@AudioPlayerRow
            if (mp.isPlaying) { mp.pause(); isPlaying = false }
            else { mp.start(); isPlaying = true }
        },
        waveformSeed = url.hashCode(),
        playbackSpeed = playbackSpeed,
        onSpeedChange = { speed ->
            playbackSpeed = speed
            playerRef.value?.let { mp ->
                mp.playbackParams = mp.playbackParams.setSpeed(speed)
            }
        },
    )
}

@Suppress("LongParameterList")
@Composable
internal fun AudioPlayerRow(
    isPrepared: Boolean,
    isPlaying: Boolean,
    currentMs: Int,
    durationMs: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    waveformSeed: Int = 0,
    realWaveform: List<Float> = emptyList(),
    playbackSpeed: Float = 1f,
    onSpeedChange: (Float) -> Unit = {},
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val progress = if (durationMs > 0) currentMs.toFloat() / durationMs else 0f

    // Use the actual recorded amplitude data when available (resampled to a fixed bar count so
    // it draws the same regardless of recording length); fall back to a seeded random waveform
    // only when we have no real data (e.g. audio received from someone else).
    val bars = remember(waveformSeed, realWaveform) {
        if (realWaveform.isNotEmpty()) resampleToBars(realWaveform, barCount = 32)
        else {
            val rng = java.util.Random(waveformSeed.toLong())
            List(32) { 0.2f + rng.nextFloat() * 0.8f }
        }
    }

    val speedSteps = listOf(1f, 1.5f, 2f)

    Row(
        modifier = modifier.widthIn(min = 160.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggle, enabled = isPrepared) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
            )
        }
        Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
            ) {
                val barCount = bars.size
                val gap = size.width * 0.015f
                val barW = (size.width - gap * (barCount - 1)) / barCount
                bars.forEachIndexed { i, h ->
                    val barH = h * size.height
                    val x = i * (barW + gap)
                    val y = (size.height - barH) / 2f
                    val fraction = (i + 1f) / barCount
                    drawRoundRect(
                        color = if (fraction <= progress) activeColor else inactiveColor,
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 2),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatAudioDuration(if (currentMs > 0) currentMs else durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        // Speed chip: cycles ×1 → ×1.5 → ×2 → ×1
        Surface(
            onClick = {
                val nextIndex = (speedSteps.indexOf(playbackSpeed) + 1) % speedSteps.size
                onSpeedChange(speedSteps[nextIndex])
            },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        ) {
            Text(
                text = "×${if (playbackSpeed % 1f == 0f) playbackSpeed.toInt().toString() else playbackSpeed.toString()}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}
