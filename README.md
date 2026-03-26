# 📸 compose-media-picker

[![](https://jitpack.io/v/NtbAndroidDev/compose-media-picker.svg)](https://jitpack.io/#NtbAndroidDev/compose-media-picker)
![Min SDK](https://img.shields.io/badge/minSdk-24-brightgreen)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-blue)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

A modern, extensible **Android media picker library** built with **Jetpack Compose** and clean **MVVM architecture**. Supports camera capture, gallery browsing, multi-select with numbered badges, and a polished design system — all behind a **single composable**.

---

## ✨ Features

| Feature | Description |
|---|---|
| 📷 **Camera** | Take a photo or record a video, with instant full-screen preview |
| 🖼 **Gallery** | Browse device media with `All / Photos / Videos` filter chips |
| 🔢 **Multi-select** | Numbered order badges (1, 2, 3…) show tap sequence |
| ✅ **Single-select** | Tap-to-select/deselect with checkmark badge |
| 👁 **Preview screen** | Review camera output before confirming; Retake supported |
| 🎬 **Video thumbnails** | Auto-extracted via Coil `VideoFrameDecoder` |
| 🎨 **Design system** | "Fluid Navigator" — tonal green palette, glassmorphism, spring animations |
| 📦 **Library ready** | One composable, one config object — no boilerplate in your app |

---

## 📦 Installation

### Step 1 — Add JitPack repository

In your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2 — Add the dependency

In your `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.NtbAndroidDev:compose-media-picker:1.0.0")
}
```

> Check [Releases](https://github.com/NtbAndroidDev/compose-media-picker/releases) for the latest version.


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

> ✅ Permissions, camera launchers, FileProvider, and ViewModel are all handled **internally**. No extras needed in your `AndroidManifest.xml`.

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
│ [✕]                        │  ← cancel → returns to previous screen
│                            │
│   full-screen preview      │
│   (▶ overlay for video)    │
│                            │
│  [🔄 Retake] [Use This →]  │
└────────────────────────────┘
```

---

## 🏗 Architecture

### Module structure

```
compose-media-picker/
├── library/                    ← Android Library (.aar) — published to JitPack
│   ├── build.gradle.kts        (com.android.library + maven-publish)
│   └── src/main/
│       ├── AndroidManifest.xml (permissions + FileProvider — auto-merged into app)
│       └── java/com/example/picker_photo/
│           ├── data/
│           │   ├── model/      MediaItem, PickerConfig, PickerResult,
│           │   │               LaunchMode, MediaFilter, SelectionMode
│           │   ├── source/     MediaLocalDataSource  (MediaStore on IO)
│           │   └── repository/ MediaRepository
│           ├── domain/
│           │   └── GetMediaUseCase
│           └── ui/
│               ├── picker/     PhotoPickerEntryPoint ← public API
│               │               PhotoPickerScreen, ViewModel, UiState
│               └── theme/      Fluid Navigator design tokens
│
└── app/                        ← Demo application
    └── MainActivity.kt         (shows library usage)
```

### Layer responsibilities

| Layer | Class | Responsibility |
|---|---|---|
| **Data** | `MediaLocalDataSource` | Queries `MediaStore` on `Dispatchers.IO` |
| **Data** | `MediaRepository` | Single Source of Truth |
| **Domain** | `GetMediaUseCase` | Business logic, filter application |
| **UI** | `PhotoPickerViewModel` | `StateFlow` state + `Channel` one-shot events |
| **UI** | `PhotoPickerEntryPoint` | Public composable — permissions, camera launchers |
| **UI** | `PhotoPickerScreen` | Stateless Compose UI |

---

## 🔐 Permissions & FileProvider

**No setup required!** The `:library` module's `AndroidManifest.xml` declares everything:

```xml
<!-- These are automatically merged into your app's manifest -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.CAMERA" />

<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.picker.fileprovider" ... />
```

Runtime permission requests are also handled internally by `PhotoPickerEntryPoint`.

---

## 🛠 Tech Stack

| | Library / Version |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Image & video loading | [Coil](https://coil-kt.github.io/coil/) 2.7.0 + `coil-video` |
| State management | `StateFlow` + `Channel` (one-shot events) |
| ViewModel | `lifecycle-viewmodel-compose` 2.8.7 |
| DI | Manual factory (Hilt/Koin-ready) |
| Min SDK | 24 |
| Target SDK | 36 |
| Language | Kotlin 2.0.21 |

---

## 🗓 Roadmap

- [x] Camera: photo + video capture with preview screen
- [x] Gallery: multi-select with numbered badges
- [x] Gallery: single-select with checkmark
- [x] Video thumbnails (Coil VideoFrameDecoder)
- [x] MediaFilter: All / Photos / Videos
- [x] JitPack publishing
- [ ] Hilt / Koin integration module
- [ ] Image crop & rotate after selection
- [ ] Cloud / remote media source support
- [ ] Unit tests for ViewModel
- [ ] Maven Central publishing

---

## 📄 License

```
MIT License — free to use, modify, and distribute.
```
