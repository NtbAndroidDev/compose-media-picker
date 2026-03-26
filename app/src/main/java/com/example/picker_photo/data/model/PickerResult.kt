package com.example.picker_photo.data.model

import android.net.Uri

sealed interface PickerResult {
    data class  Selected(val uris: List<Uri>) : PickerResult
    data object Cancelled                     : PickerResult
}
