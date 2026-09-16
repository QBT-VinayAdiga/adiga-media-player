package com.mplayerx.playback

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Milestone 2 placeholder. Same [PlayerEngine] contract, backed by
 * JNI/libmpv once native prebuilts land in app/src/main/jniLibs + native/
 * bridge (see ARCHITECTURE.md § "libmpv swap").
 *
 * Kept (not deleted) so the swap is a one-line DI change in MPlayerXApp.
 */
class MpvPlayerEngine : PlayerEngine {
    private val _state = MutableStateFlow(PlayerState(error = "libmpv not bundled in this MVP build"))
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private fun todo(): Nothing =
        error("MpvPlayerEngine: native libmpv not bundled yet — use ExoPlayerEngine (default).")

    override fun open(uri: Uri, startPositionMs: Long): Nothing = todo()
    override fun play(): Nothing = todo()
    override fun pause(): Nothing = todo()
    override fun togglePlayPause(): Nothing = todo()
    override fun seek(positionMs: Long): Nothing = todo()
    override fun seekBy(deltaMs: Long): Nothing = todo()
    override fun setVolume(volume: Float): Nothing = todo()
    override fun setPlaybackSpeed(speed: Float): Nothing = todo()
    override fun selectAudioTrack(id: Int?): Nothing = todo()
    override fun selectSubtitleTrack(id: Int?): Nothing = todo()
    override fun setSubtitleDelay(delayMs: Long): Nothing = todo()
    override fun setAudioDelay(delayMs: Long): Nothing = todo()
    override fun setVideoScale(scale: Float): Nothing = todo()
    override fun setAspectRatio(ratio: String): Nothing = todo()
    override fun setMirrorHorizontal(enabled: Boolean): Nothing = todo()
    override fun setRepeatOne(enabled: Boolean): Nothing = todo()
    override fun attachVideoSurface(surface: Any?): Nothing = todo()
    override fun decoderInfo(): DecoderInfo = DecoderInfo(decoderName = "libmpv (not bundled)")
    override fun release() = Unit
}
