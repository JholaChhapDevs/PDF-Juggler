package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TextPositionData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PdfMid(
    modifier: Modifier = Modifier,
    pageImage: ImageBitmap? = null,
    textData: List<TextPositionData> = emptyList(),
    rotation: Float = 0f,
    isFullscreen: Boolean = false,
    onTextSelected: (String) -> Unit = {},
    onViewportChanged: (IntSize) -> Unit = {},
    onRotateClockwise: () -> Unit = {},
    onRotateCounterClockwise: () -> Unit = {},
    onToggleFullscreen: () -> Unit = {},
    pageSizePoints: Size? = null,
    onZoomChanged: (Float) -> Unit = {},
    searchHighlightPositions: List<TextPositionData> = emptyList(),
    scrollToMatchTrigger: Int = 0,
    pageIndex: Int = 0,
    showToolbar: Boolean = true // New parameter to control toolbar visibility
) {
    val cs = MaterialTheme.colorScheme
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    // Text selection state
    var selectionStart by remember { mutableStateOf<Offset?>(null) }
    var selectionEnd by remember { mutableStateOf<Offset?>(null) }
    var selectedText by remember { mutableStateOf("") }
    var isSelecting by remember { mutableStateOf(false) }
    var selectedRectsNormalized by remember { mutableStateOf<List<Rect>>(emptyList()) }

    // Clear selection when switching pages (when textData changes)
    LaunchedEffect(textData) {
        selectionStart = null
        selectionEnd = null
        selectedText = ""
        isSelecting = false
        selectedRectsNormalized = emptyList()
    }

    // Notify parent about viewport changes
    LaunchedEffect(viewportSize) {
        onViewportChanged(viewportSize)
    }

    fun rotateRectNormalized(l: Float, t: Float, w: Float, h: Float, angle: Int): Rect {
        return when ((angle % 360 + 360) % 360) {
            90 -> {
                val nl = 1f - (t + h)
                val nt = l
                Rect(nl, nt, nl + h, nt + w)
            }
            180 -> {
                val nl = 1f - (l + w)
                val nt = 1f - (t + h)
                Rect(nl, nt, nl + w, nt + h)
            }
            270 -> {
                val nl = t
                val nt = 1f - (l + w)
                Rect(nl, nt, nl + h, nt + w)
            }
            else -> Rect(l, t, l + w, t + h)
        }
    }

    fun mergeRectsOnLines(rects: List<Rect>): List<Rect> {
        if (rects.isEmpty()) return emptyList()
        val sorted = rects.sortedWith(compareBy({ it.top }, { it.left }))
        val merged = mutableListOf<Rect>()

        val heights = sorted.map { it.height }.sorted()
        val medianH = heights[heights.size / 2]
        val lineTol = medianH * 0.6f
        val gapTol = medianH * 0.8f

        var currentLine = mutableListOf<Rect>()
        var currentLineTop = sorted.first().top

        fun flushLine() {
            if (currentLine.isEmpty()) return
            currentLine.sortBy { it.left }
            var acc = currentLine.first()
            for (i in 1 until currentLine.size) {
                val r = currentLine[i]
                val sameRow = abs(r.top - acc.top) <= lineTol
                val close = r.left - acc.right <= gapTol
                if (sameRow && close) {
                    acc = Rect(
                        left = acc.left,
                        top = minOf(acc.top, r.top),
                        right = maxOf(acc.right, r.right),
                        bottom = maxOf(acc.bottom, r.bottom)
                    )
                } else {
                    merged.add(acc)
                    acc = r
                }
            }
            merged.add(acc)
            currentLine.clear()
        }

        for (r in sorted) {
            if (abs(r.top - currentLineTop) <= lineTol) {
                currentLine.add(r)
            } else {
                flushLine()
                currentLine.add(r)
                currentLineTop = r.top
            }
        }
        flushLine()
        return merged
    }

    val textBoundsNormalized = remember(textData, pageImage, pageSizePoints, rotation, pageIndex) {
        val rot = ((rotation % 360f) + 360f) % 360f
        val rotInt = when {
            rot in 45f..135f -> 90
            rot in 135f..225f -> 180
            rot in 225f..315f -> 270
            else -> 0
        }

        val normalized: List<Pair<Rect, TextPositionData>> = if (pageSizePoints != null) {
            val pdfW = pageSizePoints.width
            val pdfH = pageSizePoints.height
            textData.mapIndexed { index, t ->
                val l = (t.x / pdfW)
                val w = (t.width / pdfW)
                val h = (t.height / pdfH)
                val top = ((t.y - t.height) / pdfH)
                val left = l.coerceAtLeast(0f)
                val top2 = top.coerceAtLeast(0f)
                val right = (l + w).coerceAtMost(1f)
                val bottom = (top + h).coerceAtMost(1f)

                val rect0 = Rect(
                    left = left,
                    top = top2,
                    right = right,
                    bottom = bottom
                )

                val rectR = rotateRectNormalized(rect0.left, rect0.top, rect0.width, rect0.height, rotInt)

                rectR to t
            }
        } else {
            pageImage?.let { image ->
                val pdfW = image.width.toFloat()
                val pdfH = image.height.toFloat()
                textData.map { t ->
                    val l = (t.x / pdfW)
                    val w = (t.width / pdfW)
                    val h = (t.height / pdfH)
                    val top = ((t.y - t.height) / pdfH)
                    val leftClamped = l.coerceAtLeast(0f)
                    val topClamped = top.coerceAtLeast(0f)
                    val rightClamped = (l + w).coerceAtMost(1f)
                    val bottomClamped = (top + h).coerceAtMost(1f)
                    val rect0 = Rect(
                        left = leftClamped,
                        top = topClamped,
                        right = rightClamped,
                        bottom = bottomClamped
                    )
                    val rectR = rotateRectNormalized(rect0.left, rect0.top, rect0.width, rect0.height, rotInt)
                    rectR to t
                }
            } ?: emptyList()
        }
        normalized
    }

    // Zoom & Pan state
    var zoomFactor by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var contentBaseSize by remember { mutableStateOf(IntSize.Zero) }

    val minZoom = 0.25f
    val maxZoom = 5f

    LaunchedEffect(zoomFactor) {
        if (zoomFactor < minZoom) {
            zoomFactor = minZoom
            panOffset = Offset.Zero
        } else if (zoomFactor > maxZoom) {
            zoomFactor = maxZoom
        }
        onZoomChanged(zoomFactor)
    }

    fun clampPan(viewSize: IntSize, base: IntSize, scale: Float, current: Offset): Offset {
        if (viewSize.width == 0 || viewSize.height == 0 || base.width == 0 || base.height == 0) return Offset.Zero
        val scaledW = base.width * scale
        val scaledH = base.height * scale
        val halfExcessW = max(0f, (scaledW - viewSize.width) / 2f)
        val halfExcessH = max(0f, (scaledH - viewSize.height) / 2f)
        val clampedX = current.x.coerceIn(-halfExcessW, halfExcessW)
        val clampedY = current.y.coerceIn(-halfExcessH, halfExcessH)
        return Offset(clampedX, clampedY)
    }

    fun setZoomAroundAnchor(newZoom: Float, anchorInView: Offset) {
        val oldZoom = zoomFactor
        val nz = newZoom.coerceIn(minZoom, maxZoom)
        if (contentBaseSize.width == 0 || contentBaseSize.height == 0) {
            zoomFactor = nz; return
        }
        val t = panOffset
        val tx = anchorInView.x - ((anchorInView.x - t.x) / oldZoom) * nz
        val ty = anchorInView.y - ((anchorInView.y - t.y) / oldZoom) * nz
        panOffset = clampPan(viewportSize, contentBaseSize, nz, Offset(tx, ty))
        zoomFactor = nz
    }

    fun resetZoom() {
        zoomFactor = 1f
        panOffset = Offset.Zero
    }
    fun zoomInStep() { setZoomAroundAnchor(zoomFactor * 1.25f, Offset(viewportSize.width/2f, viewportSize.height/2f)) }
    fun zoomOutStep() { setZoomAroundAnchor(zoomFactor / 1.25f, Offset(viewportSize.width/2f, viewportSize.height/2f)) }

    // Auto-scroll to search match
    LaunchedEffect(scrollToMatchTrigger, searchHighlightPositions, viewportSize, contentBaseSize) {
        if (scrollToMatchTrigger > 0 && searchHighlightPositions.isNotEmpty() &&
            viewportSize.width > 0 && viewportSize.height > 0 &&
            contentBaseSize.width > 0 && contentBaseSize.height > 0) {

            val posSet = searchHighlightPositions.toSet()
            val matchRects = textBoundsNormalized.filter { (_, tp) ->
                posSet.contains(tp)
            }.map { it.first }

            if (matchRects.isNotEmpty()) {
                val minLeft = matchRects.minOf { it.left }
                val maxRight = matchRects.maxOf { it.right }
                val minTop = matchRects.minOf { it.top }
                val maxBottom = matchRects.maxOf { it.bottom }

                val centerX = (minLeft + maxRight) / 2f
                val centerY = (minTop + maxBottom) / 2f

                val contentCenterX = centerX * contentBaseSize.width * zoomFactor
                val contentCenterY = centerY * contentBaseSize.height * zoomFactor

                val targetOffsetX = (viewportSize.width / 2f) - contentCenterX
                val targetOffsetY = (viewportSize.height / 2f) - contentCenterY

                val newOffset = clampPan(
                    viewportSize,
                    contentBaseSize,
                    zoomFactor,
                    Offset(targetOffsetX, targetOffsetY)
                )

                panOffset = newOffset
            }
        }
    }

    Surface(color = cs.background, tonalElevation = 0.dp, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .onSizeChanged { size -> viewportSize = size }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.isCtrlPressed) {
                        when (event.key) {
                            Key.Equals, Key.Plus, Key.NumPadAdd -> { zoomInStep(); true }
                            Key.Minus, Key.NumPadSubtract -> { zoomOutStep(); true }
                            Key.Zero, Key.NumPad0 -> { resetZoom(); true }
                            else -> false
                        }
                    } else false
                }
                .focusable(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (pageImage != null) {
                Surface(
                    color = cs.surface,
                    tonalElevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxSize(0.95f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val aspectRatio = pageImage.width.toFloat() / pageImage.height.toFloat()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspectRatio)
                                .onSizeChanged { contentBaseSize = it }
                                .graphicsLayer(
                                    scaleX = zoomFactor,
                                    scaleY = zoomFactor,
                                    translationX = panOffset.x,
                                    translationY = panOffset.y
                                )
                                .pointerInput("ctrl_wheel_zoom_and_pan") {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type == PointerEventType.Scroll) {
                                                val change = event.changes.firstOrNull() ?: continue
                                                val scroll = change.scrollDelta
                                                if (event.keyboardModifiers.isCtrlPressed) {
                                                    val factor = if (scroll.y > 0) 0.9f else 1.1f
                                                    val nz = (zoomFactor * factor)
                                                    setZoomAroundAnchor(nz, change.position)
                                                    change.consume()
                                                } else {
                                                    val scaledW = contentBaseSize.width * zoomFactor
                                                    val scaledH = contentBaseSize.height * zoomFactor
                                                    val canPan = scaledW > viewportSize.width || scaledH > viewportSize.height
                                                    if (canPan) {
                                                        val panDelta = Offset(
                                                            x = -scroll.x * 50f,
                                                            y = -scroll.y * 50f
                                                        )
                                                        panOffset = clampPan(
                                                            viewportSize,
                                                            contentBaseSize,
                                                            zoomFactor,
                                                            panOffset + panDelta
                                                        )
                                                        change.consume()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                .pointerInput("pinch_zoom_only") {
                                    detectTransformGestures(panZoomLock = false) { centroid, _, zoom, _ ->
                                        if (zoom != 1f) {
                                            setZoomAroundAnchor(zoomFactor * zoom, centroid)
                                        }
                                    }
                                }
                                .focusable()
                        ) {
                            Image(
                                bitmap = pageImage,
                                contentDescription = "Current Page",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspectRatio),
                                contentScale = ContentScale.FillWidth
                            )

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspectRatio)
                            ) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height

                                // Draw search highlights
                                if (searchHighlightPositions.isNotEmpty()) {
                                    val posSet = searchHighlightPositions.toSet()
                                    val matchRects = textBoundsNormalized.filter { (_, tp) ->
                                        posSet.contains(tp)
                                    }.map { it.first }
                                    val mergedMatch = mergeRectsOnLines(matchRects)
                                    mergedMatch.forEach { nb ->
                                        val left = nb.left * canvasWidth
                                        val top = nb.top * canvasHeight
                                        val rectWidth = nb.width * canvasWidth
                                        val rectHeight = nb.height * canvasHeight
                                        drawRect(
                                            color = androidx.compose.ui.graphics.Color(0xFF00BCD4).copy(alpha = 0.35f),
                                            topLeft = Offset(left, top),
                                            size = Size(rectWidth, rectHeight)
                                        )
                                    }
                                }

                                // Draw text selection highlights
                                val toDraw = mergeRectsOnLines(selectedRectsNormalized)
                                toDraw.forEach { nb ->
                                    val left = nb.left * canvasWidth
                                    val top = nb.top * canvasHeight
                                    val rectWidth = nb.width * canvasWidth
                                    val rectHeight = nb.height * canvasHeight
                                    drawRect(
                                        color = androidx.compose.ui.graphics.Color(0xFFFFEB3B).copy(alpha = 0.45f),
                                        topLeft = Offset(left, top),
                                        size = Size(rectWidth, rectHeight)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspectRatio)
                                    .pointerInput(textData, textBoundsNormalized) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()

                                                when (event.type) {
                                                    PointerEventType.Press -> {
                                                        val position = event.changes.first().position
                                                        selectionStart = position
                                                        selectionEnd = position
                                                        isSelecting = true
                                                        selectedText = ""
                                                        selectedRectsNormalized = emptyList()
                                                        event.changes.forEach { it.consume() }
                                                    }

                                                    PointerEventType.Move -> {
                                                        if (isSelecting) {
                                                            selectionEnd = event.changes.first().position
                                                            event.changes.forEach { it.consume() }

                                                            selectionStart?.let { start ->
                                                                selectionEnd?.let { end ->
                                                                    val minX = minOf(start.x, end.x)
                                                                    val minY = minOf(start.y, end.y)
                                                                    val maxX = maxOf(start.x, end.x)
                                                                    val maxY = maxOf(start.y, end.y)

                                                                    val width = size.width
                                                                    val height = size.height
                                                                    if (width > 0f && height > 0f) {
                                                                        val selNorm = Rect(
                                                                            left = (minX / width).coerceIn(0f, 1f),
                                                                            top = (minY / height).coerceIn(0f, 1f),
                                                                            right = (maxX / width).coerceIn(0f, 1f),
                                                                            bottom = (maxY / height).coerceIn(0f, 1f)
                                                                        )

                                                                        val selectedPairs = textBoundsNormalized.filter { (nb, _) ->
                                                                            nb.overlaps(selNorm)
                                                                        }.sortedWith(compareBy(
                                                                            { (nb, _) -> nb.top },
                                                                            { (nb, _) -> nb.left }
                                                                        ))

                                                                        selectedRectsNormalized = selectedPairs.map { it.first }
                                                                        selectedText = selectedPairs.joinToString("") { it.second.text }
                                                                        if (selectedText.isNotEmpty()) onTextSelected(selectedText)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    PointerEventType.Release -> {
                                                        if (isSelecting && selectedText.isNotBlank()) {
                                                            clipboardManager.setText(AnnotatedString(selectedText))
                                                        }
                                                        isSelecting = false
                                                        coroutineScope.launch {
                                                            delay(100)
                                                            selectionStart = null
                                                            selectionEnd = null
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                            )
                        }
                    }
                }
            } else {
                JText(
                    text = "No page to display",
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onBackground.copy(alpha = 0.6f)
                )
            }

            // Only show floating toolbar in fullscreen mode or when showToolbar is true
            if (showToolbar && isFullscreen) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            cs.surface.copy(alpha = 0.9f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onRotateCounterClockwise) {
                        Icon(Icons.AutoMirrored.Filled.RotateLeft, "Rotate Left")
                    }

                    IconButton(onClick = onRotateClockwise) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, "Rotate Right")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = { zoomOutStep() }, enabled = zoomFactor > minZoom + 1e-4f) {
                        Icon(Icons.Filled.ZoomOut, "Zoom Out")
                    }
                    JText(text = "${(zoomFactor * 100).toInt()}%")
                    IconButton(onClick = { zoomInStep() }) {
                        Icon(Icons.Filled.ZoomIn, "Zoom In")
                    }
                    IconButton(onClick = { resetZoom() }) {
                        Icon(Icons.Outlined.RestartAlt, "Reset Zoom")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onToggleFullscreen) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen"
                        )
                    }
                }
            }
        }
    }
}