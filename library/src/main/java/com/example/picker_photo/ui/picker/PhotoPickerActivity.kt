package com.example.picker_photo.ui.picker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.picker_photo.data.model.CameraType
import com.example.picker_photo.data.model.LaunchMode
import com.example.picker_photo.data.model.MediaFilter
import com.example.picker_photo.data.model.PickerConfig
import com.example.picker_photo.data.model.PickerResult
import com.example.picker_photo.data.model.SelectionMode
import com.example.picker_photo.ui.theme.PickerphotoTheme

/**
 * Activity host for the picker — enables use from **any** project type
 * (View-based, XML fragments, other activities) via [PhotoPickerContract].
 *
 * Do **not** start this activity directly; use [PhotoPickerContract] instead.
 */
class PhotoPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val config = intent.toPickerConfig()

        setContent {
            PickerphotoTheme {
                PhotoPickerEntryPoint(
                    config   = config,
                    onResult = { result ->
                        when (result) {
                            is PickerResult.Selected -> {
                                val data = Intent().apply {
                                    putParcelableArrayListExtra(
                                        PhotoPickerContract.KEY_URIS,
                                        ArrayList(result.uris)
                                    )
                                }
                                setResult(Activity.RESULT_OK, data)
                            }
                            PickerResult.Cancelled ->
                                setResult(Activity.RESULT_CANCELED)
                        }
                        finish()
                    }
                )
            }
        }
    }

    // System back → RESULT_CANCELED (ComponentActivity default behaviour)
}

// ── Intent ↔ PickerConfig helpers (internal) ─────────────────────────────────

internal fun Intent.toPickerConfig(): PickerConfig = PickerConfig(
    launchMode        = launchModeFromCode(getIntExtra(PhotoPickerContract.KEY_LAUNCH_MODE, 0)),
    selectionMode     = if (getIntExtra(PhotoPickerContract.KEY_SELECTION_MODE, 1) == 0)
                            SelectionMode.SINGLE else SelectionMode.MULTIPLE,
    initialFilter     = MediaFilter.entries.getOrElse(
                            getIntExtra(PhotoPickerContract.KEY_FILTER, 0)) { MediaFilter.ALL },
    maxSelectionCount = getIntExtra(PhotoPickerContract.KEY_MAX_COUNT, Int.MAX_VALUE),
    allowFilterChange = getBooleanExtra(PhotoPickerContract.KEY_ALLOW_FILTER, true)
)

internal fun LaunchMode.toCode(): Int = when (this) {
    LaunchMode.Combined                             -> 0
    LaunchMode.GalleryOnly                          -> 1
    is LaunchMode.CameraOnly -> if (type == CameraType.VIDEO) 3 else 2
}

internal fun launchModeFromCode(code: Int): LaunchMode = when (code) {
    1    -> LaunchMode.GalleryOnly
    2    -> LaunchMode.CameraOnly(CameraType.PHOTO)
    3    -> LaunchMode.CameraOnly(CameraType.VIDEO)
    else -> LaunchMode.Combined
}
