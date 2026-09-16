package com.mplayerx.playback

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVP engine over Media3 ExoPlayer. HW decoding preferred via MediaCodec
 * (DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER), AV1 included
 * where the device supports it.
 */
@OptIn(UnstableApi::class)
class ExoPlayerEngine(
    private val context: Context,
    private val scope: CoroutineScope,
) : PlayerEngine {

    private val trackSelector = DefaultTrackSelector(context).apply {
        setParameters(buildUponParameters().setForceHighestSupportedBitrate(true))
    }
    private val renderersFactory = DefaultRenderersFactory(context).apply {
        setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
    }

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context, renderersFactory)
        .setTrackSelector(trackSelector)
        .setSeekBackIncrementMs(10_000)
        .setSeekForwardIncrementMs(10_000)
        .build()

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var positionJob: Job? = null
    private var currentUri: Uri? = null
    private var scale = 1f
    private var aspect = "fit"

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }
            override fun onPlaybackStateChanged(s: Int) {
                _state.update {
                    it.copy(isLoading = s == Player.STATE_BUFFERING)
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                _state.update { it.copy(error = error.message, isLoading = false) }
            }
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                _state.update { it.copy(videoWidth = videoSize.width, videoHeight = videoSize.height) }
            }
            override fun onTracksChanged(tracks: Tracks) {
                _state.update {
                    it.copy(
                        audioTracks = audioTracks(tracks),
                        subtitleTracks = subtitleTracks(tracks),
                    )
                }
            }
        })
        positionJob = scope.launch(Dispatchers.Main) {
            while (true) {
                runCatching {
                    _state.update {
                        it.copy(
                            positionMs = exoPlayer.currentPosition.coerceAtLeast(0),
                            durationMs = exoPlayer.duration.takeIf { d -> d > 0 } ?: it.durationMs,
                            bufferedMs = exoPlayer.bufferedPosition.coerceAtLeast(0),
                            volume = exoPlayer.volume,
                            speed = exoPlayer.playbackParameters.speed,
                        )
                    }
                }
                delay(250)
            }
        }
    }

    override fun open(uri: Uri, startPositionMs: Long) {
        currentUri = uri
        _state.update {
            it.copy(
                isLoading = true, error = null, title = uri.lastPathSegment ?: uri.toString(),
                positionMs = startPositionMs, durationMs = 0L,
            )
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        if (startPositionMs > 0) exoPlayer.seekTo(startPositionMs)
        exoPlayer.play()
    }

    override fun play() { _state.update { it.copy(error = null) }; exoPlayer.play() }
    override fun pause() = exoPlayer.pause()
    override fun togglePlayPause() = if (exoPlayer.isPlaying) pause() else play()

    override fun seek(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceAtLeast(0))
        _state.update { it.copy(positionMs = positionMs.coerceAtLeast(0)) }
    }

    override fun seekBy(deltaMs: Long) = seek(exoPlayer.currentPosition + deltaMs)

    override fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
        if (volume > 0) _state.update { it.copy(muted = false) }
    }

    override fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed.coerceIn(0.25f, 4f))
    }

    override fun selectAudioTrack(id: Int?) {
        val group = currentTracks()?.groups?.firstOrNull {
            it.type == C.TRACK_TYPE_AUDIO
        } ?: return
        trackSelector.parameters = if (id == null) {
            trackSelector.parameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_AUDIO).build()
        } else {
            val tg = group.mediaTrackGroup
            val idx = (0 until tg.length).firstOrNull { tg.getFormat(it).id == id.toString() } ?: return
            trackSelector.parameters.buildUpon()
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, idx))
                .build()
        }
    }

    override fun selectSubtitleTrack(id: Int?) {
        val params = trackSelector.parameters.buildUpon()
        if (id == null) {
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            currentTracks()?.groups?.firstOrNull { it.type == C.TRACK_TYPE_TEXT }?.let { group ->
                val tg = group.mediaTrackGroup
                (0 until tg.length).firstOrNull { tg.getFormat(it).id == id.toString() }?.let { idx ->
                    params.setOverrideForType(TrackSelectionOverride(tg, idx))
                }
            }
        }
        trackSelector.parameters = params.build()
        _state.update { it.copy(subtitleTracks = it.subtitleTracks.map { t -> t.copy(selected = t.id == id) }) }
    }

    override fun setSubtitleDelay(delayMs: Long) {
        _state.update { it.copy(subtitleDelayMs = delayMs) }
        // ponytail: Media3 has no per-sub delay API; applied on libmpv swap. Stored for now.
    }

    override fun setAudioDelay(delayMs: Long) {
        _state.update { it.copy(audioDelayMs = delayMs) }
    }

    override fun setVideoScale(scale: Float) {
        this.scale = scale.coerceIn(0.5f, 4f)
        _state.update { it.copy(videoScale = this.scale) }
    }

    override fun setAspectRatio(ratio: String) {
        aspect = ratio
        _state.update { it.copy(aspectRatio = ratio) }
    }

    override fun setRepeatOne(enabled: Boolean) {
        exoPlayer.repeatMode = if (enabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    override fun attachVideoSurface(surface: Any?) {
        when (surface) {
            is SurfaceView -> exoPlayer.setVideoSurfaceView(surface)
            is TextureView -> exoPlayer.setVideoTextureView(surface)
            null -> { exoPlayer.clearVideoSurfaceView(null as SurfaceView?); exoPlayer.clearVideoTextureView(null as TextureView?) }
        }
    }

    override fun decoderInfo(): DecoderInfo {
        val format = exoPlayer.videoFormat ?: return DecoderInfo()
        val decoder = runCatching {
            val mc = android.media.MediaCodecList(android.media.MediaCodecList.ALL_CODECS)
            mc.codecInfos.firstOrNull { info ->
                runCatching {
                    info.isEncoder.not() && info.supportedTypes.any { t ->
                        format.sampleMimeType?.contains(t.split("/").last(), true) == true
                    }
                }.getOrDefault(false)
            }?.name
        }.getOrNull() ?: "–"
        val hw = !decoder.contains("OMX.google", true) && !decoder.contains("c2.android", true) && decoder != "–"
        return DecoderInfo(
            videoCodec = format.sampleMimeType?.substringAfter("/")?.uppercase() ?: "–",
            resolution = if (format.width > 0) "${format.width} × ${format.height}" else "–",
            fps = format.frameRate.takeIf { it > 0 } ?: 0f,
            bitrateMbps = (format.bitrate.takeIf { it > 0 } ?: 0) / 1_000_000.0,
            hardwareDecoding = if (decoder == "–") null else hw,
            decoderName = decoder,
        )
    }

    override fun release() {
        positionJob?.cancel()
        exoPlayer.release()
    }

    private fun currentTracks(): Tracks? = runCatching { exoPlayer.currentTracks }.getOrNull()

    private fun audioTracks(tracks: Tracks): List<TrackInfo> =
        tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }.flatMap { g ->
            (0 until g.mediaTrackGroup.length).map { i ->
                val f = g.mediaTrackGroup.getFormat(i)
                TrackInfo(
                    id = f.id?.toIntOrNull() ?: i,
                    label = f.label ?: f.language ?: "Audio ${i + 1}",
                    language = f.language,
                    selected = g.isTrackSelected(i),
                )
            }
        }

    private fun subtitleTracks(tracks: Tracks): List<TrackInfo> =
        tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }.flatMap { g ->
            (0 until g.mediaTrackGroup.length).map { i ->
                val f = g.mediaTrackGroup.getFormat(i)
                TrackInfo(
                    id = f.id?.toIntOrNull() ?: i,
                    label = f.label ?: f.language ?: "Subtitle ${i + 1}",
                    language = f.language,
                    selected = g.isTrackSelected(i),
                )
            }
        }
}
