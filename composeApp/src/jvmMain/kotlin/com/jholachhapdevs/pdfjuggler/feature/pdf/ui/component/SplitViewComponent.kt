package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.Tab
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.PdfDisplayArea
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.PdfTab
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.TabScreenModel
import com.jholachhapdevs.pdfjuggler.feature.tts.ui.TTSViewModel

@Composable
fun SplitViewComponent(
    leftModel: TabScreenModel?,
    rightModel: TabScreenModel?,
    availableTabs: List<Tab> = emptyList(),
    onLeftTabChange: (Tab?) -> Unit = {},
    onRightTabChange: (Tab?) -> Unit = {},
    modifier: Modifier = Modifier,
    ttsViewModel: TTSViewModel? = null
) {
    var splitRatio by remember { mutableStateOf(0.5f) }

    Row(modifier = modifier.fillMaxSize()) {
        // Left pane
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(splitRatio)
        ) {
            if (leftModel != null) {
                Column(Modifier.fillMaxSize()) {
                    if (availableTabs.isNotEmpty()) {
                        SplitPaneHeader(
                            title = "Left Pane",
                            selectedTab = availableTabs.find { (it as? PdfTab)?.pdfFile?.path == leftModel.pdfFile.path },
                            availableTabs = availableTabs,
                            onTabSelected = onLeftTabChange
                        )
                    }
                    PdfDisplayArea(model = leftModel, ttsViewModel = ttsViewModel)
                }
            } else {
                EmptyPaneMessage("Select a document for the left pane")
            }
        }

        // Divider (draggable)
        if (leftModel != null || rightModel != null) {
            DraggableDivider(
                onDrag = { delta ->
                    val newRatio = (splitRatio + delta).coerceIn(0.2f, 0.8f)
                    splitRatio = newRatio
                }
            )
        }

        // Right pane
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f - splitRatio)
        ) {
            if (rightModel != null) {
                Column(Modifier.fillMaxSize()) {
                    if (availableTabs.isNotEmpty()) {
                        SplitPaneHeader(
                            title = "Right Pane",
                            selectedTab = availableTabs.find { (it as? PdfTab)?.pdfFile?.path == rightModel.pdfFile.path },
                            availableTabs = availableTabs,
                            onTabSelected = onRightTabChange
                        )
                    }
                    PdfDisplayArea(model = rightModel, ttsViewModel = ttsViewModel)
                }
            } else {
                EmptyPaneMessage("Select a document for the right pane")
            }
        }
    }
}

@Composable
private fun SplitPaneHeader(
    title: String,
    selectedTab: Tab?,
    availableTabs: List<Tab>,
    onTabSelected: (Tab?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            JText(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                TextButton(
                    onClick = { expanded = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    JText(
                        text = selectedTab?.options?.title ?: "None",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select document",
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableTabs.forEach { tab ->
                        DropdownMenuItem(
                            text = { JText(tab.options.title) },
                            onClick = {
                                onTabSelected(tab)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPaneMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        JText(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun DraggableDivider(
    onDrag: (Float) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(12.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x / size.width)
                }
            },
        color = if (isDragging)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp),
        tonalElevation = if (isDragging) 4.dp else 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to resize",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
