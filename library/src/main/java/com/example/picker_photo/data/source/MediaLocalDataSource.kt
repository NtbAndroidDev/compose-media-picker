package com.example.picker_photo.data.source

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore
import com.example.picker_photo.data.model.MediaFilter
import com.example.picker_photo.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaLocalDataSource(private val contentResolver: ContentResolver) {

    suspend fun fetchAllMedia(limit: Int = 300): List<MediaItem> =
        fetchMedia(MediaFilter.ALL, limit)

    suspend fun fetchMedia(filter: MediaFilter, limit: Int = 300): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<MediaItem>()

            if (filter != MediaFilter.VIDEOS_ONLY) {
                items += queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            }
            if (filter != MediaFilter.IMAGES_ONLY) {
                items += queryMedia(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    withDuration = true
                )
            }

            items.sortedByDescending { it.dateTaken }.take(limit)
        }

    private fun queryMedia(
        collection: android.net.Uri,
        withDuration: Boolean = false
    ): List<MediaItem> {
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.DATE_TAKEN)
            if (withDuration) add(MediaStore.Video.VideoColumns.DURATION)
        }.toTypedArray()

        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC"

        return contentResolver.query(collection, projection, null, null, sortOrder)
            ?.use { cursor -> cursor.toMediaItems(collection, withDuration) }
            ?: emptyList()
    }

    private fun Cursor.toMediaItems(
        collection: android.net.Uri,
        withDuration: Boolean
    ): List<MediaItem> {
        val idCol       = getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val nameCol     = getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val mimeCol     = getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
        val dateCol     = getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
        val durationCol = if (withDuration) getColumnIndex(MediaStore.Video.VideoColumns.DURATION) else -1

        val result = mutableListOf<MediaItem>()
        while (moveToNext()) {
            val id = getLong(idCol)
            result += MediaItem(
                id          = id,
                uri         = ContentUris.withAppendedId(collection, id),
                displayName = getString(nameCol) ?: "",
                mimeType    = getString(mimeCol) ?: "image/jpeg",
                dateTaken   = getLong(dateCol),
                durationMs  = if (durationCol >= 0) getLong(durationCol) else 0L
            )
        }
        return result
    }
}
