package com.mplayerx.utils

import java.util.Locale
import kotlin.math.abs

fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
    else String.format(Locale.US, "%02d:%02d", m, sec)
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "–"
    val kb = 1024.0; val mb = kb * 1024; val gb = mb * 1024
    return when {
        bytes >= gb -> "%.1f GB".format(bytes / gb)
        bytes >= mb -> "%.0f MB".format(bytes / mb)
        bytes >= kb -> "%.0f KB".format(bytes / kb)
        else -> "$bytes B"
    }
}

fun formatDelta(ms: Long): String {
    val s = abs(ms) / 1000
    return (if (ms >= 0) "+" else "−") + s + "s"
}
