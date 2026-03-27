package com.example.picker_photo.ui.picker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.picker_photo.ui.theme.OnPrimary
import com.example.picker_photo.ui.theme.PrimaryContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// Public API
// ─────────────────────────────────────────────────────────────────────────────

enum class CropAspectRatio(val label: String, val w: Float, val h: Float) {
    FREE("Free", 0f, 0f),
    SQUARE("1:1", 1f, 1f),
    PORTRAIT("3:4", 3f, 4f),
    LANDSCAPE("4:3", 4f, 3f),
    WIDE("16:9", 16f, 9f),
}

// ─────────────────────────────────────────────────────────────────────────────
// Composable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Telegram-style image cropper.
 *
 * Uses a **single unified pointer-input** to route gestures:
 *   • Finger on corner/edge handle → resize crop box
 *   • Finger inside crop box   → move crop box
 *   • 1 finger elsewhere       → pan image
 *   • 2 fingers                → pinch-zoom image
 *
 * This avoids all gesture conflicts — there is no competing detector.
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
    var imgScale  by remember { mutableFloatStateOf(1f) }
    var imgOffset by remember { mutableStateOf(Offset.Zero) }
    var cropRect  by remember { mutableStateOf<Rect?>(null) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var currentRatio by remember { mutableStateOf(aspectRatio) }

    val minCropPx = 80f

    LaunchedEffect(sourceUri) {
        rawBitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(sourceUri)
                    ?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
        rawBitmap?.let { bitmap = it.asImageBitmap() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // ── Single canvas — single pointerInput that owns ALL gestures ─────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {

                        // ── 1. Wait for the very first finger down ────────────
                        val first = awaitFirstDown(requireUnconsumed = false)
                        val cr0   = cropRect
                        val handle = if (cr0 != null) hitTest(first.position, cr0)
                                     else CropHandle.NONE

                        // ── 2a. Handle is on a crop control → CROP MODE ───────
                        if (handle != CropHandle.NONE) {
                            first.consume()
                            var prev = first.position
                            val locked = if (currentRatio == CropAspectRatio.FREE) null
                                         else currentRatio.w / currentRatio.h

                            // track only this finger until it lifts
                            while (true) {
                                val event  = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull { it.id == first.id }
                                    ?: break
                                if (!change.pressed) break

                                val delta = change.position - prev
                                prev = change.position
                                if (delta != Offset.Zero) {
                                    cropRect = resizeCropRect(
                                        handle      = handle,
                                        drag        = delta,
                                        current     = cropRect ?: cr0!!,
                                        canvas      = canvasSize,
                                        minPx       = minCropPx,
                                        lockedRatio = locked
                                    )
                                }
                                change.consume()
                            }

                        // ── 2b. No handle → PAN / ZOOM MODE ──────────────────
                        } else {
                            first.consume()
                            var prevPositions = mapOf(first.id to first.position)
                            var prevDist = 0f

                            while (true) {
                                val event   = awaitPointerEvent(PointerEventPass.Main)
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break

                                // Update known positions map
                                val currPositions = pressed.associate { it.id to it.position }

                                when (pressed.size) {
                                    // ── Single finger: pan ────────────────────
                                    1 -> {
                                        val ch    = pressed[0]
                                        val prev  = prevPositions[ch.id]
                                        if (prev != null) {
                                            imgOffset = imgOffset + (ch.position - prev)
                                        }
                                        ch.consume()
                                    }
                                    // ── Two fingers: pinch zoom ───────────────
                                    else -> {
                                        val p0 = pressed[0].position
                                        val p1 = pressed[1].position
                                        val currDist = dist(p0, p1)
                                        if (prevDist > 0f && currDist > 0f) {
                                            imgScale = (imgScale * (currDist / prevDist))
                                                .coerceIn(0.5f, 6f)
                                        }
                                        prevDist = currDist
                                        pressed.forEach { it.consume() }
                                    }
                                }

                                prevPositions = currPositions
                            }
                        }
                    }
                }
        ) {
            // Init crop rect on first draw
            if (canvasSize != size) {
                canvasSize = size
                if (cropRect == null) cropRect = computeCropRect(size, currentRatio)
            }

            // Draw image
            bitmap?.let { bm ->
                val iW = bm.width  * imgScale
                val iH = bm.height * imgScale
                val l  = (size.width  - iW) / 2f + imgOffset.x
                val t  = (size.height - iH) / 2f + imgOffset.y
                drawImage(bm,
                    dstOffset = IntOffset(l.toInt(), t.toInt()),
                    dstSize   = IntSize(iW.toInt(), iH.toInt()))
            }

            // Draw crop overlay
            cropRect?.let { cr ->
                drawDimOverlay(cr)
                drawGrid(cr)
                drawBorder(cr)
                drawCornerHandles(cr)
                drawEdgePills(cr)
            }
        }

        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.65f))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, "Cancel", tint = Color.White)
            }
            Text("Crop Image",
                style = MaterialTheme.typography.titleMedium, color = Color.White)
            PrimaryGradientButton(
                label   = "Done",
                onClick = {
                    scope.launch {
                        val uri = cropAndSave(context, rawBitmap, canvasSize, cropRect, imgScale, imgOffset)
                        uri?.let { onCropped(it) } ?: onCancel()
                    }
                },
                modifier = Modifier.height(40.dp).padding(end = 8.dp)
            )
        }

        // ── Aspect ratio chips ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.75f))
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            CropAspectRatio.entries.forEach { ratio ->
                FilterChip(
                    selected = ratio == currentRatio,
                    onClick  = {
                        currentRatio = ratio
                        cropRect = cropRect?.let { cur ->
                            if (ratio == CropAspectRatio.FREE) cur
                            else computeCropRectCentered(cur.center, canvasSize, ratio)
                        } ?: computeCropRect(canvasSize, ratio)
                    },
                    label  = {
                        Text(ratio.label, style = MaterialTheme.typography.labelMedium)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryContainer,
                        selectedLabelColor     = OnPrimary,
                        containerColor         = Color.White.copy(alpha = 0.15f),
                        labelColor             = Color.White
                    ),
                    border = null
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hit testing
// ─────────────────────────────────────────────────────────────────────────────

private enum class CropHandle {
    NONE,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,   // corners
    TOP, BOTTOM, LEFT, RIGHT,                          // edges
    INSIDE                                             // move whole box
}

private fun hitTest(pos: Offset, cr: Rect): CropHandle {
    val cR = 48f   // corner touch radius (px)
    val eR = 36f   // edge mid-point touch radius (px)

    // Corners — highest priority
    if (abs(pos.x - cr.left)  < cR && abs(pos.y - cr.top)    < cR) return CropHandle.TOP_LEFT
    if (abs(pos.x - cr.right) < cR && abs(pos.y - cr.top)    < cR) return CropHandle.TOP_RIGHT
    if (abs(pos.x - cr.left)  < cR && abs(pos.y - cr.bottom) < cR) return CropHandle.BOTTOM_LEFT
    if (abs(pos.x - cr.right) < cR && abs(pos.y - cr.bottom) < cR) return CropHandle.BOTTOM_RIGHT

    // Edges (mid-point area)
    val mX = cr.left + cr.width  / 2f
    val mY = cr.top  + cr.height / 2f
    if (abs(pos.x - mX) < eR && abs(pos.y - cr.top)    < eR) return CropHandle.TOP
    if (abs(pos.x - mX) < eR && abs(pos.y - cr.bottom) < eR) return CropHandle.BOTTOM
    if (abs(pos.y - mY) < eR && abs(pos.x - cr.left)   < eR) return CropHandle.LEFT
    if (abs(pos.y - mY) < eR && abs(pos.x - cr.right)  < eR) return CropHandle.RIGHT

    // Inside → move
    if (cr.contains(pos)) return CropHandle.INSIDE

    return CropHandle.NONE
}

// ─────────────────────────────────────────────────────────────────────────────
// Resize logic
// ─────────────────────────────────────────────────────────────────────────────

private fun resizeCropRect(
    handle: CropHandle,
    drag: Offset,
    current: Rect,
    canvas: Size,
    minPx: Float,
    lockedRatio: Float?
): Rect {
    var l = current.left;  var t = current.top
    var r = current.right; var b = current.bottom

    when (handle) {
        CropHandle.TOP_LEFT     -> { l += drag.x; t += drag.y }
        CropHandle.TOP_RIGHT    -> { r += drag.x; t += drag.y }
        CropHandle.BOTTOM_LEFT  -> { l += drag.x; b += drag.y }
        CropHandle.BOTTOM_RIGHT -> { r += drag.x; b += drag.y }
        CropHandle.TOP          -> { t += drag.y }
        CropHandle.BOTTOM       -> { b += drag.y }
        CropHandle.LEFT         -> { l += drag.x }
        CropHandle.RIGHT        -> { r += drag.x }
        CropHandle.INSIDE       -> { l += drag.x; r += drag.x; t += drag.y; b += drag.y }
        CropHandle.NONE         -> return current
    }

    // Enforce minimum size
    if (r - l < minPx) when (handle) {
        CropHandle.LEFT, CropHandle.TOP_LEFT, CropHandle.BOTTOM_LEFT -> l = r - minPx
        else -> r = l + minPx
    }
    if (b - t < minPx) when (handle) {
        CropHandle.TOP, CropHandle.TOP_LEFT, CropHandle.TOP_RIGHT -> t = b - minPx
        else -> b = t + minPx
    }

    // Lock aspect ratio
    if (lockedRatio != null && handle != CropHandle.INSIDE) {
        val newW  = r - l
        val newH  = newW / lockedRatio
        val ctrY  = (t + b) / 2f
        t = ctrY - newH / 2f
        b = ctrY + newH / 2f
    }

    // Clamp to canvas
    val pad = 10f
    val fw  = (r - l).coerceAtLeast(minPx)
    val fh  = (b - t).coerceAtLeast(minPx)
    l = l.coerceIn(pad, canvas.width  - pad - fw)
    t = t.coerceIn(pad, canvas.height - pad - fh)

    return Rect(l, t, l + fw, t + fh)
}

// ─────────────────────────────────────────────────────────────────────────────
// Drawing helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawDimOverlay(cr: Rect) {
    val dim = Color.Black.copy(alpha = 0.55f)
    drawRect(dim, Offset.Zero,              Size(size.width, cr.top))
    drawRect(dim, Offset(0f, cr.bottom),    Size(size.width, size.height - cr.bottom))
    drawRect(dim, Offset(0f, cr.top),       Size(cr.left, cr.height))
    drawRect(dim, Offset(cr.right, cr.top), Size(size.width - cr.right, cr.height))
}

private fun DrawScope.drawGrid(cr: Rect) {
    val g  = Color.White.copy(alpha = 0.25f)
    val sw = 0.8.dp.toPx()
    val cW = cr.width / 3f; val rH = cr.height / 3f
    for (i in 1..2) {
        drawLine(g, Offset(cr.left + cW * i, cr.top), Offset(cr.left + cW * i, cr.bottom), sw)
        drawLine(g, Offset(cr.left, cr.top + rH * i), Offset(cr.right, cr.top + rH * i),   sw)
    }
}

private fun DrawScope.drawBorder(cr: Rect) {
    drawRect(Color.White, cr.topLeft, cr.size, style = Stroke(1.5.dp.toPx()))
}

private fun DrawScope.drawCornerHandles(cr: Rect) {
    val len = 24.dp.toPx(); val sw = 3.5.dp.toPx(); val c = Color.White
    // TL
    drawLine(c, cr.topLeft, Offset(cr.left + len, cr.top), sw)
    drawLine(c, cr.topLeft, Offset(cr.left, cr.top + len), sw)
    // TR
    drawLine(c, cr.topRight, Offset(cr.right - len, cr.top), sw)
    drawLine(c, cr.topRight, Offset(cr.right, cr.top + len), sw)
    // BL
    drawLine(c, cr.bottomLeft, Offset(cr.left + len, cr.bottom), sw)
    drawLine(c, cr.bottomLeft, Offset(cr.left, cr.bottom - len), sw)
    // BR
    drawLine(c, cr.bottomRight, Offset(cr.right - len, cr.bottom), sw)
    drawLine(c, cr.bottomRight, Offset(cr.right, cr.bottom - len), sw)
}

private fun DrawScope.drawEdgePills(cr: Rect) {
    val pw = 24.dp.toPx(); val ph = 4.dp.toPx()
    val r  = CornerRadius(ph / 2f)
    val mX = cr.left + cr.width / 2f; val mY = cr.top + cr.height / 2f
    listOf(cr.top, cr.bottom).forEach { y ->
        drawRoundRect(Color.White, Offset(mX - pw / 2f, y - ph / 2f), Size(pw, ph), r)
    }
    listOf(cr.left, cr.right).forEach { x ->
        drawRoundRect(Color.White, Offset(x - ph / 2f, mY - pw / 2f), Size(ph, pw), r)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Utilities
// ─────────────────────────────────────────────────────────────────────────────

private fun dist(a: Offset, b: Offset): Float {
    val dx = a.x - b.x; val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

private fun computeCropRect(canvas: Size, ratio: CropAspectRatio): Rect {
    val pad = 48f
    val mW  = canvas.width - pad * 2; val mH = canvas.height - pad * 2
    val (w, h) = when {
        ratio == CropAspectRatio.FREE || ratio.w == 0f -> mW to mH
        else -> { val rH = mW * ratio.h / ratio.w; if (rH <= mH) mW to rH else (mH * ratio.w / ratio.h) to mH }
    }
    return Rect((canvas.width - w) / 2f, (canvas.height - h) / 2f,
                (canvas.width + w) / 2f, (canvas.height + h) / 2f)
}

private fun computeCropRectCentered(center: Offset, canvas: Size, ratio: CropAspectRatio): Rect {
    val pad = 48f; val mW = canvas.width - pad * 2; val mH = canvas.height - pad * 2
    val rH = mW * ratio.h / ratio.w
    val (w, h) = if (rH <= mH) mW to rH else (mH * ratio.w / ratio.h) to mH
    val l = (center.x - w / 2f).coerceIn(pad, canvas.width - pad - w)
    val t = (center.y - h / 2f).coerceIn(pad, canvas.height - pad - h)
    return Rect(l, t, l + w, t + h)
}

// ─────────────────────────────────────────────────────────────────────────────
// Crop & save
// ─────────────────────────────────────────────────────────────────────────────

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
    if (imgScale <= 0f || canvas.width <= 0f || canvas.height <= 0f) return@withContext null

    val iW    = bm.width  * imgScale
    val iH    = bm.height * imgScale
    val iLeft = (canvas.width  - iW) / 2f + imgOffset.x
    val iTop  = (canvas.height - iH) / 2f + imgOffset.y
    val sX    = bm.width  / iW
    val sY    = bm.height / iH

    // Map crop rect from canvas space → bitmap pixel space
    val rawX = ((cr.left  - iLeft) * sX).toInt()
    val rawY = ((cr.top   - iTop)  * sY).toInt()
    val rawW = (cr.width  * sX).toInt()
    val rawH = (cr.height * sY).toInt()

    // Clamp strictly inside bitmap bounds to prevent IllegalArgumentException
    val bx = rawX.coerceIn(0, bm.width  - 1)
    val by = rawY.coerceIn(0, bm.height - 1)
    val bw = rawW.coerceIn(1, bm.width  - bx)
    val bh = rawH.coerceIn(1, bm.height - by)

    val cropped = runCatching { Bitmap.createBitmap(bm, bx, by, bw, bh) }.getOrElse { bm }

    // ──────────────────────────────────────────────────────────────────────────
    // IMPORTANT: Save to the same "picker/" directory that is already registered
    // in picker_file_paths.xml. This guarantees FileProvider can serve the URI.
    // Do NOT use a separate directory (e.g. "picker_cropped/") unless it is also
    // explicitly listed in picker_file_paths.xml.
    // ──────────────────────────────────────────────────────────────────────────
    val baseDir  = context.externalCacheDir ?: context.cacheDir
    val dir  = File(baseDir, "picker").also { it.mkdirs() }
    val file = File(dir, "CROP_${System.currentTimeMillis()}.jpg")

    runCatching {
        FileOutputStream(file).use { out ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.picker.fileprovider",
            file
        )
    }.getOrNull()
}
