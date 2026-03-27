package com.example.picker_photo.ui.picker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

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

/** Which part of the crop rect the user is dragging. */
private enum class DragHandle {
    NONE,
    // Corners
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    // Edges
    TOP, BOTTOM, LEFT, RIGHT,
    // Move whole rect
    INSIDE
}

/**
 * Full-screen Image Cropper composable.
 *
 * Features:
 * - Drag corners/edges to freely resize the crop region
 * - Drag inside the crop box to move it
 * - Pinch-to-zoom and pan the image underneath
 * - Aspect ratio presets: Free | 1:1 | 3:4 | 4:3 | 16:9
 * - Rule-of-thirds grid + thick corner handles
 *
 * @param sourceUri   The original image Uri to crop.
 * @param aspectRatio Initial aspect ratio preset (default: FREE for free-form drag).
 * @param onCropped   Called with the cropped image [Uri] when the user taps Done.
 * @param onCancel    Called when the user taps the ✕ button.
 */
@Composable
fun ImageCropperScreen(
    sourceUri: Uri,
    aspectRatio: CropAspectRatio = CropAspectRatio.FREE,
    onCropped: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var bitmap    by remember { mutableStateOf<ImageBitmap?>(null) }
    var rawBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Image pan + zoom
    var scale  by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Crop rect state
    var cropRect    by remember { mutableStateOf<Rect?>(null) }
    var canvasSize  by remember { mutableStateOf(Size.Zero) }

    // Track which handle is being dragged
    var activeHandle by remember { mutableStateOf(DragHandle.NONE) }

    // Aspect ratio lock (when a preset other than FREE is selected)
    var currentRatio by remember { mutableStateOf(aspectRatio) }

    // Minimum crop size in pixels
    val minCropPx = 80f

    // Load bitmap
    LaunchedEffect(sourceUri) {
        rawBitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
        rawBitmap?.let { bitmap = it.asImageBitmap() }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        // ── Image + crop overlay canvas ──────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // 1️⃣ Pinch/zoom listener for the image
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // Only pan/zoom when not dragging a handle
                        if (activeHandle == DragHandle.NONE) {
                            scale  = (scale * zoom).coerceIn(0.5f, 5f)
                            offset = offset + pan
                        }
                    }
                }
                // 2️⃣ Drag listener for the crop rect handles
                .pointerInput(cropRect) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            activeHandle = cropRect?.let { hitTest(pos, it) } ?: DragHandle.NONE
                        },
                        onDragEnd   = { activeHandle = DragHandle.NONE },
                        onDragCancel = { activeHandle = DragHandle.NONE },
                        onDrag = { change, drag ->
                            change.consume()
                            val cr = cropRect ?: return@detectDragGestures
                            cropRect = resizeCropRect(
                                handle     = activeHandle,
                                drag       = drag,
                                current    = cr,
                                canvas     = canvasSize,
                                minPx      = minCropPx,
                                lockedRatio = if (currentRatio == CropAspectRatio.FREE) null
                                              else currentRatio.w / currentRatio.h
                            )
                        }
                    )
                }
        ) {
            // Init crop rect on first draw
            if (canvasSize != size) {
                canvasSize = size
                if (cropRect == null) cropRect = computeCropRect(size, currentRatio)
            }

            // Draw image
            bitmap?.let { bm ->
                val imgW = bm.width  * scale
                val imgH = bm.height * scale
                val left = (size.width  - imgW) / 2f + offset.x
                val top  = (size.height - imgH) / 2f + offset.y
                drawImage(
                    image     = bm,
                    dstOffset = IntOffset(left.toInt(), top.toInt()),
                    dstSize   = IntSize(imgW.toInt(), imgH.toInt())
                )
            }

            // Draw overlay
            cropRect?.let { cr ->
                drawDimOverlay(cr)
                drawCropBorder(cr)
                drawCornerHandles(cr)
                drawEdgeHandles(cr)
            }
        }

        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.60f))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, "Cancel", tint = Color.White)
            }
            Text("Crop Image", style = MaterialTheme.typography.titleMedium, color = Color.White)
            PrimaryGradientButton(
                label    = "Done",
                onClick  = {
                    scope.launch {
                        val uri = cropAndSave(context, rawBitmap, canvasSize, cropRect, scale, offset)
                        uri?.let { onCropped(it) } ?: onCancel()
                    }
                },
                modifier = Modifier.height(40.dp).padding(end = 8.dp)
            )
        }

        // ── Bottom: aspect ratio chips ────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.70f))
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
                        // Recompute crop rect from current crop center
                        cropRect = cropRect?.let { cur ->
                            if (ratio == CropAspectRatio.FREE) cur
                            else computeCropRectCentered(cur.center, canvasSize, ratio)
                        } ?: computeCropRect(canvasSize, ratio)
                        scale  = 1f
                        offset = Offset.Zero
                    },
                    modifier = Modifier
                        .height(36.dp)
                        .then(
                            if (selected) Modifier.background(Color.White.copy(alpha = 0.25f),
                                androidx.compose.foundation.shape.RoundedCornerShape(50))
                            else Modifier
                        ),
                    enabled = true
                )
            }
        }
    }
}

