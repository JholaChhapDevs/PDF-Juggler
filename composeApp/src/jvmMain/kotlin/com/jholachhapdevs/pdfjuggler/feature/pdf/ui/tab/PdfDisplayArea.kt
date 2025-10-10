package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.core.ui.components.JButton
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component.SaveDialog
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea.PdfLeft
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea.PdfMid

@Composable
fun PdfSearchBar(model: TabScreenModel) {
    val cs = MaterialTheme.colorScheme
    var query by remember { mutableStateOf(model.searchQuery) }
    var isSearchVisible by remember { mutableStateOf(false) }
    
    // Search toggle button (positioned to trigger the popup)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        JButton(
            onClick = { isSearchVisible = !isSearchVisible },
            modifier = Modifier.padding(8.dp)
        ) {
            Text(if (isSearchVisible) "Hide Search" else "Search")
        }
    }
    
    // Search popup positioned on the right side
    if (isSearchVisible) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        isSearchVisible = false
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
                            onClick = { isSearchVisible = false },
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
    model: TabScreenModel
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
                            Key.F -> { /* Search shortcut handled by PdfSearchBar */; true }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
                .focusable()
        ) {
            Column(Modifier.fillMaxSize()) {
                // Save Controls Bar (shown when there are page changes)
                if (model.hasPageChanges) {
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
                                JText(
                                    text = "Pages have been reordered",
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
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Reset button
                                OutlinedButton(
                                    onClick = { model.resetPageOrder() },
                                    enabled = !model.isSaving
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Reset",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    JText("Reset")
                                }
                                
                                // Save As button
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
                            .weight(if (model.isFullscreen) 1f else 0.85f)
                            .fillMaxSize()
                            .padding(top = 24.dp), // add gap only above the PDF area
                        pageImage = model.currentPageImage,
                        textData = textDataForPage,
                        rotation = model.currentRotation,
                        isFullscreen = model.isFullscreen,
                        onTextSelected = { selectedText ->
                            // Handle selected text (no logging)
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
                        scrollToMatchTrigger = model.scrollToMatchTrigger
                    )
                }
            }
            
            // Search popup overlay (positioned on top-right)
            PdfSearchBar(model)
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
            model.savePdfAs(path)
        },
        onValidatePath = { path -> model.validateSavePath(path) }
    )
}
