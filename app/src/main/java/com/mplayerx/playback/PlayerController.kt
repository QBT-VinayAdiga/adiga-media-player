package com.mplayerx.playback

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Thin lifecycle owner for the engine: open/save/resume bookkeeping lives
 * here so screens stay dumb. One instance per process (see MPlayerXApp).
 */
class PlayerController(val engine: PlayerEngine) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val state: StateFlow<PlayerState> = engine.state

    var onPositionSave: ((uri: Uri, positionMs: Long, durationMs: Long) -> Unit)? = null
    private var currentUri: Uri? = null

    fun open(uri: Uri, startPositionMs: Long = 0L) {
        currentUri = uri
        engine.open(uri, startPositionMs)
    }

    fun toggle() = engine.togglePlayPause()
    fun saveNow() {
        val u = currentUri ?: return
        val s = engine.state.value
        if (s.durationMs > 0) onPositionSave?.invoke(u, s.positionMs, s.durationMs)
    }

    fun release() {
        scope.launch { saveNow() }
        engine.release()
    }
}
