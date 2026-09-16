package com.mplayerx

import android.app.Application
import android.net.Uri
import com.mplayerx.database.AppDatabase
import com.mplayerx.database.PlaybackHistory
import com.mplayerx.media.MediaRepository
import com.mplayerx.playback.ExoPlayerEngine
import com.mplayerx.playback.PlayerController
import com.mplayerx.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual DI (requirements allow lightweight manual DI instead of Hilt). */
class MPlayerXApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var controller: PlayerController
        private set
    lateinit var mediaRepository: MediaRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var db: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.get(this)
        settingsRepository = SettingsRepository(this)
        mediaRepository = MediaRepository(this)
        val engine = ExoPlayerEngine(this, appScope)
        // Swap to MpvPlayerEngine() here once Milestone 2 JNI lands.
        controller = PlayerController(engine)
        controller.onPositionSave = { uri: Uri, pos: Long, dur: Long ->
            appScope.launch(Dispatchers.IO) {
                // ponytail: skip saving near-start/near-end positions
                if (dur > 0 && pos > 5_000 && pos < dur - 10_000) {
                    db.historyDao().upsert(
                        PlaybackHistory(
                            uri = uri.toString(),
                            title = uri.lastPathSegment ?: uri.toString(),
                            positionMs = pos,
                            durationMs = dur,
                        )
                    )
                }
            }
        }
    }
}
