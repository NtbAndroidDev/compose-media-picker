package com.example.picker_photo.data.model

import android.net.Uri

/**
 * Immutable domain model representing a single media item (photo or video).
 * Thread-safe by design — all fields are val.
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val dateTaken: Long,      // epoch-millis
    val durationMs: Long = 0  // > 0 for videos
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
}
