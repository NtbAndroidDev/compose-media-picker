# 📸 compose-media-picker

A modern, extensible **Android media picker library** built with **Jetpack Compose** and clean **MVVM architecture**. Supports camera capture, gallery browsing, multi-select, and a polished design system — all behind a single composable.

---

## ✨ Features

| Feature | Description |
|---|---|
| 📷 **Camera** | Take a photo or record a video, with instant preview |
| 🖼 **Gallery** | Browse device media with `All / Photos / Videos` filter chips |
| 🔢 **Multi-select** | Numbered order badges (1, 2, 3…) show tap sequence |
| ✅ **Single-select** | Tap-to-select/deselect with checkmark badge |
| 👁 **Preview screen** | Review camera output before confirming; Retake supported |
| 🎬 **Video thumbnails** | Auto-extracted via Coil `VideoFrameDecoder` |
| 🎨 **Design system** | "Fluid Navigator" — tonal green palette, glassmorphism, spring animations |
| 📦 **Library-ready** | One composable, one config object — drop into any project |

---

## 🚀 Quick Start

Drop `PhotoPickerEntryPoint` anywhere in your Compose hierarchy:

```kotlin
PhotoPickerEntryPoint(
    config = PickerConfig(
        launchMode        = LaunchMode.Combined,     // Camera + Library entry screen
        selectionMode     = SelectionMode.MULTIPLE,
        initialFilter     = MediaFilter.ALL,
        maxSelectionCount = 10,
        allowFilterChange = true
    ),
    onResult = { result ->
        when (result) {
            is PickerResult.Selected -> {
                val uris: List<Uri> = result.uris
                // use the selected media URIs
            }
            PickerResult.Cancelled -> { /* handle cancel */ }
        }
    }
)
```

> All permissions, camera launchers, FileProvider, and ViewModel wiring are handled **internally**.

---

## ⚙️ Configuration — `PickerConfig`

```kotlin
data class PickerConfig(
    val launchMode:        LaunchMode    = LaunchMode.Combined,
    val selectionMode:     SelectionMode = SelectionMode.MULTIPLE,
    val initialFilter:     MediaFilter   = MediaFilter.ALL,
    val maxSelectionCount: Int           = Int.MAX_VALUE,
    val allowFilterChange: Boolean       = true
)
```

### `LaunchMode`

| Value | Behaviour |
|---|---|
| `LaunchMode.Combined` | Shows entry screen — user chooses Camera or Library |
| `LaunchMode.GalleryOnly` | Opens gallery directly |
| `LaunchMode.CameraOnly(CameraType.PHOTO)` | Launches system camera for a photo immediately |
| `LaunchMode.CameraOnly(CameraType.VIDEO)` | Launches system camera for a video immediately |

### `MediaFilter`

| Value | Shows |
|---|---|
| `MediaFilter.ALL` | Images + Videos |
| `MediaFilter.IMAGES_ONLY` | Images only |
| `MediaFilter.VIDEOS_ONLY` | Videos only |

### `SelectionMode`

| Value | Badge style |
|---|---|
| `SelectionMode.MULTIPLE` | Gradient circle with tap-order number (1, 2, 3…) |
| `SelectionMode.SINGLE` | Checkmark — re-tap to deselect |

---

## 🗺 UX Flow

```
PickerConfig(Combined)
   └── ModeEntry Screen
         ├── [📷 Camera] ──▶ CameraSheet (ModalBottomSheet)
         │                       ├── Take Photo  ──▶ system camera ──▶ Preview ──▶ Deliver
         │                       └── Record Video ─▶ system camera ──▶ Preview ──▶ Deliver
         └── [🔍 Library] ─▶ Gallery Screen
                               ├── FilterChips: [All] [Photos] [Videos]
                               ├── [➕] Camera icon in top bar (Combined only)
                               └── [Add N items] ──▶ Deliver
```

**Preview Screen** (after camera capture):
```
┌────────────────────────────┐
│ [✕]                        │  ← cancel, returns to previous screen
│                            │
│   full-screen preview      │
│   (▶ overlay for video)    │
│                            │
│  [🔄 Retake] [Use This →]  │
└────────────────────────────┘
```

---

## 🏗 Architecture

```
app/
└── data/
│   ├── model/          MediaItem, PickerConfig, PickerResult,
│   │                   LaunchMode, MediaFilter, SelectionMode
│   ├── source/         MediaLocalDataSource  (MediaStore queries on IO)
│   └── repository/     MediaRepository       (Single Source of Truth)
├── domain/
│   └── GetMediaUseCase
└── ui/
    ├── picker/
    │   ├── PhotoPickerEntryPoint.kt   ← public API composable
    │   ├── PhotoPickerScreen.kt       ← stateless UI
    │   ├── PhotoPickerViewModel.kt    ← state + events
    │   └── PhotoPickerUiState.kt      ← sealed state & events
    └── theme/                         Fluid Navigator design tokens
```

---

## 🔐 Required Permissions

Add to your **`AndroidManifest.xml`** (already included in the demo app):

```xml
<!-- Android 13+ -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

<!-- Android 12 and below -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />

<!-- Camera -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

And register the **FileProvider** for camera output URIs:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.picker.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/picker_file_paths" />
</provider>
```

---

## 🛠 Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Image loading | [Coil](https://coil-kt.github.io/coil/) 2.7.0 + `coil-video` |
| State management | `StateFlow` + `Channel` (one-shot events) |
| DI | Manual (Hilt-ready via `ViewModelProvider.Factory`) |
| Min SDK | 24 |
| Target SDK | 36 |

---

## 🗓 Roadmap

- [ ] Hilt / Koin integration module
- [ ] Crop & rotate after selection
- [ ] Cloud media source (remote data source)
- [ ] Unit tests for ViewModel
- [ ] Publish to Maven Central as an AAR library

---

## 📄 License

```
MIT License — feel free to use, modify, and distribute.
```
