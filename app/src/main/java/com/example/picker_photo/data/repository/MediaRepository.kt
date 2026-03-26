package com.example.picker_photo.data.repository

import com.example.picker_photo.data.model.MediaFilter
import com.example.picker_photo.data.model.MediaItem
import com.example.picker_photo.data.source.MediaLocalDataSource

class MediaRepository(private val localDataSource: MediaLocalDataSource) {

    suspend fun getAllMedia(limit: Int = 300): List<MediaItem> =
        localDataSource.fetchAllMedia(limit)

    suspend fun getMedia(filter: MediaFilter, limit: Int = 300): List<MediaItem> =
        localDataSource.fetchMedia(filter, limit)
}
