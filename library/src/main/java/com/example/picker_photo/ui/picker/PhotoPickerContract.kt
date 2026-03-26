package com.example.picker_photo.ui.picker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.example.picker_photo.data.model.PickerConfig
import com.example.picker_photo.data.model.SelectionMode

/**
 * **ActivityResultContract for the picker — works in ANY Android project.**
 *
 * ### Usage (View-based / Fragment / Activity)
 * ```kotlin
 * // 1. Register (in onCreate or field level)
 * val pickMedia = registerForActivityResult(PhotoPickerContract()) { uris ->
 *     if (uris != null) {
 *         // use selected URIs
 *     }
 * }
 *
 * // 2. Launch
 * pickMedia.launch(
 *     PickerConfig(
 *         selectionMode     = SelectionMode.MULTIPLE,
 *         maxSelectionCount = 10
 *     )
 * )
 * ```
 *
 * ### Usage (Compose)
 * ```kotlin
 * val launcher = rememberLauncherForActivityResult(PhotoPickerContract()) { uris ->
 *     if (uris != null) use(uris)
 * }
 * launcher.launch(PickerConfig())
 * ```
 *
 * ### Returns
 * - `List<Uri>` — selected media URIs (empty list if none selected)
 * - `null` — user cancelled
 */
class PhotoPickerContract : ActivityResultContract<PickerConfig, List<Uri>?>() {

    override fun createIntent(context: Context, input: PickerConfig): Intent =
        Intent(context, PhotoPickerActivity::class.java).apply {
            putExtra(KEY_LAUNCH_MODE,    input.launchMode.toCode())
            putExtra(KEY_SELECTION_MODE, if (input.selectionMode == SelectionMode.SINGLE) 0 else 1)
            putExtra(KEY_FILTER,         input.initialFilter.ordinal)
            putExtra(KEY_MAX_COUNT,      input.maxSelectionCount)
            putExtra(KEY_ALLOW_FILTER,   input.allowFilterChange)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri>? {
        if (resultCode != Activity.RESULT_OK) return null
        @Suppress("DEPRECATION")
        return intent?.getParcelableArrayListExtra(KEY_URIS) ?: emptyList()
    }

    companion object {
        internal const val KEY_LAUNCH_MODE    = "picker_launch_mode"
        internal const val KEY_SELECTION_MODE = "picker_selection_mode"
        internal const val KEY_FILTER         = "picker_filter"
        internal const val KEY_MAX_COUNT      = "picker_max_count"
        internal const val KEY_ALLOW_FILTER   = "picker_allow_filter"
        internal const val KEY_URIS           = "picker_uris"
    }
}
