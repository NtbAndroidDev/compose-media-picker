package com.example.picker_photo.ui.picker

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.picker_photo.data.model.CameraType
import com.example.picker_photo.data.model.LaunchMode
import com.example.picker_photo.data.model.PickerConfig
import com.example.picker_photo.data.model.SelectionMode

/**
 * Composable-friendly handle returned by [rememberMediaPicker].
 * Call the convenience methods to launch the picker from anywhere in your UI.
 */
class MediaPickerState internal constructor(
    private val launch: (PickerConfig) -> Unit
) {
    /** Launch with a fully custom [PickerConfig]. */
    fun launch(config: PickerConfig = PickerConfig()) = launch.invoke(config)

    /** 🎯 Open the full picker (Gallery + Camera). */
    fun openPicker(maxSelection: Int = 1) = launch(
        PickerConfig(
            selectionMode     = if (maxSelection > 1) SelectionMode.MULTIPLE else SelectionMode.SINGLE,
            maxSelectionCount = maxSelection,
            launchMode        = LaunchMode.Combined
        )
    )

    /** 🖼️ Open the Gallery only. */
    fun openGallery(maxSelection: Int = 1) = launch(
        PickerConfig(
            selectionMode     = if (maxSelection > 1) SelectionMode.MULTIPLE else SelectionMode.SINGLE,
            maxSelectionCount = maxSelection,
            launchMode        = LaunchMode.GalleryOnly
        )
    )

    /** 📸 Open Camera to take a photo. */
    fun takePhoto() = launch(
        PickerConfig(
            selectionMode = SelectionMode.SINGLE,
            launchMode    = LaunchMode.CameraOnly(CameraType.PHOTO)
        )
    )

    /** 🎥 Open Camera to record a video. */
    fun recordVideo() = launch(
        PickerConfig(
            selectionMode = SelectionMode.SINGLE,
            launchMode    = LaunchMode.CameraOnly(CameraType.VIDEO)
        )
    )
}

/**
 * **Compose API for the Media Picker.**
 *
 * Register once inside your composable — similar to [rememberLauncherForActivityResult].
 * Use the returned [MediaPickerState] to launch the picker from any click handler.
 *
 * Example:
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val picker = rememberMediaPicker { uris ->
 *         uris?.forEach { Log.d("Picker", "$it") }
 *     }
 *
 *     Button(onClick = { picker.openPicker(maxSelection = 5) }) {
 *         Text("Pick Media")
 *     }
 * }
 * ```
 *
 * @param onResult  Called with the selected [Uri] list, or `null` if cancelled.
 */
@Composable
fun rememberMediaPicker(
    onResult: (List<Uri>?) -> Unit
): MediaPickerState {
    val launcher = rememberLauncherForActivityResult(PhotoPickerContract()) { uris ->
        onResult(uris)
    }
    return remember { MediaPickerState(launch = { config -> launcher.launch(config) }) }
}
