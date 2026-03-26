package com.example.picker_photo

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.picker_photo.data.model.LaunchMode
import com.example.picker_photo.data.model.MediaFilter
import com.example.picker_photo.data.model.PickerConfig
import com.example.picker_photo.data.model.PickerResult
import com.example.picker_photo.data.model.SelectionMode
import com.example.picker_photo.ui.picker.PhotoPickerEntryPoint
import com.example.picker_photo.ui.theme.PickerphotoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PickerphotoTheme {
                PhotoPickerEntryPoint(
                    config = PickerConfig(
                        launchMode        = LaunchMode.Combined,
                        selectionMode     = SelectionMode.MULTIPLE,
                        initialFilter     = MediaFilter.ALL,
                        maxSelectionCount = 30,
                        allowFilterChange = true
                    ),
                    onResult = { result ->
                        when (result) {
                            is PickerResult.Selected -> {
                                val n = result.uris.size
                                Toast.makeText(this, "$n item(s) selected", Toast.LENGTH_SHORT).show()
                                Log.d("Picker", "Delivered: ${result.uris}")
                                // TODO: setResult(RESULT_OK, ...) or navigate to preview
                            }
                            PickerResult.Cancelled ->
                                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}