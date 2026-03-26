package com.example.picker_photo.ui.picker

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.example.picker_photo.data.model.LaunchMode
import com.example.picker_photo.data.model.MediaFilter
import com.example.picker_photo.data.model.MediaItem
import com.example.picker_photo.data.model.SelectionMode
import com.example.picker_photo.ui.theme.OnPrimary
import com.example.picker_photo.ui.theme.Primary
import com.example.picker_photo.ui.theme.PrimaryContainer
import com.example.picker_photo.ui.theme.PrimaryFixed
import com.example.picker_photo.ui.theme.SelectionOverlay
import com.example.picker_photo.ui.theme.SurfaceContainerHigh
import com.example.picker_photo.ui.theme.SurfaceContainerLow
import com.example.picker_photo.ui.theme.SurfaceContainerLowest

// ─────────────────────────────────────────────────────────────────────────────
// Root
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickerScreen(
    uiState: PhotoPickerUiState,
    onItemToggled: (MediaItem) -> Unit,
    onConfirm: () -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
    onGallerySelected: () -> Unit,
    onCameraButtonTapped: () -> Unit,
    onCameraSheetDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onRecordVideo: () -> Unit,
    onFilterChanged: (MediaFilter) -> Unit,
    onPreviewConfirm: () -> Unit,
    onPreviewRetake: () -> Unit,
    onPreviewCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (uiState) {
            PhotoPickerUiState.Loading            -> LoadingContent()
            PhotoPickerUiState.PermissionRequired -> PermissionContent()
            is PhotoPickerUiState.Error           -> ErrorContent(uiState.message, onRetry)

            is PhotoPickerUiState.ModeEntry -> {
                ModeEntryContent(onGallerySelected, onCameraButtonTapped)
                if (uiState.showCameraSheet) {
                    CameraSubSheet(onCameraSheetDismiss, onTakePhoto, onRecordVideo)
                }
            }

            is PhotoPickerUiState.Gallery -> {
                GalleryContent(
                    state                = uiState,
                    onItem               = onItemToggled,
                    onConfirm            = onConfirm,
                    onClear              = onClear,
                    onCameraButtonTapped = onCameraButtonTapped,
                    onFilterChanged      = onFilterChanged
                )
                if (uiState.showCameraSheet) {
                    CameraSubSheet(onCameraSheetDismiss, onTakePhoto, onRecordVideo)
                }
            }

            is PhotoPickerUiState.Preview ->
                PreviewContent(
                    state     = uiState,
                    onConfirm = onPreviewConfirm,
                    onRetake  = onPreviewRetake,
                    onCancel  = onPreviewCancel
                )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading / Permission / Error
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryContainer, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Loading media…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PermissionContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("📷", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(24.dp))
            Text("Permission Required", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Please grant media access so we can show your photos and videos.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("⚠️", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            PrimaryGradientButton("Try Again", onRetry)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mode Entry
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModeEntryContent(onGallerySelected: () -> Unit, onCameraButtonTapped: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add Media", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text("Choose how you'd like to add media", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(40.dp))
        ModeCard(Icons.Default.Add,    "Camera",  "Take a photo or record a video", isGradient = true,  onClick = onCameraButtonTapped)
        Spacer(Modifier.height(16.dp))
        ModeCard(Icons.Default.Search, "Library", "Browse photos & videos",         isGradient = false, onClick = onGallerySelected)
    }
}

@Composable
private fun ModeCard(icon: ImageVector, title: String, subtitle: String, isGradient: Boolean, onClick: () -> Unit) {
    val bgMod = if (isGradient)
        Modifier.background(Brush.linearGradient(listOf(Primary, PrimaryContainer)), RoundedCornerShape(20.dp))
    else
        Modifier.background(SurfaceContainerLowest, RoundedCornerShape(20.dp))
    val contentColor  = if (isGradient) OnPrimary else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isGradient) OnPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.fillMaxWidth().then(bgMod).clickable(onClick = onClick).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).background(
                    (if (isGradient) Color.White else PrimaryContainer).copy(alpha = 0.15f), RoundedCornerShape(16.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (isGradient) Color.White else PrimaryContainer, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title,    style = MaterialTheme.typography.titleLarge,  color = contentColor)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,  color = subtitleColor)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Camera sub-sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraSubSheet(onDismiss: () -> Unit, onTakePhoto: () -> Unit, onRecordVideo: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = SurfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Use Camera", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 20.dp))
            CameraOption(Icons.Default.Add,       "Take Photo",   "Capture a still image", onTakePhoto)
            Spacer(Modifier.height(12.dp))
            CameraOption(Icons.Default.PlayArrow, "Record Video", "Record a video clip",   onRecordVideo)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CameraOption(icon: ImageVector, label: String, sublabel: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceContainerLow).clickable(onClick = onClick).padding(16.dp)
    ) {
        Box(modifier = Modifier.size(48.dp).background(Brush.linearGradient(listOf(Primary, PrimaryContainer)), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label,    style = MaterialTheme.typography.bodyMedium,  color = MaterialTheme.colorScheme.onSurface)
            Text(sublabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview Screen (after camera capture)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PreviewContent(
    state: PhotoPickerUiState.Preview,
    onConfirm: () -> Unit,
    onRetake: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Full‑screen preview ───────────────────────────────────────────────
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(state.uri)
                .decoderFactory(VideoFrameDecoder.Factory())  // works for both photo & video
                .build(),
            contentDescription = "Preview",
            contentScale       = ContentScale.Fit,
            modifier           = Modifier.fillMaxSize()
        )

        // Video indicator overlay
        if (state.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, "Video", tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }

        // ── Top bar: Cancel ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))
                )
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            IconButton(onClick = onCancel) {
                Box(
                    modifier = Modifier.size(36.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Cancel", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── Bottom bar: Retake + Confirm ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))
                )
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Retake
                OutlinedButton(
                    onClick = onRetake,
                    shape   = RoundedCornerShape(50),
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Retake", style = MaterialTheme.typography.bodyMedium)
                }

                // Use this
                PrimaryGradientButton(
                    label    = "Use This",
                    onClick  = onConfirm,
                    modifier = Modifier.weight(1.5f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gallery
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryContent(
    state: PhotoPickerUiState.Gallery,
    onItem: (MediaItem) -> Unit,
    onConfirm: () -> Unit,
    onClear: () -> Unit,
    onCameraButtonTapped: () -> Unit,
    onFilterChanged: (MediaFilter) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        GalleryTopBar(state, onClear, onCameraButtonTapped)

        if (state.config.allowFilterChange) {
            Row(
                modifier = Modifier.fillMaxWidth().background(SurfaceContainerLowest).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaFilter.entries.forEach { f ->
                    FilterChip(
                        selected = f == state.activeFilter,
                        onClick  = { onFilterChanged(f) },
                        label    = { Text(f.label, style = MaterialTheme.typography.labelMedium) },
                        shape    = RoundedCornerShape(50),
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryContainer, selectedLabelColor = OnPrimary,
                            containerColor = SurfaceContainerHigh, labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement   = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(items = state.mediaItems, key = { it.id }) { item ->
                MediaThumbnail(
                    item          = item,
                    orderIndex    = state.orderOf(item),      // null = not selected
                    selectionMode = state.config.selectionMode,
                    onClick       = { onItem(item) }
                )
            }
        }

        AnimatedVisibility(
            visible = state.confirmEnabled,
            enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit    = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().background(SurfaceContainerLowest.copy(alpha = 0.95f)).navigationBarsPadding().padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                PrimaryGradientButton(
                    label    = "Add ${state.selectedCount} ${if (state.selectedCount == 1) "item" else "items"}",
                    onClick  = onConfirm,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun GalleryTopBar(
    state: PhotoPickerUiState.Gallery,
    onClear: () -> Unit,
    onCameraButtonTapped: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().background(SurfaceContainerLowest.copy(alpha = 0.92f)).statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when (state.config.selectionMode) {
                        SelectionMode.SINGLE   -> "Select a photo"
                        SelectionMode.MULTIPLE -> "Select Media"
                    },
                    style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (state.config.launchMode is LaunchMode.Combined) {
                    IconButton(onClick = onCameraButtonTapped) {
                        Icon(Icons.Default.Add, "Open camera", tint = PrimaryContainer)
                    }
                }
                AnimatedVisibility(visible = state.selectedCount > 0, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (state.selectedCount > 0) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(Brush.linearGradient(listOf(PrimaryFixed, PrimaryContainer))))
                    Spacer(Modifier.width(8.dp))
                    Text("${state.selectedCount} selected", style = MaterialTheme.typography.labelMedium, color = PrimaryContainer)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Media thumbnail cell — video thumbnail via VideoFrameDecoder
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MediaThumbnail(
    item: MediaItem,
    orderIndex: Int?,           // 1-based (MULTIPLE), null = unselected
    selectionMode: SelectionMode,
    onClick: () -> Unit
) {
    val isSelected = orderIndex != null
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label         = "thumb_scale"
    )
    val context = LocalContext.current

    Box(modifier = Modifier.aspectRatio(1f).scale(scale).clip(RoundedCornerShape(4.dp)).clickable(onClick = onClick)) {

        // ── Coil image with VideoFrameDecoder for video thumbnails ────────────
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .decoderFactory(VideoFrameDecoder.Factory())
                .crossfade(true)
                .build(),
            contentDescription = item.displayName,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )

        // Selection overlay
        if (isSelected) Box(Modifier.fillMaxSize().background(SelectionOverlay))

        // Video duration badge (bottom-left)
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart).padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(item.durationMs.toFormattedDuration(), style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }

        // ── Selection badge (top-right) ───────────────────────────────────────
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
            when {
                // MULTIPLE mode: show order number
                isSelected && selectionMode == SelectionMode.MULTIPLE -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(Primary, PrimaryContainer)),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = "$orderIndex",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize   = 11.sp
                            )
                        )
                    }
                }
                // SINGLE mode: show checkmark
                isSelected -> {
                    Icon(Icons.Filled.CheckCircle, "Selected", tint = PrimaryFixed, modifier = Modifier.size(24.dp))
                }
                // Unselected: circle outline
                else -> {
                    Canvas(Modifier.size(24.dp)) {
                        val px = 2.dp.toPx()
                        drawCircle(
                            color  = Color.White.copy(alpha = 0.75f),
                            radius = size.minDimension / 2f - px,
                            style  = androidx.compose.ui.graphics.drawscope.Stroke(width = px)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gradient pill button (public)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PrimaryGradientButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier.height(52.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled) Brush.linearGradient(
                        listOf(Primary, PrimaryContainer),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end   = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ) else Brush.linearGradient(listOf(SurfaceContainerHigh, SurfaceContainerHigh)),
                    shape = RoundedCornerShape(50)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label, style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun Long.toFormattedDuration(): String {
    val s = this / 1000; return "%d:%02d".format(s / 60, s % 60)
}
