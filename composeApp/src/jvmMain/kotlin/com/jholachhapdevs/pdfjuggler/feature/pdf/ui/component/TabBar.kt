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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.RestartAlt
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
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        // Zoom Out
                        IconButton(
                            onClick = onZoomOut,
                            enabled = zoomFactor > minZoom + 1e-4f,
                            modifier = Modifier.width(40.dp).height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ZoomOut,
                                contentDescription = "Zoom Out",
                                tint = if (zoomFactor > minZoom + 1e-4f)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }

                        // Zoom percentage display
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            JText(
                                text = "${(zoomFactor * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        // Zoom In
                        IconButton(
                            onClick = onZoomIn,
                            modifier = Modifier.width(40.dp).height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ZoomIn,
                                contentDescription = "Zoom In",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Reset Zoom
                        IconButton(
                            onClick = onResetZoom,
                            modifier = Modifier.width(40.dp).height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RestartAlt,
                                contentDescription = "Reset Zoom",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Divider
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                .padding(horizontal = 4.dp)
                        )

                        // Rotate Left
                        IconButton(
                            onClick = onRotateCounterClockwise,
                            modifier = Modifier.width(40.dp).height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                                contentDescription = "Rotate Left",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Rotate Right
                        IconButton(
                            onClick = onRotateClockwise,
                            modifier = Modifier.width(40.dp).height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = "Rotate Right",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Right spacer
                    Spacer(modifier = Modifier.weight(1f))

                    // Right side: Print, AI, Split View, Search, Fullscreen
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        // Print
                        IconButton(
                            onClick = onPrint,
                            modifier = Modifier.width(40.dp).height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Print,
                                contentDescription = "Print",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // AI Chat toggle
                        IconButton(
                            onClick = onToggleAiChat,
                            modifier = Modifier.width(40.dp).height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = if (isAiChatEnabled) "Hide AI chat" else "Show AI chat",
                                tint = if (isAiChatEnabled)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        // Split view toggle
                        IconButton(
                            onClick = onToggleSplitView,
                            modifier = Modifier.width(40.dp).height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Splitscreen,
                                contentDescription = if (isSplitViewEnabled)
                                    "Disable split view"
                                else
                                    "Enable split view",
                                tint = if (isSplitViewEnabled)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        // Search
                        IconButton(
                            onClick = onSearchClick,
                            modifier = Modifier.width(40.dp).height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Divider
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                .padding(horizontal = 4.dp)
                        )

                        // Fullscreen toggle
                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier.width(40.dp).height(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullscreen)
                                    Icons.Filled.FullscreenExit
                                else
                                    Icons.Filled.Fullscreen,
                                contentDescription = if (isFullscreen)
                                    "Exit Fullscreen"
                                else
                                    "Enter Fullscreen",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}