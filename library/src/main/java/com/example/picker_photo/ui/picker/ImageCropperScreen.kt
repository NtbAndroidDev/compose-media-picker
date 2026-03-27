package com.example.picker_photo.ui.picker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.picker_photo.ui.theme.Primary
import com.example.picker_photo.ui.theme.PrimaryContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Aspect ratio preset for the crop region.
 */
enum class CropAspectRatio(val label: String, val w: Float, val h: Float) {
    FREE("Free", 0f, 0f),
    SQUARE("1:1", 1f, 1f),
    PORTRAIT("3:4", 3f, 4f),
    LANDSCAPE("4:3", 4f, 3f),
    WIDE("16:9", 16f, 9f),
}

/**
 * Full-screen Image Cropper composable.
 *
 * Supports pinch-to-zoom, pan, and preset aspect ratios.
 * The cropped result is saved to the app's external cache and returned as a [Uri].
 *
 * Example:
 * ```kotlin
 * ImageCropperScreen(
 *     sourceUri   = selectedUri,
 *     aspectRatio = CropAspectRatio.SQUARE,
 *     onCropped   = { croppedUri -> /* use it */ },
 *     onCancel    = { /* dismiss */ }
 * )
 * ```
 *
 * @param sourceUri   The original image Uri to crop.
 * @param aspectRatio The desired crop aspect ratio (default: FREE).
 * @param onCropped   Called with the cropped image [Uri] when the user confirms.
 * @param onCancel    Called when the user cancels without cropping.
 */
@Composable
fun ImageCropperScreen(
    sourceUri: Uri,
    aspectRatio: CropAspectRatio = CropAspectRatio.SQUARE,
    onCropped: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // Loaded bitmap
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var rawBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Pan + zoom state
    var scale  by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Crop rect in canvas coordinates (computed once canvas size is known)
    var cropRect by remember { mutableStateOf<Rect?>(null) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    // Current aspect ratio selection
    var currentRatio by remember { mutableStateOf(aspectRatio) }

    // Load bitmap off the main thread
    LaunchedEffect(sourceUri) {
        rawBitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
        }
        rawBitmap?.let { bitmap = it.asImageBitmap() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        //─── Canvas: image + dimmed overlay + crop handle ───────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale  = (scale * zoom).coerceIn(0.5f, 5f)
                        offset = offset + pan
                    }
                }
        ) {
            // Save canvas size for crop rect computation
            if (canvasSize != size) {
                canvasSize = size
                cropRect = computeCropRect(size, currentRatio)
            }

            // Draw image
            bitmap?.let { bm ->
                val imgW = bm.width  * scale
                val imgH = bm.height * scale
                val left = (size.width  - imgW) / 2f + offset.x
                val top  = (size.height - imgH) / 2f + offset.y
                drawImage(
                    image  = bm,
                    dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                    dstSize   = androidx.compose.ui.unit.IntSize(imgW.toInt(), imgH.toInt())
                )
            }

            // Dim outside crop
            cropRect?.let { cr ->
                drawDimOverlay(cr)
                drawCropBorder(cr)
                drawCornerHandles(cr)
            }
        }

        //─── Top bar: Cancel ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.55f))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, "Cancel", tint = Color.White)
            }
            Text(
                "Crop Image",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            // Confirm button
            PrimaryGradientButton(
                label    = "Done",
                onClick  = {
                    scope.launch {
                        val croppedUri = cropAndSave(
                            context    = context,
                            rawBitmap  = rawBitmap,
                            canvas     = canvasSize,
                            cropRect   = cropRect,
                            imgScale   = scale,
                            imgOffset  = offset
                        )
                        croppedUri?.let { onCropped(it) } ?: onCancel()
                    }
                },
                modifier = Modifier
                    .height(40.dp)
                    .padding(end = 8.dp)
            )
        }

        //─── Bottom: aspect ratio selector ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            CropAspectRatio.entries.forEach { ratio ->
                val selected = ratio == currentRatio
                PrimaryGradientButton(
                    label    = ratio.label,
                    onClick  = {
                        currentRatio = ratio
                        cropRect = computeCropRect(canvasSize, ratio)
                        // Reset pan/zoom when ratio changes
                        scale  = 1f
                        offset = Offset.Zero
                    },
                    modifier = Modifier
                        .height(36.dp)
                        .background(
                            if (selected) Color.Transparent
                            else Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(50)
                        ),
                    enabled  = true
                )
            }
        }
    }
}

// ── Drawing helpers ───────────────────────────────────────────────────────────

private fun DrawScope.drawDimOverlay(crop: Rect) {
    // Top
    drawRect(Color.Black.copy(alpha = 0.55f),
        topLeft = Offset.Zero,
        size    = androidx.compose.ui.geometry.Size(size.width, crop.top))
    // Bottom
    drawRect(Color.Black.copy(alpha = 0.55f),
        topLeft = Offset(0f, crop.bottom),
        size    = androidx.compose.ui.geometry.Size(size.width, size.height - crop.bottom))
    // Left
    drawRect(Color.Black.copy(alpha = 0.55f),
        topLeft = Offset(0f, crop.top),
        size    = androidx.compose.ui.geometry.Size(crop.left, crop.height))
    // Right
    drawRect(Color.Black.copy(alpha = 0.55f),
        topLeft = Offset(crop.right, crop.top),
        size    = androidx.compose.ui.geometry.Size(size.width - crop.right, crop.height))
}

private fun DrawScope.drawCropBorder(crop: Rect) {
    drawRect(
        color   = Color.White,
        topLeft = crop.topLeft,
        size    = crop.size,
        style   = Stroke(width = 2.dp.toPx())
    )
    // Rule-of-thirds grid
    val colW = crop.width  / 3f
    val rowH = crop.height / 3f
    val gridAlpha = Color.White.copy(alpha = 0.35f)
    for (i in 1..2) {
        drawLine(gridAlpha, Offset(crop.left + colW * i, crop.top), Offset(crop.left + colW * i, crop.bottom), 1.dp.toPx())
        drawLine(gridAlpha, Offset(crop.left, crop.top + rowH * i), Offset(crop.right, crop.top + rowH * i), 1.dp.toPx())
    }
}

private fun DrawScope.drawCornerHandles(crop: Rect) {
    val len = 20.dp.toPx()
    val w   = 3.dp.toPx()
    val corners = listOf(
        crop.topLeft     to (Pair(Offset(crop.left + len, crop.top),   Offset(crop.left, crop.top + len))),
        crop.topRight    to (Pair(Offset(crop.right - len, crop.top),  Offset(crop.right, crop.top + len))),
        crop.bottomLeft  to (Pair(Offset(crop.left + len, crop.bottom),Offset(crop.left, crop.bottom - len))),
        crop.bottomRight to (Pair(Offset(crop.right - len, crop.bottom),Offset(crop.right, crop.bottom - len)))
    )
    corners.forEach { (corner, lines) ->
        drawLine(Color.White, corner, lines.first,  w)
        drawLine(Color.White, corner, lines.second, w)
    }
}

// ── Crop rect computation ─────────────────────────────────────────────────────

private fun computeCropRect(
    canvas: androidx.compose.ui.geometry.Size,
    ratio: CropAspectRatio
): Rect {
    val padding = 40f
    val maxW = canvas.width  - padding * 2
    val maxH = canvas.height - padding * 2

    val (w, h) = when {
        ratio == CropAspectRatio.FREE || ratio.w == 0f -> maxW to maxH
        else -> {
            val rW = maxW
            val rH = maxW * ratio.h / ratio.w
            if (rH <= maxH) rW to rH else (maxH * ratio.w / ratio.h) to maxH
        }
    }
    val left = (canvas.width  - w) / 2f
    val top  = (canvas.height - h) / 2f
    return Rect(left, top, left + w, top + h)
}

// ── Crop & save ───────────────────────────────────────────────────────────────

private suspend fun cropAndSave(
    context: android.content.Context,
    rawBitmap: Bitmap?,
    canvas: androidx.compose.ui.geometry.Size,
    cropRect: Rect?,
    imgScale: Float,
    imgOffset: Offset
): Uri? = withContext(Dispatchers.IO) {
    val bm = rawBitmap ?: return@withContext null
    val cr = cropRect     ?: return@withContext null

    // Map crop rect from canvas space → bitmap space
    val imgW = bm.width  * imgScale
    val imgH = bm.height * imgScale
    val imgLeft = (canvas.width  - imgW) / 2f + imgOffset.x
    val imgTop  = (canvas.height - imgH) / 2f + imgOffset.y

    // Clamp crop to image bounds
    val bitmapScaleX = bm.width  / imgW
    val bitmapScaleY = bm.height / imgH

    val bx = ((cr.left  - imgLeft) * bitmapScaleX).toInt().coerceIn(0, bm.width)
    val by = ((cr.top   - imgTop)  * bitmapScaleY).toInt().coerceIn(0, bm.height)
    val bw = (cr.width  * bitmapScaleX).toInt().coerceIn(1, bm.width  - bx)
    val bh = (cr.height * bitmapScaleY).toInt().coerceIn(1, bm.height - by)

    val cropped = Bitmap.createBitmap(bm, bx, by, bw, bh)

    // Save to cache
    val dir  = File(context.externalCacheDir, "picker_cropped").also { it.mkdirs() }
    val file = File(dir, "cropped_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { out ->
        cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }

    androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.picker.fileprovider",
        file
    )
}
