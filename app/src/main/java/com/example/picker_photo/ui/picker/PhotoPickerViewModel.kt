package com.example.picker_photo.ui.picker

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.picker_photo.data.model.CameraType
import com.example.picker_photo.data.model.LaunchMode
import com.example.picker_photo.data.model.MediaFilter
import com.example.picker_photo.data.model.MediaItem
import com.example.picker_photo.data.model.PickerConfig
import com.example.picker_photo.data.model.SelectionMode
import com.example.picker_photo.domain.GetMediaUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class PhotoPickerViewModel(
    private val getMediaUseCase: GetMediaUseCase,
    val config: PickerConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow<PhotoPickerUiState>(PhotoPickerUiState.Loading)
    val uiState: StateFlow<PhotoPickerUiState> = _uiState.asStateFlow()

    private val _events = Channel<PhotoPickerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Selection: List preserves tap order for numbered badges
    private val selectionOrder = mutableListOf<Long>()
    private var currentFilter = config.initialFilter
    // State to return to when preview is cancelled
    private var stateBeforePreview: PhotoPickerUiState? = null

    // ── Start ─────────────────────────────────────────────────────────────────

    fun start() {
        when (val mode = config.launchMode) {
            LaunchMode.Combined    -> _uiState.value = PhotoPickerUiState.ModeEntry()
            LaunchMode.GalleryOnly -> loadGallery(currentFilter)
            is LaunchMode.CameraOnly -> viewModelScope.launch {
                _events.send(
                    if (mode.type == CameraType.VIDEO) PhotoPickerEvent.LaunchCameraVideo
                    else PhotoPickerEvent.LaunchCameraPhoto
                )
            }
        }
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    fun onPermissionGranted() = start()
    fun onPermissionDenied()  { _uiState.value = PhotoPickerUiState.PermissionRequired }

    // ── Mode entry ────────────────────────────────────────────────────────────

    fun onGallerySelected() = loadGallery(currentFilter)

    fun onCameraButtonTapped() = setCameraSheet(true)
    fun onCameraSheetDismiss() = setCameraSheet(false)

    fun onTakePhoto() {
        setCameraSheet(false)
        viewModelScope.launch { _events.send(PhotoPickerEvent.LaunchCameraPhoto) }
    }

    fun onRecordVideo() {
        setCameraSheet(false)
        viewModelScope.launch { _events.send(PhotoPickerEvent.LaunchCameraVideo) }
    }

    // ── Camera results → Preview screen ──────────────────────────────────────

    fun onCameraPhotoResult(uri: Uri) {
        stateBeforePreview = _uiState.value.takeUnless { it is PhotoPickerUiState.Preview }
            ?: stateBeforePreview
        _uiState.value = PhotoPickerUiState.Preview(uri, isVideo = false, cameraType = CameraType.PHOTO)
    }

    fun onCameraVideoResult(uri: Uri) {
        stateBeforePreview = _uiState.value.takeUnless { it is PhotoPickerUiState.Preview }
            ?: stateBeforePreview
        _uiState.value = PhotoPickerUiState.Preview(uri, isVideo = true, cameraType = CameraType.VIDEO)
    }

    fun onCameraResultCancelled() {
        if (config.launchMode is LaunchMode.CameraOnly) {
            viewModelScope.launch { _events.send(PhotoPickerEvent.Cancelled) }
        }
        // Combined/GalleryOnly: stay on current screen
    }

    // ── Preview intents ───────────────────────────────────────────────────────

    fun onPreviewConfirm() {
        val p = _uiState.value as? PhotoPickerUiState.Preview ?: return
        viewModelScope.launch { _events.send(PhotoPickerEvent.Deliver(listOf(p.uri))) }
    }

    fun onPreviewRetake() {
        val p = _uiState.value as? PhotoPickerUiState.Preview ?: return
        viewModelScope.launch {
            _events.send(
                if (p.cameraType == CameraType.VIDEO) PhotoPickerEvent.LaunchCameraVideo
                else PhotoPickerEvent.LaunchCameraPhoto
            )
        }
    }

    fun onPreviewCancel() {
        _uiState.value = stateBeforePreview ?: PhotoPickerUiState.ModeEntry()
    }

    // ── Filter ────────────────────────────────────────────────────────────────

    fun onFilterChanged(filter: MediaFilter) {
        if (filter == currentFilter) return
        currentFilter = filter
        loadGallery(filter)
    }

    // ── Selection ─────────────────────────────────────────────────────────────

    fun onMediaItemToggled(item: MediaItem) {
        val current = _uiState.value as? PhotoPickerUiState.Gallery ?: return
        when (config.selectionMode) {
            SelectionMode.SINGLE -> {
                if (item.id in selectionOrder) selectionOrder.clear()      // deselect on re-tap
                else { selectionOrder.clear(); selectionOrder.add(item.id) }
            }
            SelectionMode.MULTIPLE -> {
                if (item.id in selectionOrder) selectionOrder.remove(item.id)
                else if (selectionOrder.size < config.maxSelectionCount) selectionOrder.add(item.id)
            }
        }
        _uiState.value = current.copy(selectionOrder = selectionOrder.toList())
    }

    fun onClearSelection() {
        val current = _uiState.value as? PhotoPickerUiState.Gallery ?: return
        selectionOrder.clear()
        _uiState.value = current.copy(selectionOrder = emptyList())
    }

    fun onConfirmSelection() {
        val current = _uiState.value as? PhotoPickerUiState.Gallery ?: return
        viewModelScope.launch {
            _events.send(PhotoPickerEvent.Deliver(current.selectedItems.map { it.uri }))
        }
    }

    fun onRetry() = loadGallery(currentFilter)

    // ── Private ───────────────────────────────────────────────────────────────

    private fun loadGallery(filter: MediaFilter) {
        _uiState.value = PhotoPickerUiState.Loading
        viewModelScope.launch {
            runCatching { getMediaUseCase(filter) }
                .onSuccess { items ->
                    _uiState.value = PhotoPickerUiState.Gallery(
                        mediaItems     = items,
                        selectionOrder = selectionOrder.toList(),
                        activeFilter   = filter,
                        config         = config
                    )
                }
                .onFailure { e ->
                    _uiState.value = PhotoPickerUiState.Error(e.localizedMessage ?: "Unknown error")
                }
        }
    }

    private fun setCameraSheet(visible: Boolean) {
        when (val s = _uiState.value) {
            is PhotoPickerUiState.ModeEntry -> _uiState.value = s.copy(showCameraSheet = visible)
            is PhotoPickerUiState.Gallery   -> _uiState.value = s.copy(showCameraSheet = visible)
            else -> {}
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(
        private val getMediaUseCase: GetMediaUseCase,
        private val config: PickerConfig
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PhotoPickerViewModel(getMediaUseCase, config) as T
    }
}
