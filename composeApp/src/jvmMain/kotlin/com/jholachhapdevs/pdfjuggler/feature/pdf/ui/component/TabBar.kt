package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText

@Composable
fun TabBar(
    tabs: List<Tab>,
    onAdd: () -> Unit,
    onSelect: (Tab) -> Unit,
    onClose: (Tab) -> Unit,
    onPrint: () -> Unit,
    isSplitViewEnabled: Boolean = false,
    onToggleSplitView: () -> Unit = {},
    isAiChatEnabled: Boolean = false,
    onToggleAiChat: () -> Unit = {},
    // PDF Viewer controls
    zoomFactor: Float = 1f,
    minZoom: Float = 0.25f,
    isFullscreen: Boolean = false,
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onResetZoom: () -> Unit = {},
    onRotateClockwise: () -> Unit = {},
    onRotateCounterClockwise: () -> Unit = {},
    onToggleFullscreen: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val navigator = LocalTabNavigator.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // First Row: Tabs and New Tab button
        Surface(tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.surface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scrollable tabs
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val selected = navigator.current == tab
                        TabChip(
                            title = tab.options.title,
                            selected = selected,
                            onClick = { onSelect(tab) },
                            onClose = { onClose(tab) }
                        )
                    }
                }

                // New Tab button
                IconButton(onClick = onAdd) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "New tab",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Second Row: Complete Toolbar (only show when tabs exist)
        if (tabs.isNotEmpty()) {
            Surface(
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left spacer
                    Spacer(modifier = Modifier.weight(1f))

                    // Center: Zoom and Rotation controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Zoom Out
                        IconButton(
                            onClick = onZoomOut,
                            enabled = zoomFactor > minZoom + 1e-4f
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ZoomOut,
                                contentDescription = "Zoom Out",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Zoom percentage display
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            JText(
                                text = "${(zoomFactor * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Zoom In
                        IconButton(onClick = onZoomIn) {
                            Icon(
                                imageVector = Icons.Filled.ZoomIn,
                                contentDescription = "Zoom In",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Reset Zoom
                        IconButton(onClick = onResetZoom) {
                            Icon(
                                imageVector = Icons.Outlined.RestartAlt,
                                contentDescription = "Reset Zoom",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Divider
                        VerticalDivider(
                            modifier = Modifier
                                .height(24.dp)
                                .padding(horizontal = 4.dp)
                        )

                        // Rotate Left
                        IconButton(onClick = onRotateCounterClockwise) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                                contentDescription = "Rotate Left",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Rotate Right
                        IconButton(onClick = onRotateClockwise) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = "Rotate Right",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Right spacer
                    Spacer(modifier = Modifier.weight(1f))

                    // Right side: Print, AI, Split View, Search, Fullscreen
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Print
                        IconButton(onClick = onPrint) {
                            Icon(
                                imageVector = Icons.Outlined.Print,
                                contentDescription = "Print",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // AI Chat toggle
                        IconButton(onClick = onToggleAiChat) {
                            Icon(
                                imageVector = Icons.Outlined.SmartToy,
                                contentDescription = if (isAiChatEnabled) "Hide AI chat" else "Show AI chat",
                                tint = if (isAiChatEnabled)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Split view toggle
                        IconButton(onClick = onToggleSplitView) {
                            Icon(
                                imageVector = if (isSplitViewEnabled)
                                    Icons.Filled.ViewDay
                                else
                                    Icons.Filled.ViewColumn,
                                contentDescription = if (isSplitViewEnabled)
                                    "Disable split view"
                                else
                                    "Enable split view",
                                tint = if (isSplitViewEnabled)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Search
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Divider
                        VerticalDivider(
                            modifier = Modifier
                                .height(24.dp)
                                .padding(horizontal = 4.dp)
                        )

                        // Fullscreen toggle
                        IconButton(onClick = onToggleFullscreen) {
                            Icon(
                                imageVector = if (isFullscreen)
                                    Icons.Default.FullscreenExit
                                else
                                    Icons.Default.Fullscreen,
                                contentDescription = if (isFullscreen)
                                    "Exit Fullscreen"
                                else
                                    "Enter Fullscreen",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}