package com.mplayerx.playback

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

/**
 * UI-facing playback abstraction. The rest of the app depends on this,
 * never on libmpv / ExoPlayer directly (requirements §6, §23).
 *
 * Milestone 2 note: MpvPlayerEngine will implement this same interface over
 * JNI/libmpv. ExoPlayerEngine is the MVP engine (HW decode, AV1 via MediaCodec).
 */
interface PlayerEngine {
    val state: StateFlow<PlayerState>
    fun open(uri: Uri, startPositionMs: Long = 0L)
    fun play()
    fun pause()
    fun togglePlayPause()
    fun seek(positionMs: Long)
    fun seekBy(deltaMs: Long)
    fun setVolume(volume: Float) // 0..1
    fun setPlaybackSpeed(speed: Float)
    fun selectAudioTrack(id: Int?)
    fun selectSubtitleTrack(id: Int?)
    fun setSubtitleDelay(delayMs: Long)
    fun setAudioDelay(delayMs: Long)
    fun setVideoScale(scale: Float)
    fun setAspectRatio(ratio: String) // e.g. "fit", "fill", "16:9", "4:3"
    fun setRepeatOne(enabled: Boolean)
    fun attachVideoSurface(surface: Any?)
    fun decoderInfo(): DecoderInfo
    fun release()
}

data class TrackInfo(
    val id: Int,
    val label: String,
    val language: String? = null,
    val selected: Boolean = false,
)

data class DecoderInfo(
    val videoCodec: String = "–",
    val resolution: String = "–",
    val fps: Float = 0f,
    val bitrateMbps: Double = 0.0,
    val hardwareDecoding: Boolean? = null, // null = unknown
    val decoderName: String = "–",
    val renderer: String = "MediaCodec / GPU",
)
