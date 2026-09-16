package com.mplayerx.media

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory cache over MediaScanner; Room holds only history/resume. */
class MediaRepository(context: Context) {
    private val scanner = MediaScanner(context)
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    suspend fun refresh() {
        _items.value = scanner.scanAll()
    }
}
