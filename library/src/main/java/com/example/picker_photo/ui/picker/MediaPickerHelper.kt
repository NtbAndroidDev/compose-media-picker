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
     * Launch with custom [PickerConfig]
     */
    fun launch(config: PickerConfig = PickerConfig()) {
        launcher.launch(config)
    }

    /**
     * 🖼️ OPTION: Mở Thư viện (Gallery) để chọn ảnh/video
     * @param maxSelection Số lượng tối đa (Mặc định: 1)
     */
    fun openGallery(maxSelection: Int = 1) {
        val selectionMode = if (maxSelection > 1) SelectionMode.MULTIPLE else SelectionMode.SINGLE
        launcher.launch(
            PickerConfig(
                selectionMode = selectionMode,
                maxSelectionCount = maxSelection,
                launchMode = LaunchMode.GalleryOnly
            )
        )
    }

    /**
     * 📸 OPTION: Mở Camera để CHỤP ẢNH
     */
    fun takePhoto() {
        launcher.launch(
            PickerConfig(
                selectionMode = SelectionMode.SINGLE,
                launchMode = LaunchMode.CameraOnly(com.example.picker_photo.data.model.CameraType.PHOTO)
            )
        )
    }

    /**
     * 🎥 OPTION: Mở Camera để QUAY VIDEO
     */
    fun recordVideo() {
        launcher.launch(
            PickerConfig(
                selectionMode = SelectionMode.SINGLE,
                launchMode = LaunchMode.CameraOnly(com.example.picker_photo.data.model.CameraType.VIDEO)
            )
        )
    }

    /**
     * ⚙️ OPTION: Mở giao diện Đầy Đủ (Combined - Cho phép chọn cả Gallery và Camera)
     */
    fun openPicker(maxSelection: Int = 1) {
        val selectionMode = if (maxSelection > 1) SelectionMode.MULTIPLE else SelectionMode.SINGLE
        launcher.launch(
            PickerConfig(
                selectionMode = selectionMode,
                maxSelectionCount = maxSelection,
                launchMode = LaunchMode.Combined
            )
        )
    }
}