// ── Hit-test: which handle did the finger land on? ───────────────────────────

private fun hitTest(pos: Offset, cr: Rect): DragHandle {
    val touch = 36f // touch target radius in px

    // Corners (check first — higher priority)
    if (abs(pos.x - cr.left) < touch  && abs(pos.y - cr.top) < touch)    return DragHandle.TOP_LEFT
    if (abs(pos.x - cr.right) < touch && abs(pos.y - cr.top) < touch)    return DragHandle.TOP_RIGHT
    if (abs(pos.x - cr.left) < touch  && abs(pos.y - cr.bottom) < touch) return DragHandle.BOTTOM_LEFT
    if (abs(pos.x - cr.right) < touch && abs(pos.y - cr.bottom) < touch) return DragHandle.BOTTOM_RIGHT

    // Edges (mid-point areas)
    val midX = cr.left + cr.width / 2f
    val midY = cr.top  + cr.height / 2f
    if (abs(pos.x - midX) < touch && abs(pos.y - cr.top) < touch)    return DragHandle.TOP
    if (abs(pos.x - midX) < touch && abs(pos.y - cr.bottom) < touch) return DragHandle.BOTTOM
    if (abs(pos.y - midY) < touch && abs(pos.x - cr.left) < touch)   return DragHandle.LEFT
    if (abs(pos.y - midY) < touch && abs(pos.x - cr.right) < touch)  return DragHandle.RIGHT

    // Inside rect → move
    if (cr.contains(pos)) return DragHandle.INSIDE

    return DragHandle.NONE
}

// ── Resize crop rect based on which handle is being dragged ──────────────────

private fun resizeCropRect(
    handle: DragHandle,
    drag: Offset,
    current: Rect,
    canvas: Size,
    minPx: Float,
    lockedRatio: Float?   // w/h — if non-null, resize maintains this ratio
): Rect {
    var l = current.left
    var t = current.top
    var r = current.right
    var b = current.bottom

    when (handle) {
        DragHandle.TOP_LEFT     -> { l += drag.x; t += drag.y }
        DragHandle.TOP_RIGHT    -> { r += drag.x; t += drag.y }
        DragHandle.BOTTOM_LEFT  -> { l += drag.x; b += drag.y }
        DragHandle.BOTTOM_RIGHT -> { r += drag.x; b += drag.y }
        DragHandle.TOP          -> { t += drag.y }
        DragHandle.BOTTOM       -> { b += drag.y }
        DragHandle.LEFT         -> { l += drag.x }
        DragHandle.RIGHT        -> { r += drag.x }
        DragHandle.INSIDE       -> { l += drag.x; r += drag.x; t += drag.y; b += drag.y }
        DragHandle.NONE         -> return current
    }

    // Enforce minimum size
    if (r - l < minPx) { if (handle == DragHandle.LEFT || handle == DragHandle.TOP_LEFT || handle == DragHandle.BOTTOM_LEFT) l = r - minPx else r = l + minPx }
    if (b - t < minPx) { if (handle == DragHandle.TOP || handle == DragHandle.TOP_LEFT || handle == DragHandle.TOP_RIGHT) t = b - minPx else b = t + minPx }

    // If ratio is locked, adjust the opposite dimension to maintain it
    if (lockedRatio != null && handle != DragHandle.INSIDE) {
        val w = r - l
        val h = b - t
        val newH = w / lockedRatio
        val centerY = (t + b) / 2f
        t = centerY - newH / 2f
        b = centerY + newH / 2f
    }

    // Clamp to canvas bounds
    val padding = 10f
    val cx = (r - l)
    val cy = (b - t)
    l = l.coerceIn(padding, canvas.width  - padding - cx)
    t = t.coerceIn(padding, canvas.height - padding - cy)
    r = l + cx
    b = t + cy

    return Rect(l, t, r, b)
}

// ── Canvas drawing ─────────────────────────────────────────────────────────────

private fun DrawScope.drawDimOverlay(crop: Rect) {
    drawRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset.Zero,            size = Size(size.width, crop.top))
    drawRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset(0f, crop.bottom),size = Size(size.width, size.height - crop.bottom))
    drawRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset(0f, crop.top),   size = Size(crop.left, crop.height))
    drawRect(Color.Black.copy(alpha = 0.55f), topLeft = Offset(crop.right, crop.top), size = Size(size.width - crop.right, crop.height))
}

private fun DrawScope.drawCropBorder(crop: Rect) {
    drawRect(Color.White, topLeft = crop.topLeft, size = crop.size, style = Stroke(width = 1.5.dp.toPx()))
    // Rule-of-thirds grid
    val colW = crop.width / 3f; val rowH = crop.height / 3f
    val grid = Color.White.copy(alpha = 0.30f)
    for (i in 1..2) {
        drawLine(grid, Offset(crop.left + colW * i, crop.top),  Offset(crop.left + colW * i, crop.bottom), 1.dp.toPx())
        drawLine(grid, Offset(crop.left, crop.top + rowH * i),  Offset(crop.right, crop.top + rowH * i),   1.dp.toPx())
    }
}

