package com.mplayerx.ui.browser

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mplayerx.MPlayerXApp
import com.mplayerx.ui.VideoThumbnail
import com.mplayerx.utils.formatSize
import com.mplayerx.utils.formatTime

private enum class Sort { NAME, DATE, SIZE, DURATION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    app: MPlayerXApp,
    folder: String?,
    onOpenVideo: (Uri, Long) -> Unit,
    onBack: () -> Unit,
) {
    val items by app.mediaRepository.items.collectAsState()
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(Sort.DATE) }
    var grid by remember { mutableStateOf(true) }
    var sortMenu by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { runCatching { app.mediaRepository.refresh() } }

    val filtered = remember(items, query, sort, folder) {
        var list = items
        if (folder != null) list = list.filter { it.folder == folder }
        if (query.isNotBlank()) list =
            list.filter { it.name.contains(query, true) || it.folder.contains(query, true) }
        list = when (sort) {
            Sort.NAME -> list.sortedBy { it.name.lowercase() }
            Sort.DATE -> list.sortedByDescending { it.dateAddedSec }
            Sort.SIZE -> list.sortedByDescending { it.sizeBytes }
            Sort.DURATION -> list.sortedByDescending { it.durationMs }
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${folder ?: "Videos"} (${filtered.size})") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { grid = !grid }) { Icon(Icons.Default.List, null) }
                    TextButton(onClick = { sortMenu = true }) { Text(sort.name) }
                    DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                        Sort.entries.forEach {
                            DropdownMenuItem(text = { Text(it.name) }, onClick = { sort = it; sortMenu = false })
                        }
                    }
                    IconButton(onClick = {
                        refreshing = true
                    }) { Icon(Icons.Default.Refresh, null) }
                },
            )
        },
    ) { pad ->
        LaunchedEffect(refreshing) {
            if (refreshing) { runCatching { app.mediaRepository.refresh() }; refreshing = false }
        }
        Column(Modifier.fillMaxSize().padding(pad)) {
            TextField(
                query, { query = it }, Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("Search name or folder") }, singleLine = true,
            )
            if (grid) {
                LazyVerticalGrid(
                    GridCells.Fixed(3), Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered, key = { it.id }) { v ->
                        Card(Modifier.clickable { onOpenVideo(v.uri, -1) }) {
                            VideoThumbnail(
                                uri = v.uri,
                                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                            )
                            Column(Modifier.padding(6.dp)) {
                                Text(v.name, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                                Text(formatTime(v.durationMs), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
                    items(filtered, key = { it.id }) { v ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenVideo(v.uri, -1) }.padding(8.dp),
                        ) {
                            VideoThumbnail(
                                uri = v.uri,
                                modifier = Modifier.weight(0.35f).aspectRatio(16f / 9f),
                            )
                            Spacer(Modifier.height(0.dp))
                            Column(Modifier.weight(0.65f).padding(start = 12.dp)) {
                                Text(v.name, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${v.folder} • ${formatTime(v.durationMs)} • ${formatSize(v.sizeBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
