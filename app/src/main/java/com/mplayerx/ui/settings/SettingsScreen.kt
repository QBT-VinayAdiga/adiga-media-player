package com.mplayerx.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mplayerx.MPlayerXApp
import com.mplayerx.settings.AppSettings
import com.mplayerx.settings.ResumeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: MPlayerXApp, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settings by app.settingsRepository.settings.collectAsState(initial = AppSettings())
    var resumeMenu by remember { mutableStateOf(false) }

    fun update(fn: suspend () -> Unit) = scope.launch { fn() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Playback", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Resume playback", Modifier.weight(1f))
                    TextButton(onClick = { resumeMenu = true }) { Text(settings.resumeMode.name) }
                    DropdownMenu(resumeMenu, onDismissRequest = { resumeMenu = false }) {
                        ResumeMode.entries.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.name) },
                                onClick = {
                                    update { app.settingsRepository.setResume(m) }
                                    resumeMenu = false
                                },
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Background audio", Modifier.weight(1f))
                    Switch(settings.backgroundAudio, { update { app.settingsRepository.setBackgroundAudio(it) } })
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto rotate", Modifier.weight(1f))
                    Switch(settings.autoRotate, { update { app.settingsRepository.setAutoRotate(it) } })
                }
                Text("Double-tap seek: ${settings.seekSeconds}s")
                Slider(
                    settings.seekSeconds.toFloat(), { update { app.settingsRepository.setSeekSeconds(it.toInt()) } },
                    valueRange = 5f..30f, steps = 4,
                )
            }
            item {
                Text("Subtitles", style = MaterialTheme.typography.titleMedium)
                Text("Size: ${settings.subtitleSize.toInt()}sp")
                Slider(
                    settings.subtitleSize, { update { app.settingsRepository.setSubtitleSize(it) } },
                    valueRange = 10f..30f,
                )
            }
            item {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Column {
                    Text("MPlayerX 0.1.0-mvp", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Engine: Media3/ExoPlayer (HW decode). libmpv swap: Milestone 2.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
