package com.example.picker_photo.ui.picker

import android.net.Uri
import com.example.picker_photo.data.model.CameraType
import com.example.picker_photo.data.model.MediaFilter
import com.example.picker_photo.data.model.MediaItem
import com.example.picker_photo.data.model.PickerConfig
import com.example.picker_photo.data.model.SelectionMode

sealed interface PhotoPickerUiState {

    data object Loading            : PhotoPickerUiState
    data object PermissionRequired : PhotoPickerUiState
    data class Error(val message: String) : PhotoPickerUiState

    /** Initial entry screen for [LaunchMode.Combined]. */
    data class ModeEntry(
        val showCameraSheet: Boolean = false
    ) : PhotoPickerUiState

    /**
     * Gallery grid.
     * [selectionOrder] keeps IDs in tap order so we can render numbered badges.
     */
    data class Gallery(
        val mediaItems: List<MediaItem>,
        val selectionOrder: List<Long> = emptyList(),   // preserves insertion order
        val activeFilter: MediaFilter,
        val config: PickerConfig,
        val showCameraSheet: Boolean = false
    ) : PhotoPickerUiState {
        val selectedIds: Set<Long>     get() = selectionOrder.toSet()
        val confirmEnabled: Boolean    get() = selectionOrder.isNotEmpty()
        val selectedCount: Int         get() = selectionOrder.size
        fun isSelected(item: MediaItem)     = item.id in selectedIds
        /** 1-based index in the selection order, or null if not selected. */
        fun orderOf(item: MediaItem): Int?  = selectionOrder.indexOf(item.id).takeIf { it >= 0 }?.plus(1)
        val selectedItems: List<MediaItem>
            get() = selectionOrder.mapNotNull { id -> mediaItems.find { it.id == id } }
    }

    /**
     * Full-screen preview after a camera capture.
     * [cameraType] is stored so "Retake" re-launches the same camera mode.
     * [showCropper] when true, overlays [ImageCropperScreen] on top of the preview.
     */
    data class Preview(
        val uri: Uri,
        val isVideo: Boolean,
        val cameraType: CameraType,
        val showCropper: Boolean = false
    ) : PhotoPickerUiState
}

sealed interface PhotoPickerEvent {
    data object LaunchCameraPhoto            : PhotoPickerEvent
    data object LaunchCameraVideo            : PhotoPickerEvent
    data class  Deliver(val uris: List<Uri>) : PhotoPickerEvent
    data object Cancelled                    : PhotoPickerEvent
}
