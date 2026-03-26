package com.example.picker_photo.ui.picker

import android.net.Uri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.example.picker_photo.data.model.LaunchMode
import com.example.picker_photo.data.model.PickerConfig
import com.example.picker_photo.data.model.SelectionMode

/**
 * A handy helper class for View-based projects (Activities/Fragments).
 * 
 * IMPORTANT: You MUST initialize this helper class BEFORE the Activity/Fragment
 * reaches the STARTED state (typically inside `onCreate` or `onAttach`).
 *
 * Example Usage:
 * ```
 * class MainActivity : AppCompatActivity() {
 *     private val pickerHelper = MediaPickerHelper(this) { uris ->
 *         uris?.forEach { Log.d("Media", "Picked: $it") }
 *     }
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         // ...
 *         pickerHelper.launchMultiple(maxSelection = 5)
 *     }
 * }
 * ```
 */
class MediaPickerHelper(
    caller: ActivityResultCaller,
    private val onResult: (List<Uri>?) -> Unit
) {
    private val launcher: ActivityResultLauncher<PickerConfig> =
        caller.registerForActivityResult(PhotoPickerContract()) { uris ->
            onResult(uris)
        }

    /**
     * Launch picker with custom [PickerConfig]
     */
    fun launch(config: PickerConfig = PickerConfig()) {
        launcher.launch(config)
    }

    /**
     * Convenient method to pick a single media item.
     * @param launchMode Choose between PhotoOnly, VideoOnly, or Combined (default).
     */
    fun launchSingle(launchMode: LaunchMode = LaunchMode.Combined) {
        launcher.launch(
            PickerConfig(
                selectionMode = SelectionMode.SINGLE,
                launchMode = launchMode
            )
        )
    }

    /**
     * Convenient method to pick multiple media items.
     * @param maxSelection Maximum number of items the user can select.
     * @param launchMode Choose between PhotoOnly, VideoOnly, or Combined (default).
     */
    fun launchMultiple(maxSelection: Int = 10, launchMode: LaunchMode = LaunchMode.Combined) {
        launcher.launch(
            PickerConfig(
                selectionMode = SelectionMode.MULTIPLE,
                maxSelectionCount = maxSelection,
                launchMode = launchMode
            )
        )
    }
}
