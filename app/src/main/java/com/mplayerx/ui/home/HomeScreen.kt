package com.mplayerx.ui.home

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mplayerx.MPlayerXApp
import com.mplayerx.utils.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    app: MPlayerXApp,
    onOpenVideo: (Uri, Long) -> Unit,
    onBrowse: () -> Unit,
    onSettings: () -> Unit,
) {
    val items by app.mediaRepository.items.collectAsState()
    val recent by app.db.historyDao().recent(10).collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { runCatching { app.mediaRepository.refresh() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MPlayerX") },
                actions = {
                    IconButton(onClick = onBrowse) {
                        Icon(Icons.Default.Search, contentDescription = "Browse")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (recent.isNotEmpty()) {
                item {
                    Text("Continue Watching", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recent) { h ->
                            Card(
                                Modifier.width(160.dp).clickable {
                                    onOpenVideo(Uri.parse(h.uri), h.positionMs)
                                },
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Text(h.title, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                    val pct = if (h.durationMs > 0) (h.positionMs * 100 / h.durationMs).toInt() else 0
                                    Text("$pct% • ${formatTime(h.positionMs)}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Folders", style = MaterialTheme.typography.titleMedium)
                    Text("See all", Modifier.clickable { onBrowse() }, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                val folders = items.groupBy { it.folder }.toList().take(8)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    folders.forEach { (folder, vids) ->
                        Row(Modifier.fillMaxWidth().clickable { onBrowse() }.padding(8.dp)) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Text("$folder (${vids.size})")
                        }
                    }
                    if (folders.isEmpty()) Text("No videos found yet.", style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                Text("Recently added", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }
            val shown = items.take(12)
            items(shown.chunked(3)) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { v ->
                        Card(
                            Modifier.weight(1f).clickable { onOpenVideo(v.uri, -1) },
                        ) {
                            AsyncImage(
                                model = v.uri, contentDescription = null,
                                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                                contentScale = ContentScale.Crop,
                            )
                            Text(
                                v.name, maxLines = 1, style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(6.dp),
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
