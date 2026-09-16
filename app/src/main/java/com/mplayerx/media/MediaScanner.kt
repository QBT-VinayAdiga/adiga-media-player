package com.mplayerx.media

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** MediaStore video discovery. Never on main thread (requirements §23). */
class MediaScanner(private val context: Context) {

    suspend fun scanAll(): List<MediaItem> = withContext(Dispatchers.IO) {
        val out = mutableListOf<MediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val proj = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
        )
        context.contentResolver.query(
            collection, proj, null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { c ->
            val idC = c.getColumnIndexOrThrow(proj[0])
            val nameC = c.getColumnIndexOrThrow(proj[1])
            val durC = c.getColumnIndexOrThrow(proj[2])
            val sizeC = c.getColumnIndexOrThrow(proj[3])
            val dateC = c.getColumnIndexOrThrow(proj[4])
            val dataC = c.getColumnIndexOrThrow(proj[5])
            val wC = c.getColumnIndexOrThrow(proj[6])
            val hC = c.getColumnIndexOrThrow(proj[7])
            while (c.moveToNext()) {
                val id = c.getLong(idC)
                val path: String = runCatching { c.getString(dataC) }.getOrNull() ?: ""
                val name = c.getString(nameC) ?: "Video $id"
                out += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    name = name,
                    folder = path.substringBeforeLast('/', "Videos").substringAfterLast('/', "Videos"),
                    durationMs = runCatching { c.getLong(durC) }.getOrDefault(0),
                    sizeBytes = runCatching { c.getLong(sizeC) }.getOrDefault(0),
                    dateAddedSec = runCatching { c.getLong(dateC) }.getOrDefault(0),
                    width = runCatching { c.getInt(wC) }.getOrDefault(0),
                    height = runCatching { c.getInt(hC) }.getOrDefault(0),
                )
            }
        }
        out
    }
}
