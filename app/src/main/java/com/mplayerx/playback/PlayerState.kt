package com.mplayerx.playback

data class PlayerState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val speed: Float = 1f,
    val volume: Float = 1f,
    val muted: Boolean = false,
    val aspectRatio: String = "fit",
    val videoScale: Float = 1f,
    val mirrorHorizontal: Boolean = false,
    val repeatOne: Boolean = false,
    val audioTracks: List<TrackInfo> = emptyList(),
    val subtitleTracks: List<TrackInfo> = emptyList(),
    val subtitleDelayMs: Long = 0L,
    val audioDelayMs: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null,
    val title: String = "",
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
)
