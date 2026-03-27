package com.example.picker_photo.ui.picker

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.picker_photo.data.model.PickerConfig
import com.example.picker_photo.data.model.PickerResult
import com.example.picker_photo.data.repository.MediaRepository
import com.example.picker_photo.data.source.MediaLocalDataSource
import com.example.picker_photo.domain.GetMediaUseCase
import java.io.File

/**
 * **Public entry-point composable — use this in your Activity or NavHost.**
 *
 * Handles permissions, camera launchers, FileProvider, and ViewModel internally.
 *
 * ```kotlin
 * PhotoPickerEntryPoint(
 *     config   = PickerConfig(launchMode = LaunchMode.Combined),
 *     onResult = { result -> if (result is PickerResult.Selected) use(result.uris) }
 * )
 * ```
 */
@Composable
fun PhotoPickerEntryPoint(
    config: PickerConfig = PickerConfig(),
    onResult: (PickerResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val viewModel: PhotoPickerViewModel = viewModel(
        factory = remember(config) {
            val ds   = MediaLocalDataSource(context.contentResolver)
            val repo = MediaRepository(ds)
            val uc   = GetMediaUseCase(repo)
            PhotoPickerViewModel.Factory(uc, config)
        }
    )

    // Pending camera URI (created before launching the camera; read back in the result callback)
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingPhotoUri?.let { viewModel.onCameraPhotoResult(it) }
        else viewModel.onCameraResultCancelled()
    }

    val takeVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) pendingVideoUri?.let { viewModel.onCameraVideoResult(it) }
        else viewModel.onCameraResultCancelled()
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) viewModel.onPermissionGranted()
        else viewModel.onPermissionDenied()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredPermissions())
    }

    // One-shot event bus
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                PhotoPickerEvent.LaunchCameraPhoto -> {
                    pendingPhotoUri = createTempMediaUri(context, isVideo = false)
                    pendingPhotoUri?.let { takePhotoLauncher.launch(it) }
                }
                PhotoPickerEvent.LaunchCameraVideo -> {
                    pendingVideoUri = createTempMediaUri(context, isVideo = true)
                    pendingVideoUri?.let { takeVideoLauncher.launch(it) }
                }
                is PhotoPickerEvent.Deliver  -> onResult(PickerResult.Selected(event.uris))
                PhotoPickerEvent.Cancelled   -> onResult(PickerResult.Cancelled)
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    PhotoPickerScreen(
        uiState              = uiState,
        onItemToggled        = viewModel::onMediaItemToggled,
        onConfirm            = viewModel::onConfirmSelection,
        onClear              = viewModel::onClearSelection,
        onRetry              = viewModel::onRetry,
        onGallerySelected    = viewModel::onGallerySelected,
        onCameraButtonTapped = viewModel::onCameraButtonTapped,
        onCameraSheetDismiss = viewModel::onCameraSheetDismiss,
        onTakePhoto          = viewModel::onTakePhoto,
        onRecordVideo        = viewModel::onRecordVideo,
        onFilterChanged      = viewModel::onFilterChanged,
        onPreviewConfirm     = viewModel::onPreviewConfirm,
        onPreviewRetake      = viewModel::onPreviewRetake,
        onPreviewCancel      = viewModel::onPreviewCancel,
        onCropRequested      = viewModel::onCropRequested,
        onCropCompleted      = viewModel::onCropCompleted,
        onCropCancelled      = viewModel::onCropCancelled,
        modifier             = modifier
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.CAMERA
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
    }

internal fun createTempMediaUri(context: Context, isVideo: Boolean): Uri? = runCatching {
    val ext  = if (isVideo) ".mp4" else ".jpg"
    val pref = if (isVideo) "VID_"  else "IMG_"
    val dir  = File(context.externalCacheDir, "picker").also { it.mkdirs() }
    val file = File.createTempFile("${pref}${System.currentTimeMillis()}", ext, dir)
    FileProvider.getUriForFile(context, "${context.packageName}.picker.fileprovider", file)
}.getOrNull()