private fun DrawScope.drawCornerHandles(crop: Rect) {
    val len = 22.dp.toPx(); val w = 3.dp.toPx()
    listOf(
        crop.topLeft     to listOf(Offset(crop.left + len, crop.top),    Offset(crop.left, crop.top + len)),
        crop.topRight    to listOf(Offset(crop.right - len, crop.top),   Offset(crop.right, crop.top + len)),
        crop.bottomLeft  to listOf(Offset(crop.left + len, crop.bottom), Offset(crop.left, crop.bottom - len)),
        crop.bottomRight to listOf(Offset(crop.right - len, crop.bottom),Offset(crop.right, crop.bottom - len))
    ).forEach { (corner, lines) ->
        lines.forEach { end -> drawLine(Color.White, corner, end, w) }
    }
}

/** Mid-edge drag handles — small white rectangles for user feedback */
private fun DrawScope.drawEdgeHandles(crop: Rect) {
    val hw = 20.dp.toPx(); val hh = 4.dp.toPx()
    val midX = crop.left + crop.width / 2f
    val midY = crop.top  + crop.height / 2f

    // Top / Bottom (horizontal pill)
    listOf(crop.top, crop.bottom).forEach { y ->
        drawRoundRect(Color.White, topLeft = Offset(midX - hw / 2f, y - hh / 2f),
            size = Size(hw, hh), cornerRadius = androidx.compose.ui.geometry.CornerRadius(hh / 2f))
    }
    // Left / Right (vertical pill)
    listOf(crop.left, crop.right).forEach { x ->
        drawRoundRect(Color.White, topLeft = Offset(x - hh / 2f, midY - hw / 2f),
            size = Size(hh, hw), cornerRadius = androidx.compose.ui.geometry.CornerRadius(hh / 2f))
    }
}

// ── Crop rect initial computation ─────────────────────────────────────────────

private fun computeCropRect(canvas: Size, ratio: CropAspectRatio): Rect {
    val pad = 40f
    val maxW = canvas.width - pad * 2; val maxH = canvas.height - pad * 2
    val (w, h) = when {
        ratio == CropAspectRatio.FREE || ratio.w == 0f -> maxW to maxH
        else -> { val rH = maxW * ratio.h / ratio.w; if (rH <= maxH) maxW to rH else (maxH * ratio.w / ratio.h) to maxH }
    }
    return Rect((canvas.width - w) / 2f, (canvas.height - h) / 2f, (canvas.width + w) / 2f, (canvas.height + h) / 2f)
}

private fun computeCropRectCentered(center: Offset, canvas: Size, ratio: CropAspectRatio): Rect {
    val pad = 40f
    val maxW = canvas.width - pad * 2; val maxH = canvas.height - pad * 2
    val (w, h) = run {
        val rH = maxW * ratio.h / ratio.w
        if (rH <= maxH) maxW to rH else (maxH * ratio.w / ratio.h) to maxH
    }
    val l = (center.x - w / 2f).coerceIn(pad, canvas.width - pad - w)
    val t = (center.y - h / 2f).coerceIn(pad, canvas.height - pad - h)
    return Rect(l, t, l + w, t + h)
}

// ── Crop & save ────────────────────────────────────────────────────────────────

private suspend fun cropAndSave(
    context: android.content.Context,
    rawBitmap: Bitmap?,
    canvas: Size,
    cropRect: Rect?,
    imgScale: Float,
    imgOffset: Offset
): Uri? = withContext(Dispatchers.IO) {
    val bm = rawBitmap ?: return@withContext null
    val cr = cropRect  ?: return@withContext null

    val imgW     = bm.width  * imgScale
    val imgH     = bm.height * imgScale
    val imgLeft  = (canvas.width  - imgW) / 2f + imgOffset.x
    val imgTop   = (canvas.height - imgH) / 2f + imgOffset.y
    val scaleX   = bm.width  / imgW
    val scaleY   = bm.height / imgH

    val bx = ((cr.left  - imgLeft) * scaleX).toInt().coerceIn(0, bm.width)
    val by = ((cr.top   - imgTop)  * scaleY).toInt().coerceIn(0, bm.height)
    val bw = (cr.width  * scaleX).toInt().coerceIn(1, bm.width  - bx)
    val bh = (cr.height * scaleY).toInt().coerceIn(1, bm.height - by)

    val cropped = Bitmap.createBitmap(bm, bx, by, bw, bh)
    val dir  = File(context.externalCacheDir, "picker_cropped").also { it.mkdirs() }
    val file = File(dir, "cropped_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { out -> cropped.compress(Bitmap.CompressFormat.JPEG, 92, out) }

    androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.picker.fileprovider", file)
}
