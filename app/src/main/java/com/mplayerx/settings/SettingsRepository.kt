package com.mplayerx.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("mplayerx")

enum class ResumeMode { ALWAYS, ASK, NEVER }

data class AppSettings(
    val resumeMode: ResumeMode = ResumeMode.ASK,
    val defaultSpeed: Float = 1f,
    val defaultAspect: String = "fit",
    val seekSeconds: Int = 10,
    val backgroundAudio: Boolean = true,
    val autoRotate: Boolean = true,
    val subtitleSize: Float = 16f,
    val controlsTimeoutMs: Long = 3000,
)

class SettingsRepository(private val context: Context) {
    private object K {
        val resume = stringPreferencesKey("resume")
        val speed = floatPreferencesKey("speed")
        val aspect = stringPreferencesKey("aspect")
        val seek = floatPreferencesKey("seek_s")
        val bg = booleanPreferencesKey("bg_audio")
        val rotate = booleanPreferencesKey("auto_rotate")
        val subSize = floatPreferencesKey("sub_size")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            resumeMode = runCatching { ResumeMode.valueOf(p[K.resume] ?: "ASK") }.getOrDefault(ResumeMode.ASK),
            defaultSpeed = p[K.speed] ?: 1f,
            defaultAspect = p[K.aspect] ?: "fit",
            seekSeconds = (p[K.seek] ?: 10f).toInt(),
            backgroundAudio = p[K.bg] ?: true,
            autoRotate = p[K.rotate] ?: false,
            subtitleSize = p[K.subSize] ?: 16f,
        )
    }

    suspend fun setResume(m: ResumeMode) = context.dataStore.edit { it[K.resume] = m.name }
    suspend fun setSpeed(v: Float) = context.dataStore.edit { it[K.speed] = v }
    suspend fun setAspect(v: String) = context.dataStore.edit { it[K.aspect] = v }
    suspend fun setSeekSeconds(v: Int) = context.dataStore.edit { it[K.seek] = v.toFloat() }
    suspend fun setBackgroundAudio(v: Boolean) = context.dataStore.edit { it[K.bg] = v }
    suspend fun setAutoRotate(v: Boolean) = context.dataStore.edit { it[K.rotate] = v }
    suspend fun setSubtitleSize(v: Float) = context.dataStore.edit { it[K.subSize] = v }
}
