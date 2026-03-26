package com.example.picker_photo.provider

import androidx.core.content.FileProvider

/**
 * Custom FileProvider to prevent manifest merge conflicts
 * with other libraries or the host app that might also
 * use androidx.core.content.FileProvider directly.
 */
class PickerFileProvider : FileProvider()
