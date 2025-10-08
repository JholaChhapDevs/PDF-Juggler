package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TextPositionData
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PdfMid(
    modifier: Modifier = Modifier,
    pageImage: ImageBitmap? = null,
    textData: List<TextPositionData> = emptyList(),
    rotation: Float = 0f,
    onTextSelected: (String) -> Unit = {},
    onZoomChanged: (Float) -> Unit = {},
    onViewportChanged: (IntSize) -> Unit = {},
    onRotateClockwise: () -> Unit = {},
    onRotateCounterClockwise: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val clipboardManager = LocalClipboardManager.current
    val density = LocalDensity.current

    // Zoom and pan state
    var zoomFactor by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Text selection state
    var selectionStart by remember { mutableStateOf<Offset?>(null) }
    var selectionEnd by remember { mutableStateOf<Offset?>(null) }
    var selectedText by remember { mutableStateOf("") }
    var isHoveringText by remember { mutableStateOf(false) }
    
    // Zoom constraints
    val minZoom = 0.25f
    val maxZoom = 5f
    
    // Notify parent about zoom changes
    LaunchedEffect(zoomFactor) {
        onZoomChanged(zoomFactor)
    }
    
    // Notify parent about viewport changes
    LaunchedEffect(viewportSize) {
        onViewportChanged(viewportSize)
    }

    // Create bounding boxes for text
    val textBounds = remember(textData) {
        textData.map { text ->
            // Estimate width based on text length (adjust multiplier as needed)
            val charWidth = 8f // Average character width in pixels
            val width = text.text.length * charWidth
            val height = 15f // Typical text height

            Rect(
                left = text.x,
                top = text.y,
                right = text.x + width,
                bottom = text.y + height
            ) to text
        }
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
            contentAlignment = Alignment.Center
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
                            .padding(16.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = pageImage,
                            contentDescription = "Current Page",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomFactor
                                    scaleY = zoomFactor
                                    translationX = panOffset.x
                                    translationY = panOffset.y
                                    transformOrigin = TransformOrigin.Center
                                },
                            contentScale = ContentScale.Inside
                        )

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomFactor
                                    scaleY = zoomFactor
                                    translationX = panOffset.x
                                    translationY = panOffset.y
                                    transformOrigin = TransformOrigin.Center
                                }
                                .pointerHoverIcon(
                                    if (isHoveringText) PointerIcon.Text else PointerIcon.Default
                                )
                                // Handle mouse scroll wheel and Ctrl+scroll for zoom
                                .onPointerEvent(PointerEventType.Scroll) { event ->
                                    val change = event.changes.first()
                                    val scrollDelta = change.scrollDelta
                                    
                                    // Check if Ctrl is pressed for zoom
                                    if (event.keyboardModifiers.isCtrlPressed) {
                                        // Ctrl+Scroll = Zoom
                                        val zoomDirection = if (scrollDelta.y > 0) 0.9f else 1.1f
                                        val oldZoom = zoomFactor
                                        val newZoom = (zoomFactor * zoomDirection).coerceIn(minZoom, maxZoom)
                                        zoomFactor = newZoom
                                        
                                        // Scale existing pan offset with zoom change
                                        val zoomRatio = newZoom / oldZoom
                                        panOffset = panOffset * zoomRatio
                                        
                                        change.consume()
                                    } else {
                                        // Regular scroll = Pan (when zoomed in)
                                        if (zoomFactor > 1f) {
                                            val panDelta = Offset(
                                                x = -scrollDelta.x * 50f, // Horizontal scroll
                                                y = -scrollDelta.y * 50f  // Vertical scroll
                                            )
                                            
                                            panOffset += panDelta
                                            
                                            // Constrain pan
                                            if (zoomFactor > 1f) {
                                                val maxPanX = size.width * (zoomFactor - 1) * 0.5f
                                                val maxPanY = size.height * (zoomFactor - 1) * 0.5f
                                                if (maxPanX > 0f && maxPanY > 0f) {
                                                    panOffset = Offset(
                                                        panOffset.x.coerceIn(-maxPanX, maxPanX),
                                                        panOffset.y.coerceIn(-maxPanY, maxPanY)
                                                    )
                                                }
                                            }
                                            
                                            change.consume()
                                        }
                                    }
                                }
                                // Handle trackpad pinch zoom gestures
                                .pointerInput("zoom_gestures") {
                                    detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, _ ->
                                        if (zoom != 1f) {
                                            // Handle zoom
                                            val oldZoom = zoomFactor
                                            val newZoom = (zoomFactor * zoom).coerceIn(minZoom, maxZoom)
                                            zoomFactor = newZoom
                                            
                                            // Scale existing pan offset with zoom change
                                            val zoomRatio = newZoom / oldZoom
                                            panOffset = panOffset * zoomRatio
                                        }
                                        
                                        if (pan != Offset.Zero && zoomFactor > 1f) {
                                            // Handle trackpad pan when zoomed in
                                            panOffset += pan
                                            
                                            // Constrain pan
                                            if (zoomFactor > 1f) {
                                                val maxPanX = size.width * (zoomFactor - 1) * 0.5f
                                                val maxPanY = size.height * (zoomFactor - 1) * 0.5f
                                                if (maxPanX > 0f && maxPanY > 0f) {
                                                    panOffset = Offset(
                                                        panOffset.x.coerceIn(-maxPanX, maxPanX),
                                                        panOffset.y.coerceIn(-maxPanY, maxPanY)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                // Handle mouse events and text selection
                                .pointerInput("mouse_text_selection") {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            
                                            // Handle hover for text cursor
                                            if (event.type == PointerEventType.Move) {
                                                val position = event.changes.first().position
                                                isHoveringText = textBounds.any { (bounds, _) ->
                                                    bounds.contains(position)
                                                }
                                            }
                                        }
                                    }
                                }
                                // Handle text selection with mouse drag
                                .pointerInput("text_selection") {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            // Only start text selection if over text and not heavily zoomed
                                            val isOverText = textBounds.any { (bounds, _) ->
                                                bounds.contains(offset)
                                            }
                                            
                                            if (isOverText && zoomFactor <= 2f) {
                                                selectionStart = offset
                                                selectionEnd = offset
                                                selectedText = ""
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            selectionStart?.let { start ->
                                                selectionEnd = change.position
                                                
                                                val end = selectionEnd ?: return@detectDragGestures
                                                
                                                val selectionRect = Rect(
                                                    left = minOf(start.x, end.x),
                                                    top = minOf(start.y, end.y),
                                                    right = maxOf(start.x, end.x),
                                                    bottom = maxOf(start.y, end.y)
                                                )
                                                
                                                val selectedTexts = textBounds
                                                    .filter { (bounds, _) ->
                                                        val intersection = bounds.intersect(selectionRect)
                                                        val textArea = bounds.width * bounds.height
                                                        val intersectionArea = intersection.width * intersection.height
                                                        intersectionArea > textArea * 0.3f
                                                    }
                                                    .sortedWith(compareBy(
                                                        { (bounds, _) -> bounds.top.toInt() / 20 },
                                                        { (bounds, _) -> bounds.left }
                                                    ))
                                                    .map { (_, text) -> text.text }
                                                
                                                selectedText = selectedTexts.joinToString("")
                                                onTextSelected(selectedText)
                                            }
                                        },
                                        onDragEnd = {
                                            if (selectedText.isNotBlank()) {
                                                clipboardManager.setText(AnnotatedString(selectedText))
                                            }
                                        }
                                    )
                                }
                        ) {
                            // Draw selection rectangle
                            selectionStart?.let { start ->
                                selectionEnd?.let { end ->
                                    drawRect(
                                        color = cs.primary.copy(alpha = 0.2f),
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
                    }
                }
            } else {
                JText(
                    text = "No page to display",
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onBackground.copy(alpha = 0.6f)
                )
            }
            
            // Zoom controls - positioned on top layer
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
                IconButton(
                    onClick = {
                        val oldZoom = zoomFactor
                        val newZoom = max(minZoom, zoomFactor * 0.8f)
                        zoomFactor = newZoom
                        
                        // Scale existing pan offset with zoom change
                        val zoomRatio = newZoom / oldZoom
                        panOffset = panOffset * zoomRatio
                    },
                    enabled = zoomFactor > minZoom
                ) {
                    Icon(Icons.Default.ZoomOut, "Zoom Out")
                }
                
                Text(
                    text = "${(zoomFactor * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurface
                )
                
                IconButton(
                    onClick = {
                        val oldZoom = zoomFactor
                        val newZoom = min(maxZoom, zoomFactor * 1.25f)
                        zoomFactor = newZoom
                        
                        // Scale existing pan offset with zoom change
                        val zoomRatio = newZoom / oldZoom
                        panOffset = panOffset * zoomRatio
                    },
                    enabled = zoomFactor < maxZoom
                ) {
                    Icon(Icons.Default.ZoomIn, "Zoom In")
                }
                
                Button(
                    onClick = {
                        zoomFactor = 1f
                        panOffset = Offset.Zero
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Fit",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Rotation controls
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = onRotateCounterClockwise
                ) {
                    Icon(Icons.Default.RotateLeft, "Rotate Left")
                }
                
                IconButton(
                    onClick = onRotateClockwise
                ) {
                    Icon(Icons.Default.RotateRight, "Rotate Right")
                }
            }
        }
    }
}