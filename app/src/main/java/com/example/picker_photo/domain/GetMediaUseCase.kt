package com.example.picker_photo.domain

import com.example.picker_photo.data.model.MediaFilter
import com.example.picker_photo.data.model.MediaItem
import com.example.picker_photo.data.repository.MediaRepository

class GetMediaUseCase(private val repository: MediaRepository) {
    suspend operator fun invoke(filter: MediaFilter = MediaFilter.ALL): List<MediaItem> =
        repository.getMedia(filter)
}
