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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.*
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TextPositionData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.HighlightMark

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
    // Page index for cache invalidation
    pageIndex: Int = 0,
    // New: persisted highlights for this page (normalized, unrotated)
    pageHighlights: List<HighlightMark> = emptyList(),
    // New: callback when user chooses Highlight from context menu
    onAddHighlight: (rectsNormalized: List<Rect>, colorArgb: Long) -> Unit = { _, _ -> },
    // New: AI actions
    onDictionaryRequest: (text: String) -> Unit = {},
    onTranslateRequest: (text: String) -> Unit = {}
    pageIndex: Int = 0,
    showToolbar: Boolean = true,
    externalZoom: Float = 1f,
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onResetZoom: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    // Text selection state
    var selectionStart by remember { mutableStateOf<Offset?>(null) }
    var selectionEnd by remember { mutableStateOf<Offset?>(null) }
    var selectedText by remember { mutableStateOf("") }
    var isSelecting by remember { mutableStateOf(false) }
    var selectedRectsNormalized by remember { mutableStateOf<List<Rect>>(emptyList()) }

    // Context menu state
    var ctxMenuOpen by remember { mutableStateOf(false) }
    var ctxMenuPos by remember { mutableStateOf(Offset.Zero) }

    // Clear selection when switching pages (when textData changes)
    LaunchedEffect(textData) {
        selectionStart = null
        selectionEnd = null
        selectedText = ""
        isSelecting = false
        selectedRectsNormalized = emptyList()
        ctxMenuOpen = false
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
        val lineTol = medianH * 0.7f // a bit more tolerant vertically
        val gapTol = medianH * 1.0f // wider gap tolerance to better connect fragments

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

    // Use external zoom instead of local state
    val zoomFactor = externalZoom
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var contentBaseSize by remember { mutableStateOf(IntSize.Zero) }

    val minZoom = 0.25f
    val maxZoom = 5f

    LaunchedEffect(zoomFactor) {
        if (zoomFactor < minZoom) {
            panOffset = Offset.Zero
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
            return
        }
        val t = panOffset
        val tx = anchorInView.x - ((anchorInView.x - t.x) / oldZoom) * nz
        val ty = anchorInView.y - ((anchorInView.y - t.y) / oldZoom) * nz
        panOffset = clampPan(viewportSize, contentBaseSize, nz, Offset(tx, ty))
    }

    fun resetZoomLocal() {
        panOffset = Offset.Zero
        onResetZoom()
    }

    fun zoomInStep() {
        onZoomIn()
    }

    fun zoomOutStep() {
        onZoomOut()
    }

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
                            Key.Zero, Key.NumPad0 -> { resetZoomLocal(); true }
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
                                                    if (scroll.y > 0) onZoomOut() else onZoomIn()
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
                                            if (zoom > 1f) onZoomIn() else onZoomOut()
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

                            // Text/Highlight Layer - Canvas
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspectRatio)
                            ) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height

                                // Slightly rounder corners for a smoother band
                                val corner = 6.dp.toPx()

                                // Expand each highlight band more, with sensible minimum padding
                                fun drawRoundedRectNorm(rect: Rect, color: androidx.compose.ui.graphics.Color) {
                                    val bandHeightPx = rect.height * canvasHeight
                                    val minVPad = 1.5.dp.toPx()
                                    val minHPad = 1.0.dp.toPx()
                                    // ~22% vertical padding each side; ~10% horizontal padding each side
                                    val vPad = kotlin.math.max(bandHeightPx * 0.22f, minVPad)
                                    val hPad = kotlin.math.max(bandHeightPx * 0.10f, minHPad)
                                    val left = rect.left * canvasWidth - hPad
                                    val top = rect.top * canvasHeight - vPad
                                    val rectWidth = rect.width * canvasWidth + 2 * hPad
                                    val rectHeight = bandHeightPx + 2 * vPad
                                    drawRoundRect(
                                        color = color,
                                        topLeft = Offset(left, top),
                                        size = Size(rectWidth, rectHeight),
                                        cornerRadius = CornerRadius(corner, corner)
                                    )
                                }

                                // First: draw search match highlight rectangles in cyan
                                if (searchHighlightPositions.isNotEmpty()) {
                                    val posSet = searchHighlightPositions.toSet()
                                    val matchRects = textBoundsNormalized.filter { (_, tp) ->
                                        posSet.contains(tp)
                                    }.map { it.first }
                                    val mergedMatch = mergeRectsOnLines(matchRects)
                                    mergedMatch.forEach { nb ->
                                        drawRoundedRectNorm(
                                            nb,
                                            androidx.compose.ui.graphics.Color(0xFF00BCD4).copy(alpha = 0.35f)
                                        )
                                    }
                                }

                                // Persisted page highlights (different colors)
                                if (pageHighlights.isNotEmpty()) {
                                    val rot = ((rotation % 360f) + 360f) % 360f
                                    val rotInt = when {
                                        rot in 45f..135f -> 90
                                        rot in 135f..225f -> 180
                                        rot in 225f..315f -> 270
                                        else -> 0
                                    }
                                    pageHighlights.forEach { mark ->
                                        val rotated = mark.rects.map { ur ->
                                            rotateRectNormalized(ur.left, ur.top, ur.width, ur.height, rotInt)
                                        }
                                        val merged = mergeRectsOnLines(rotated)
                                        val colorInt = (mark.colorArgb and 0xFFFFFFFF).toInt()
                                        val color = androidx.compose.ui.graphics.Color(colorInt).copy(alpha = 0.35f)
                                        merged.forEach { nb ->
                                            drawRoundedRectNorm(nb, color)
                                        }
                                    }
                                }

                                // Then: draw selected text highlight rectangles in yellow
                                val toDraw = mergeRectsOnLines(selectedRectsNormalized)
                                toDraw.forEach { nb ->
                                    drawRoundedRectNorm(
                                        nb,
                                        androidx.compose.ui.graphics.Color(0xFFFFEB3B).copy(alpha = 0.45f)
                                    )
                                }
                            }

                            // Pointer input for text selection and right-click context menu
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
                                                        // Right click -> open context menu if selection exists
                                                        if (event.buttons.isSecondaryPressed) {
                                                            if (selectedRectsNormalized.isNotEmpty()) {
                                                                ctxMenuPos = position
                                                                ctxMenuOpen = true
                                                                event.changes.forEach { it.consume() }
                                                                continue
                                                            }
                                                        }
                                                        // Left click -> begin selection
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

                            // Context menu for highlighting
                            val menuOffset = run {
                                val densityVal = density.density
                                DpOffset((ctxMenuPos.x / densityVal).dp, (ctxMenuPos.y / densityVal).dp)
                            }
                            DropdownMenu(
                                expanded = ctxMenuOpen,
                                onDismissRequest = { ctxMenuOpen = false },
                                offset = menuOffset
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Highlight - Yellow") },
                                    onClick = {
                                        onAddHighlight(selectedRectsNormalized, 0xFFFFEB3BL)
                                        ctxMenuOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Highlight - Green") },
                                    onClick = {
                                        onAddHighlight(selectedRectsNormalized, 0xFF8BC34AL)
                                        ctxMenuOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Highlight - Pink") },
                                    onClick = {
                                        onAddHighlight(selectedRectsNormalized, 0xFFE91E63L)
                                        ctxMenuOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Highlight - Blue") },
                                    onClick = {
                                        onAddHighlight(selectedRectsNormalized, 0xFF2196F3L)
                                        ctxMenuOpen = false
                                    }
                                )
                                HorizontalDivider()
                                // New combined AI option
                                DropdownMenuItem(
                                    text = { Text("Dictionary & Translate (AI)") },
                                    onClick = {
                                        val textSel = selectedText.trim()
                                        if (textSel.isNotEmpty()) {
                                            clipboardManager.setText(AnnotatedString(textSel))
                                            // Use dictionary request to trigger the combined flow
                                            onDictionaryRequest(textSel)
                                        }
                                        ctxMenuOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Dictionary (AI)") },
                                    onClick = {
                                        val textSel = selectedText.trim()
                                        if (textSel.isNotEmpty()) {
                                            clipboardManager.setText(AnnotatedString(textSel))
                                            onDictionaryRequest(textSel)
                                        }
                                        ctxMenuOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Translate (AI)") },
                                    onClick = {
                                        val textSel = selectedText.trim()
                                        if (textSel.isNotEmpty()) {
                                            clipboardManager.setText(AnnotatedString(textSel))
                                            onTranslateRequest(textSel)
                                        }
                                        ctxMenuOpen = false
                                    }
                                )
                            }
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

                    IconButton(onClick = { onZoomOut() }, enabled = zoomFactor > minZoom + 1e-4f) {
                        Icon(Icons.Filled.ZoomOut, "Zoom Out")
                    }
                    JText(text = "${(zoomFactor * 100).toInt()}%")
                    IconButton(onClick = { onZoomIn() }) {
                        Icon(Icons.Filled.ZoomIn, "Zoom In")
                    }
                    IconButton(onClick = { onResetZoom() }) {
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