package com.mplayerx.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.SurfaceView
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mplayerx.MPlayerXApp
import com.mplayerx.playback.ExoPlayerEngine
import com.mplayerx.settings.ResumeMode
import com.mplayerx.utils.formatDelta
import com.mplayerx.utils.formatTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
private val ASPECTS = listOf("fit", "fill", "16:9", "4:3", "zoom")

@Composable
fun PlayerScreen(
    app: MPlayerXApp,
    uri: Uri,
    resumeArgMs: Long,
    onBack: () -> Unit,
    onEnterPip: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val controller = remember { app.controller }
    val state by controller.state.collectAsState()
    val settings by app.settingsRepository.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var gestureText by remember { mutableStateOf<String?>(null) }
    var gestureDelta by remember { mutableLongStateOf(0L) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var showSpeed by remember { mutableStateOf(false) }
    var showAspect by remember { mutableStateOf(false) }
    var showTracks by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var resumeDialog by remember { mutableStateOf<Long?>(null) }
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    var hideJob by remember { mutableStateOf<Job?>(null) }
    var opened by remember(uri) { mutableStateOf(false) }

    fun pokeControls() {
        if (locked) return
        controlsVisible = true
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(3000)
            controlsVisible = false
            gestureText = null
        }
    }

    // Open once per uri; resolve resume position.
    LaunchedEffect(uri) {
        pokeControls()
        // Same video already in the engine (rotation, PiP, re-entry): resume it
        // instead of re-opening, which would restart playback from frame 0.
        if (controller.currentUri == uri) {
            controller.open(uri)
            return@LaunchedEffect
        }
        if (!opened) {
            opened = true
            val saved = runCatching {
                app.db.historyDao().byUri(uri.toString())
            }.getOrNull()
            val resumeFromArg = resumeArgMs >= 0
            val pos = when {
                resumeFromArg -> resumeArgMs
                saved == null -> 0L
                else -> when (settings?.resumeMode ?: ResumeMode.ASK) {
                    ResumeMode.ALWAYS -> saved.positionMs
                    ResumeMode.NEVER -> 0L
                    ResumeMode.ASK -> {
                        if (saved.positionMs > 5_000 && saved.positionMs < saved.durationMs - 10_000) {
                            resumeDialog = saved.positionMs
                        }
                        0L
                    }
                }
            }
            controller.open(uri, (pos as Long).coerceAtLeast(0))
            settings?.let {
                controller.engine.setPlaybackSpeed(it.defaultSpeed)
                controller.engine.setAspectRatio(it.defaultAspect)
            }
        }
    }

    DisposableEffect(uri) {
        onDispose { controller.saveNow(); controller.engine.pause() }
    }

    // Keep screen on while playing.
    LaunchedEffect(state.isPlaying) {
        activity?.window?.let {
            if (state.isPlaying) it.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Auto-rotate to match video aspect (toggle in Settings).
    LaunchedEffect(state.videoWidth, state.videoHeight, settings?.autoRotate) {
        if (settings?.autoRotate == true && state.videoWidth > 0 && state.videoHeight > 0) {
            activity?.requestedOrientation = if (state.videoWidth >= state.videoHeight) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    val audio = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    fun adjustVolume(delta: Int) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (cur + delta).coerceIn(0, max), 0)
        gestureText = "Volume ${(100 * audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.coerceAtLeast(1))}%"
    }
    fun adjustBrightness(delta: Float) {
        val win = activity?.window ?: return
        val cur = win.attributes.screenBrightness.takeIf { it >= 0 } ?: 0.5f
        val next = (cur + delta).coerceIn(0.05f, 1f)
        win.attributes = win.attributes.apply { screenBrightness = next }
        gestureText = "Brightness ${(next * 100).toInt()}%"
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(locked, settings) {
                detectTapGestures(
                    onTap = {
                        // ponytail: plain toggle; pokeControls() first would force
                        // visible=true and make this branch always hide.
                        if (!locked) {
                            controlsVisible = !controlsVisible
                            if (controlsVisible) pokeControls() else hideJob?.cancel()
                        }
                    },
                    onDoubleTap = { offset ->
                        if (locked) return@detectTapGestures
                        val w = size.width
                        val seekS = (settings?.seekSeconds ?: 10) * 1000L
                        when {
                            offset.x < w * 0.33f -> {
                                controller.engine.seekBy(-seekS)
                                gestureDelta = -seekS; gestureText = formatDelta(-seekS)
                            }
                            offset.x > w * 0.66f -> {
                                controller.engine.seekBy(seekS)
                                gestureDelta = seekS; gestureText = formatDelta(seekS)
                            }
                            else -> controller.toggle()
                        }
                        pokeControls()
                    },
                    onLongPress = {
                        if (!locked) {
                            controller.engine.setPlaybackSpeed(2f)
                            gestureText = "2× speed"
                        }
                    },
                )
            }
            .pointerInput(locked) {
                // ponytail: accumulate during the drag, seek once on release.
                // Seeking per tick floods ExoPlayer and stalls sparse-GOP AV1.
                detectHorizontalDragGestures(
                    onDragStart = { gestureDelta = 0L },
                    onDragEnd = {
                        if (!locked && gestureDelta != 0L) controller.engine.seekBy(gestureDelta)
                        gestureDelta = 0L
                        gestureText = null
                        pokeControls()
                    },
                    onHorizontalDrag = { _, dx ->
                        if (!locked) {
                            gestureDelta += (dx / 3).toLong()
                            gestureText = formatDelta(gestureDelta)
                        }
                    },
                )
            }
            .pointerInput(locked) {
                detectVerticalDragGestures(
                    onDragEnd = { pokeControls() },
                    onVerticalDrag = { change, dy ->
                        if (!locked) {
                            // Left half = brightness, right half = volume (§11.2/11.3)
                            if (change.position.x < size.width / 2) adjustBrightness(-dy / 800f)
                            else if (dy < -12) adjustVolume(1)
                            else if (dy > 12) adjustVolume(-1)
                        }
                    },
                )
            }
            .pointerInput(locked) {
                detectTransformGestures { _, _, z, _ ->
                    if (!locked) {
                        zoom = (zoom * z).coerceIn(0.5f, 4f)
                        controller.engine.setVideoScale(zoom)
                        gestureText = "Zoom ${(zoom * 100).toInt()}%"
                        pokeControls()
                    }
                }
            },
    ) {
        // Video surface
        val scale = state.videoScale
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).also { sv ->
                    (controller.engine as? ExoPlayerEngine)?.attachVideoSurface(sv)
                }
            },
            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale),
        )

        if (state.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        }

        gestureText?.let {
            Box(
                Modifier.align(Alignment.Center).background(Color(0xAA000000), MaterialTheme.shapes.medium).padding(16.dp),
            ) { Text(it, color = Color.White) }
        }

        // Top bar
        if (controlsVisible && !locked) {
            Row(
                Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    .background(Color(0x88000000)).padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { controller.saveNow(); onBack() }) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Text(
                    state.title.ifBlank { uri.lastPathSegment ?: "Video" },
                    color = Color.White, maxLines = 1, modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showInfo = true }) { Icon(Icons.Default.Info, null, tint = Color.White) }
                IconButton(onClick = { onEnterPip() }) { Icon(Icons.Default.PictureInPicture, null, tint = Color.White) }
                IconButton(onClick = { locked = true }) { Icon(Icons.Default.Lock, null, tint = Color.White) }
            }
        }
        if (locked) {
            IconButton(
                onClick = { locked = false; pokeControls() },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            ) { Icon(Icons.Default.LockOpen, null, tint = Color.White) }
        }

        // Bottom controls
        if (controlsVisible && !locked) {
            Column(
                Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .background(Color(0x88000000)).padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ponytail: seek once on release. Seeking per drag tick floods
                    // ExoPlayer and stalls AV1 (sparse keyframes, heavy decode).
                    val liveFraction =
                        if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
                    val shownFraction = scrubFraction ?: liveFraction
                    Text(formatTime((shownFraction * state.durationMs).toLong()), color = Color.White)
                    Slider(
                        value = shownFraction,
                        onValueChange = { scrubFraction = it },
                        onValueChangeFinished = {
                            // durationMs == 0 => unknown length; seeking would send
                            // the player to 0.0s (a restart), so ignore until known.
                            scrubFraction?.let {
                                if (state.durationMs > 0) {
                                    controller.engine.seek((it * state.durationMs).toLong())
                                }
                            }
                            scrubFraction = null
                            pokeControls()
                        },
                        enabled = state.durationMs > 0,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    Text(formatTime(state.durationMs), color = Color.White)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { showSpeed = true }) {
                        Text("${state.speed}×", color = Color.White)
                    }
                    IconButton(onClick = {
                        val s = (settings?.seekSeconds ?: 10) * 1000L
                        controller.engine.seekBy(-s)
                    }) { Text("−${settings?.seekSeconds ?: 10}s", color = Color.White) }
                    IconButton(
                        onClick = { controller.toggle() },
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null, tint = Color.White, modifier = Modifier.size(40.dp),
                        )
                    }
                    IconButton(onClick = {
                        val s = (settings?.seekSeconds ?: 10) * 1000L
                        controller.engine.seekBy(s)
                    }) { Text("+${settings?.seekSeconds ?: 10}s", color = Color.White) }
                    TextButton(onClick = { showAspect = true }) {
                        Text(state.aspectRatio, color = Color.White)
                    }
                    TextButton(onClick = { showTracks = true }) { Text("Tracks", color = Color.White) }
                }
            }
        }

        // Dialogs
        if (showSpeed) {
            AlertDialog(
                onDismissRequest = { showSpeed = false },
                title = { Text("Playback speed") },
                text = {
                    Column {
                        SPEEDS.forEach { s ->
                            TextButton(onClick = {
                                controller.engine.setPlaybackSpeed(s)
                                scope.launch { app.settingsRepository.setSpeed(s) }
                                showSpeed = false; pokeControls()
                            }) { Text(if (s == state.speed) "● ${s}×" else "${s}×") }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showSpeed = false }) { Text("Close") } },
            )
        }
        if (showAspect) {
            AlertDialog(
                onDismissRequest = { showAspect = false },
                title = { Text("Aspect ratio") },
                text = {
                    Column {
                        ASPECTS.forEach { a ->
                            TextButton(onClick = {
                                controller.engine.setAspectRatio(a); showAspect = false; pokeControls()
                            }) { Text(if (a == state.aspectRatio) "● $a" else a) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showAspect = false }) { Text("Close") } },
            )
        }
        if (showTracks) {
            AlertDialog(
                onDismissRequest = { showTracks = false },
                title = { Text("Audio / Subtitles") },
                text = {
                    Column {
                        Text("Audio", style = MaterialTheme.typography.titleSmall)
                        state.audioTracks.forEach { t ->
                            TextButton(onClick = { controller.engine.selectAudioTrack(t.id) }) {
                                Text((if (t.selected) "● " else "") + t.label)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Subtitles", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { controller.engine.selectSubtitleTrack(null) }) { Text("Off") }
                        state.subtitleTracks.forEach { t ->
                            TextButton(onClick = { controller.engine.selectSubtitleTrack(t.id) }) {
                                Text((if (t.selected) "● " else "") + t.label)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showTracks = false }) { Text("Close") } },
            )
        }
        if (showInfo) {
            val info = (controller.engine as? ExoPlayerEngine)?.decoderInfo()
            AlertDialog(
                onDismissRequest = { showInfo = false },
                title = { Text("Playback information") },
                text = {
                    Column {
                        Text("Codec  ${info?.videoCodec ?: "–"}")
                        Text("Resolution  ${info?.resolution ?: "–"}")
                        Text("FPS  ${info?.fps ?: 0f}")
                        Text("Bitrate  ${"%.1f".format(info?.bitrateMbps ?: 0.0)} Mbps")
                        Text("Hardware  ${info?.hardwareDecoding?.let { if (it) "Yes" else "No" } ?: "Unknown"}")
                        Text("Decoder  ${info?.decoderName ?: "–"}")
                        Text("Renderer  ${info?.renderer ?: "–"}")
                    }
                },
                confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Close") } },
            )
        }
        resumeDialog?.let { pos ->
            AlertDialog(
                onDismissRequest = { resumeDialog = null },
                title = { Text("Resume playback?") },
                text = { Text("Resume from ${formatTime(pos)}?") },
                confirmButton = {
                    Button(onClick = { controller.engine.seek(pos); resumeDialog = null }) { Text("Resume") }
                },
                dismissButton = {
                    TextButton(onClick = { resumeDialog = null }) { Text("Start over") }
                },
            )
        }
    }

    // Side-effect: reset long-press speed on release is handled by next tap; keep simple.
    LaunchedEffect(gestureText) {
        if (gestureText != null && gestureDelta == 0L) {
            delay(1200); gestureText = null
        }
    }
}
