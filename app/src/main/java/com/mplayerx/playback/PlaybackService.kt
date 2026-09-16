package com.mplayerx.playback

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** Background audio: keeps audio going with screen off + notification/controls. */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as com.mplayerx.MPlayerXApp
        val player = (app.controller.engine as? ExoPlayerEngine)?.exoPlayer ?: return
        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
