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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TextPositionData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    // New: size of the PDF page in points (mediaBox width/height)
    pageSizePoints: Size? = null
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

    // Vertical scroll state
    val scrollState = rememberScrollState()

    // Notify parent about viewport changes
    LaunchedEffect(viewportSize) {
        onViewportChanged(viewportSize)
    }

    fun rotateRectNormalized(l: Float, t: Float, w: Float, h: Float, angle: Int): Rect {
        // l,t,w,h are all in 0..1 space relative to the unrotated page with top-left origin
        return when ((angle % 360 + 360) % 360) {
            90 -> {
                // 90 CW
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
        // Sort by top then left
        val sorted = rects.sortedWith(compareBy({ it.top }, { it.left }))
        val merged = mutableListOf<Rect>()

        // Dynamic tolerances based on median height
        val heights = sorted.map { it.height }.sorted()
        val medianH = heights[heights.size / 2]
        val lineTol = medianH * 0.6f // vertical tolerance to consider same line
        val gapTol = medianH * 0.8f // horizontal gap tolerance to merge

        var currentLine = mutableListOf<Rect>()
        var currentLineTop = sorted.first().top

        fun flushLine() {
            if (currentLine.isEmpty()) return
            // Merge horizontally adjacent boxes in the current line
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

    // Create bounding boxes for text using PDF points -> normalized to 0..1, then scaled to canvas
    val textBoundsNormalized = remember(textData, pageImage, pageSizePoints, rotation) {
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
            textData.map { t ->
                // We use xDirAdj/yDirAdj from PDFBox => y increases downward from top-left origin
                val l = (t.x / pdfW)
                val w = (t.width / pdfW)
                val h = (t.height / pdfH)
                val top = ((t.y - t.height) / pdfH)
                val rect0 = Rect(
                    left = l.coerceIn(0f, 1f),
                    top = top.coerceIn(0f, 1f),
                    right = (l + w).coerceIn(0f, 1f),
                    bottom = (top + h).coerceIn(0f, 1f)
                )
                val rectR = rotateRectNormalized(rect0.left, rect0.top, rect0.width, rect0.height, rotInt)
                rectR to t
            }
        } else {
            // Fallback: approximate using bitmap pixels if page size unknown
            pageImage?.let { image ->
                val pdfW = image.width.toFloat()
                val pdfH = image.height.toFloat()
                textData.map { t ->
                    val l = (t.x / pdfW)
                    val w = (t.width / pdfW)
                    val h = (t.height / pdfH)
                    val top = ((t.y - t.height) / pdfH)
                    val rect0 = Rect(
                        left = l.coerceIn(0f, 1f),
                        top = top.coerceIn(0f, 1f),
                        right = (l + w).coerceIn(0f, 1f),
                        bottom = (top + h).coerceIn(0f, 1f)
                    )
                    val rectR = rotateRectNormalized(rect0.left, rect0.top, rect0.width, rect0.height, rotInt)
                    rectR to t
                }
            } ?: emptyList()
        }
        normalized
    }

    // Build merged, larger rectangles just for drawing
    val mergedTextBoundsNormalized = remember(textBoundsNormalized) {
        val rects = textBoundsNormalized.map { it.first }
        mergeRectsOnLines(rects)
    }

    Surface(
        color = cs.background,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .onSizeChanged { size -> viewportSize = size },
            contentAlignment = Alignment.TopCenter
        ) {
            if (pageImage != null) {
                Surface(
                    color = cs.surface,
                    tonalElevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxSize(0.95f)
                ) {
                    // Vertical scroll container
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            // Calculate aspect ratio
                            val aspectRatio = pageImage.width.toFloat() / pageImage.height.toFloat()

                            // PDF Image filling width
                            Image(
                                bitmap = pageImage,
                                contentDescription = "Current Page",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspectRatio),
                                contentScale = ContentScale.FillWidth
                            )

                            // Text Layer - Canvas for drawing text bounds
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspectRatio)
                            ) {
                                // Canvas size matches the displayed image size
                                val canvasWidth = size.width
                                val canvasHeight = size.height

                                // Draw merged text bounds using normalized coordinates scaled to canvas size
                                mergedTextBoundsNormalized.forEach { normalizedBounds ->
                                    val left = normalizedBounds.left * canvasWidth
                                    val top = normalizedBounds.top * canvasHeight
                                    val rectWidth = normalizedBounds.width * canvasWidth
                                    val rectHeight = normalizedBounds.height * canvasHeight

                                    drawRect(
                                        color = androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.25f),
                                        topLeft = Offset(left, top),
                                        size = Size(rectWidth, rectHeight)
                                    )
                                }

                                // Optionally, draw glyph boxes as thin outlines (debug)
                                // textBoundsNormalized.forEach { (nb, _) ->
                                //     val l = nb.left * canvasWidth
                                //     val t = nb.top * canvasHeight
                                //     val w = nb.width * canvasWidth
                                //     val h = nb.height * canvasHeight
                                //     drawRect(color = Color.Red.copy(alpha = 0.8f), topLeft = Offset(l, t), size = Size(w, h), style = Stroke(width = 1f))
                                // }

                                // Draw selection rectangle if active
                                selectionStart?.let { start ->
                                    selectionEnd?.let { end ->
                                        drawRect(
                                            color = androidx.compose.ui.graphics.Color(0x4000BFFF),
                                            topLeft = Offset(
                                                minOf(start.x, end.x),
                                                minOf(start.y, end.y)
                                            ),
                                            size = Size(
                                                abs(end.x - start.x),
                                                abs(end.y - start.y)
                                            )
                                        )
                                    }
                                }
                            }

                            // Pointer input for text selection - must match canvas size exactly
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspectRatio)
                                    .pointerInput("text_selection") {
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
                                                    }

                                                    PointerEventType.Move -> {
                                                        if (isSelecting) {
                                                            selectionEnd = event.changes.first().position

                                                            // Calculate selected text
                                                            selectionStart?.let { start ->
                                                                selectionEnd?.let { end ->
                                                                    val selectionRect = Rect(
                                                                        left = minOf(start.x, end.x),
                                                                        top = minOf(start.y, end.y),
                                                                        right = maxOf(start.x, end.x),
                                                                        bottom = maxOf(start.y, end.y)
                                                                    )

                                                                    // Use the pointer input scope size
                                                                    val canvasWidth = size.width
                                                                    val canvasHeight = size.height

                                                                    // Find text elements that intersect with selection
                                                                    val selectedTexts = textBoundsNormalized
                                                                        .filter { (normalizedBounds, _) ->
                                                                            val left = normalizedBounds.left * canvasWidth
                                                                            val top = normalizedBounds.top * canvasHeight
                                                                            val right = normalizedBounds.right * canvasWidth
                                                                            val bottom = normalizedBounds.bottom * canvasHeight

                                                                            val textRect = Rect(left, top, right, bottom)
                                                                            textRect.overlaps(selectionRect)
                                                                        }
                                                                        .sortedWith(compareBy(
                                                                            { (normalizedBounds, _) -> normalizedBounds.top },
                                                                            { (normalizedBounds, _) -> normalizedBounds.left }
                                                                        ))
                                                                        .map { (_, text) -> text.text }

                                                                    selectedText = selectedTexts.joinToString("")
                                                                    if (selectedText.isNotEmpty()) {
                                                                        onTextSelected(selectedText)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    PointerEventType.Release -> {
                                                        if (isSelecting && selectedText.isNotBlank()) {
                                                            clipboardManager.setText(AnnotatedString(selectedText))
                                                            println("Text copied to clipboard: $selectedText")
                                                        }
                                                        isSelecting = false
                                                        // Clear selection after a moment
                                                        coroutineScope.launch {
                                                            delay(500)
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

            // Rotation and fullscreen controls - positioned on top layer
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
                // Rotation controls
                IconButton(
                    onClick = onRotateCounterClockwise
                ) {
                    Icon(Icons.AutoMirrored.Filled.RotateLeft, "Rotate Left")
                }

                IconButton(
                    onClick = onRotateClockwise
                ) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, "Rotate Right")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Fullscreen toggle
                IconButton(
                    onClick = onToggleFullscreen
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen"
                    )
                }
            }
        }
    }
}