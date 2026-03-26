package com.example.picker_photo.data.model

/**
 * Controls the initial screen shown by [PhotoPickerEntryPoint].
 * - [Combined]    → mode-selection screen (Camera OR Library).
 * - [GalleryOnly] → jumps directly to the gallery.
 * - [CameraOnly]  → launches the system camera immediately.
 */
sealed interface LaunchMode {
    data object Combined    : LaunchMode
    data object GalleryOnly : LaunchMode
    data class  CameraOnly(val type: CameraType = CameraType.PHOTO) : LaunchMode
}

enum class CameraType { PHOTO, VIDEO }
