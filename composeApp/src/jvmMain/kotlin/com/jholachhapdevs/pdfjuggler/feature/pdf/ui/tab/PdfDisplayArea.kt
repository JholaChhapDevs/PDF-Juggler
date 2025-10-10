package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import com.jholachhapdevs.pdfjuggler.core.ui.components.JButton
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText
import com.jholachhapdevs.pdfjuggler.feature.ai.ui.AiChatComponent
import com.jholachhapdevs.pdfjuggler.feature.ai.ui.AiScreenModel
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component.SaveDialog
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea.PdfLeft
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea.PdfMid

@Composable
fun PdfSearchBar(
    model: TabScreenModel,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var query by remember { mutableStateOf(model.searchQuery) }

    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                }
                .focusable(),
            contentAlignment = Alignment.TopEnd
        ) {
            Surface(
                color = cs.surface,
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(350.dp)
                    .padding(top = 8.dp, end = 8.dp),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header row with title and close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        JText(
                            text = "Search PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = cs.onSurface
                        )

                        JButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("✕")
                        }
                    }

                    // Search input field
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            model.updateSearchQuery(it)
                        },
                        singleLine = true,
                        label = { JText("Search text") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Search results and controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val total = model.searchMatches.size
                        val current = if (model.currentSearchIndex >= 0) model.currentSearchIndex + 1 else 0

                        JText(
                            text = "$current of $total",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            JButton(
                                onClick = { model.goToPreviousMatch() },
                                enabled = total > 0
                            ) {
                                Text("Previous")
                            }

                            JButton(
                                onClick = { model.goToNextMatch() },
                                enabled = total > 0
                            ) {
                                Text("Next")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfDisplayArea(
    model: TabScreenModel,
    aiScreenModel: AiScreenModel? = null,
    isSearchVisible: Boolean = false,
    onSearchVisibilityChange: (Boolean) -> Unit = {}
) {
    val listState = rememberLazyListState()
    var showSaveAsDialog by remember { mutableStateOf(false) }

    // When this tab becomes active (composed), ensure the selected page is scrolled to top.
    LaunchedEffect(model.pdfFile.path) {
        if (model.thumbnails.isNotEmpty()) {
            val idx = model.selectedPageIndex.coerceIn(0, model.thumbnails.lastIndex)
            listState.scrollToItem(idx, 0)
        }
    }

    // Keep AI model in sync with the current selected page
    LaunchedEffect(model.selectedPageIndex) {
        aiScreenModel?.setSelectedPage(model.selectedPageIndex)
    }

    if (model.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        Box(
            Modifier
                .fillMaxSize()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.isCtrlPressed) {
                        when (event.key) {
                            Key.F -> { onSearchVisibilityChange(!isSearchVisible); true }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
                .focusable()
        ) {
            Column(Modifier.fillMaxSize()) {
                // Unified Save Controls Bar (shown when any changes exist)
                val hasAnyUnsaved = model.hasPageChanges || model.hasUnsavedBookmarks || model.hasUnsavedHighlights
                if (hasAnyUnsaved) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status text
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val parts = buildList {
                                    if (model.hasPageChanges) add("page order")
                                    if (model.hasUnsavedBookmarks) add("bookmarks")
                                    if (model.hasUnsavedHighlights) add("highlights")
                                }
                                val msg = if (parts.isEmpty()) "Unsaved changes" else "Unsaved: " + parts.joinToString(", ")
                                JText(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (model.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            // Action buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { model.resetPageOrder() },
                                    enabled = !model.isSaving && model.hasPageChanges
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Reset",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    JText("Reset order")
                                }
                                // Save (overwrite)
                                Button(
                                    onClick = { model.saveChanges() },
                                    enabled = !model.isSaving
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Save,
                                        contentDescription = "Save",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    JText("Save")
                                }
                                // Save As
                                Button(
                                    onClick = { showSaveAsDialog = true },
                                    enabled = !model.isSaving
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SaveAs,
                                        contentDescription = "Save As",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    JText("Save As")
                                }
                            }
                        }
                    }
                }

                // Main content
                Row(Modifier.fillMaxSize()) {
                    // Hide left pane when in fullscreen mode
                    if (!model.isFullscreen) {
                        PdfLeft(
                            modifier = Modifier.weight(0.15f).fillMaxSize(),
                            thumbnails = model.thumbnails,
                            tableOfContents = model.tableOfContent,
                            bookmarks = model.bookmarks,
                            selectedIndex = model.selectedPageIndex,
                            onThumbnailClick = { model.selectPage(it) },
                            onMovePageUp = { model.movePageUp(it) },
                            onMovePageDown = { model.movePageDown(it) },
                            onAddBookmark = { bookmark -> model.addBookmark(bookmark) },
                            onRemoveBookmark = { index -> model.removeBookmark(index) },
                            onRemoveBookmarkForPage = { pageIndex -> model.removeBookmarkForPage(pageIndex) },
                            onSaveBookmarksToMetadata = { model.saveBookmarksToMetadata() },
                            hasPageChanges = model.hasPageChanges,
                            hasUnsavedBookmarks = model.hasUnsavedBookmarks,
                            listState = listState
                        )
                    }

                    val originalPageIndex = model.getOriginalPageIndex(model.selectedPageIndex)
                    val pageSizePts = model.getPageSizePointsForDisplayIndex(model.selectedPageIndex)
                    val textDataForPage = model.allTextDataWithCoordinates[originalPageIndex] ?: emptyList()

                    PdfMid(
                        modifier = Modifier
                            .weight(if (model.isFullscreen) {
                                1f
                            } else if (aiScreenModel != null) {
                                0.55f
                            } else {
                                0.85f
                            })
                            .fillMaxSize(),
                        pageImage = model.currentPageImage,
                        textData = textDataForPage,
                        rotation = model.currentRotation,
                        isFullscreen = model.isFullscreen,
                        onTextSelected = { selectedText ->
                            // Handle selected text
                        },
                        onViewportChanged = { viewport ->
                            model.onViewportChanged(viewport)
                        },
                        onRotateClockwise = {
                            model.rotateClockwise()
                        },
                        onRotateCounterClockwise = {
                            model.rotateCounterClockwise()
                        },
                        onToggleFullscreen = {
                            model.toggleFullscreen()
                        },
                        pageSizePoints = pageSizePts,
                        onZoomChanged = { z -> model.onZoomChanged(z) },
                        searchHighlightPositions = model.currentMatchForDisplayedPage(),
                        scrollToMatchTrigger = model.scrollToMatchTrigger,
                        pageIndex = originalPageIndex,
                        pageHighlights = model.getHighlightsForDisplayedPage(),
                        onAddHighlight = { rects, color ->
                            if (rects.isNotEmpty()) {
                                model.addHighlightForDisplayedPage(rects, color, model.currentRotation)
                            }
                        },
                        onDictionaryRequest = { text -> model.requestAiDictionary(text) },
                        onTranslateRequest = { text -> model.requestAiTranslate(text) }
                        showToolbar = false,
                        externalZoom = model.currentZoom,
                        onZoomIn = { model.zoomIn() },
                        onZoomOut = { model.zoomOut() },
                        onResetZoom = { model.resetZoom() }
                    )

                    // AI Chat panel (only show when enabled and not in fullscreen)
                    aiScreenModel?.let { screenModel ->
                        if (!model.isFullscreen) {
                            Box(
                                modifier = Modifier
                                    .weight(0.3f)
                                    .fillMaxSize()
                                    .padding(start = 8.dp)
                            ) {
                                AiChatComponent(screenModel = screenModel)
                            }
                        }
                    }
                }
            }

            // Search popup overlay (positioned on top-right)
            PdfSearchBar(
                model = model,
                isVisible = isSearchVisible,
                onDismiss = { onSearchVisibilityChange(false) }
            )
        }
    }

    // Save dialogs
    SaveDialog(
        isOpen = showSaveAsDialog,
        currentFileName = model.pdfFile.path,
        isOverwrite = false,
        onDismiss = { showSaveAsDialog = false },
        onSave = { path ->
            showSaveAsDialog = false
            model.saveChangesAs(path)
        },
        onValidatePath = { path -> model.validateSavePath(path) }
    )
}