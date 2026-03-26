package com.example.picker_photo.data.model

/**
 * Single configuration object — the public API for library consumers.
 *
 * ```kotlin
 * PickerConfig(
 *     launchMode        = LaunchMode.Combined,
 *     selectionMode     = SelectionMode.MULTIPLE,
 *     initialFilter     = MediaFilter.ALL,
 *     maxSelectionCount = 10,
 *     allowFilterChange = true
 * )
 * ```
 */
data class PickerConfig(
    val launchMode: LaunchMode      = LaunchMode.Combined,
    val selectionMode: SelectionMode = SelectionMode.MULTIPLE,
    val initialFilter: MediaFilter  = MediaFilter.ALL,
    val maxSelectionCount: Int      = Int.MAX_VALUE,
    val allowFilterChange: Boolean  = true
)
