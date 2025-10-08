package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TextPositionData
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PdfMid(
    modifier: Modifier = Modifier,
    pageImage: ImageBitmap? = null,
    textData: List<TextPositionData> = emptyList(),
    onTextSelected: (String) -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val clipboardManager = LocalClipboardManager.current

    var selectionStart by remember { mutableStateOf<Offset?>(null) }
    var selectionEnd by remember { mutableStateOf<Offset?>(null) }
    var selectedText by remember { mutableStateOf("") }
    var isHoveringText by remember { mutableStateOf(false) }

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
                .padding(16.dp),
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
                    ) {
                        Image(
                            bitmap = pageImage,
                            contentDescription = "Current Page",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(textBounds) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            when (event.type) {
                                                PointerEventType.Move, PointerEventType.Enter -> {
                                                    val position = event.changes.first().position
                                                    isHoveringText = textBounds.any { (bounds, _) ->
                                                        bounds.contains(position)
                                                    }
                                                }
                                                PointerEventType.Exit -> {
                                                    isHoveringText = false
                                                }
                                            }
                                        }
                                    }
                                }
                                .pointerHoverIcon(
                                    if (isHoveringText) PointerIcon.Text else PointerIcon.Default
                                )
                                .pointerInput(textBounds) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            selectionStart = offset
                                            selectionEnd = offset
                                            selectedText = ""
                                        },
                                        onDrag = { change, _ ->
                                            selectionEnd = change.position

                                            val start = selectionStart ?: return@detectDragGestures
                                            val end = selectionEnd ?: return@detectDragGestures

                                            // Create selection rectangle
                                            val selectionRect = Rect(
                                                left = minOf(start.x, end.x),
                                                top = minOf(start.y, end.y),
                                                right = maxOf(start.x, end.x),
                                                bottom = maxOf(start.y, end.y)
                                            )

                                            // Find all text within selection with better filtering
                                            val selectedTexts = textBounds
                                                .filter { (bounds, _) ->
                                                    // Check if at least 50% of the text box is in selection
                                                    val intersection = bounds.intersect(selectionRect)
                                                    val textArea = bounds.width * bounds.height
                                                    val intersectionArea = intersection.width * intersection.height
                                                    intersectionArea > textArea * 0.3f // 30% overlap threshold
                                                }
                                                .sortedWith(compareBy(
                                                    { (bounds, _) -> bounds.top.toInt() / 20 }, // Group by line
                                                    { (bounds, _) -> bounds.left } // Then sort left to right
                                                ))
                                                .map { (_, text) -> text.text }

                                            selectedText = selectedTexts.joinToString("")
                                            onTextSelected(selectedText)
                                        },
                                        onDragEnd = {
                                            if (selectedText.isNotBlank()) {
                                                clipboardManager.setText(
                                                    AnnotatedString(selectedText)
                                                )
                                            }
                                            // Optionally clear selection after copying
                                            // selectionStart = null
                                            // selectionEnd = null
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
        }
    }
}