package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.*
import com.jholachhapdevs.pdfjuggler.core.ui.components.JButton
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText
import com.jholachhapdevs.pdfjuggler.feature.ai.ui.AiChatComponent
import com.jholachhapdevs.pdfjuggler.feature.ai.ui.AiScreenModel
import com.jholachhapdevs.pdfjuggler.feature.tts.ui.TTSViewModel
import com.jholachhapdevs.pdfjuggler.feature.tts.ui.TTSFloatingCloseButton
import com.jholachhapdevs.pdfjuggler.feature.tts.ui.TTSProgressBar
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
    ttsViewModel: TTSViewModel? = null,
    isSearchVisible: Boolean = false,
    onSearchVisibilityChange: (Boolean) -> Unit = {},
    aiApiKey: String? = null
) {
    val listState = rememberLazyListState()
    var showSaveAsDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Ensure this area can receive keyboard events
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Track whether the PDF viewer has focus; only then handle arrow-navigation
    var viewerHasFocus by remember { mutableStateOf(false) }

    // Manage search visibility locally while syncing with external state
    var searchVisible by remember { mutableStateOf(isSearchVisible) }
    LaunchedEffect(isSearchVisible) { searchVisible = isSearchVisible }

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

    // Handle pending AI requests from context menu
    LaunchedEffect(model.pendingAiRequest) {
        val pendingRequest = model.pendingAiRequest
        if (pendingRequest != null && aiScreenModel != null) {
            // Process the pending request
            aiScreenModel.processPendingRequest(pendingRequest.text, pendingRequest.mode)
            // Clear the pending request
            model.clearPendingAiRequest()
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
                .onFocusChanged { viewerHasFocus = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        // Debug logging
                        // println("DEBUG KeyEvent: key=${'$'}{event.key}, ctrl=${'$'}{event.isCtrlPressed}")
                        when {
                            event.isCtrlPressed && event.key == Key.F -> {
                                println("DEBUG: Ctrl+F pressed -> toggle search (was ${'$'}searchVisible)")
                                searchVisible = !searchVisible
                                onSearchVisibilityChange(searchVisible)
                                true
                            }
                            event.isCtrlPressed && event.key == Key.S -> {
                                println("DEBUG: Ctrl+S pressed -> save")
                                if (!model.isSaving) { model.saveChanges() }
                                true
                            }
                            // Undo/Redo shortcuts
                            event.isCtrlPressed && event.isShiftPressed && event.key == Key.Z -> {
                                // Ctrl+Shift+Z -> Redo highlight (if possible)
                                if (model.canRedoHighlightForDisplayedPage()) {
                                    model.redoLastHighlightForDisplayedPage()
                                    true
                                } else false
                            }
                            event.isCtrlPressed && event.key == Key.Y -> {
                                // Ctrl+Y -> Redo highlight (Windows convention)
                                if (model.canRedoHighlightForDisplayedPage()) {
                                    model.redoLastHighlightForDisplayedPage()
                                    true
                                } else false
                            }
                            event.isCtrlPressed && event.key == Key.Z -> {
                                // Ctrl+Z -> Undo highlight if available
                                if (model.canUndoHighlightForDisplayedPage()) {
                                    model.undoLastHighlightForDisplayedPage()
                                    true
                                } else false
                            }
                            // Arrow key navigation (only when viewer has focus, search is not visible, and no Ctrl modifier)
                            viewerHasFocus && !event.isCtrlPressed && !searchVisible && (event.key == Key.DirectionUp || event.key == Key.DirectionLeft) -> {
                                val idx = model.selectedPageIndex
                                if (idx > 0) {
                                    println("DEBUG: Arrow Prev -> ${'$'}idx -> ${'$'}{idx - 1}")
                                    model.selectPage(idx - 1)
                                }
                                true
                            }
                            viewerHasFocus && !event.isCtrlPressed && !searchVisible && (event.key == Key.DirectionDown || event.key == Key.DirectionRight) -> {
                                val idx = model.selectedPageIndex
                                if (idx < model.totalPages - 1) {
                                    println("DEBUG: Arrow Next -> ${'$'}idx -> ${'$'}{idx + 1}")
                                    model.selectPage(idx + 1)
                                }
                                true
                            }
                            event.key == Key.Escape && searchVisible -> {
                                println("DEBUG: Esc pressed -> close search")
                                searchVisible = false
                                onSearchVisibilityChange(false)
                                focusRequester.requestFocus()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val e = awaitPointerEvent()
                            if (e.type == PointerEventType.Press) {
                                // Reclaim focus on any click/tap in viewer area
                                focusRequester.requestFocus()
                            }
                        }
                    }
                }
                .focusRequester(focusRequester)
                .focusable()
        ) {
            Column(Modifier.fillMaxSize()) {
                // TTS simple progress bar (when TTS is active)
                ttsViewModel?.let { tts ->
                    TTSProgressBar(
                        isActive = tts.isPlaying,
                        currentWords = tts.currentWords,
                        currentWordIndex = tts.currentWordIndex,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Action bar: show when page order changed OR there are unsaved highlights OR unsaved bookmarks
                val showActionBar = model.hasUnsavedHighlights || model.hasUnsavedBookmarks
                AnimatedVisibility (showActionBar) {
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
                                    if (model.hasUnsavedBookmarks) add("bookmarks")
                                    if (model.hasUnsavedHighlights) add("highlights")
                                }
                                val msg = if (parts.isEmpty()) "Unsaved changes" else "Unsaved: " + parts.joinToString(", ")
                                JText(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Undo/Redo for highlights on current page
                                if (model.canUndoHighlightForDisplayedPage()) {
                                    JButton(
                                        onClick = { model.undoLastHighlightForDisplayedPage() },
                                        enabled = !model.isSaving
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Undo,
                                            contentDescription = "Undo highlight",
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Undo",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (model.canRedoHighlightForDisplayedPage()) {
                                    JButton(
                                        onClick = { model.redoLastHighlightForDisplayedPage() },
                                        enabled = !model.isSaving
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Redo,
                                            contentDescription = "Redo highlight",
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Redo",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                // Save (overwrite)
                                JButton(
                                    onClick = { model.saveChanges() },
                                    enabled = !model.isSaving
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Save,
                                        contentDescription = "Save",
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Save",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                // Save As
                                JButton(
                                    onClick = { showSaveAsDialog = true },
                                    enabled = !model.isSaving
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SaveAs,
                                        contentDescription = "Save As",
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Save As",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                // Dismiss/revert button (closes bar by reverting changes)
                                IconButton(
                                    onClick = {
                                        model.revertAllUnsavedChanges()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Dismiss and revert",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
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
                            // Page reordering removed: move callbacks disabled
                            onAddBookmark = { bookmark -> model.addBookmark(bookmark) },
                            onRemoveBookmark = { index -> model.removeBookmark(index) },
                            onRemoveBookmarkForPage = { pageIndex -> model.removeBookmarkForPage(pageIndex) },
                            onSaveBookmarksToMetadata = { model.saveBookmarksToMetadata() },
                            hasUnsavedBookmarks = model.hasUnsavedBookmarks,
                            listState = listState
                        )
                    }

                    val originalPageIndex = model.getOriginalPageIndex(model.selectedPageIndex)
                    val pageSizePts = model.getPageSizePointsForDisplayIndex(model.selectedPageIndex)
                    val textDataForPage = model.allTextDataWithCoordinates[originalPageIndex] ?: emptyList()

                    // In PdfDisplayArea.kt, update the PdfMid() call with these new parameters:

                    // Compute background loading to show linear progress under page number
                    val showPageLoading = remember(model.progressiveThumbnails, model.pageImages, model.isTocCurrentlyLoading()) {
                        val thumbsPending = model.progressiveThumbnails.any { it == null }
                        val pagesPending = model.pageImages.any { it == null }
                        val tocPending = model.isTocCurrentlyLoading()
                        !model.isLoading && (thumbsPending || pagesPending || tocPending)
                    }

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
                            // Handle selected text for TTS
                            ttsViewModel?.setTextToSpeak(selectedText)
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
                        onTranslateRequest = { text -> model.requestAiTranslate(text) },
                        onSpeakRequest = { text ->
                            ttsViewModel?.let { tts ->
                                tts.setTextToSpeak(text)
                                tts.play()
                            }
                        },
                        showToolbar = false,
                        externalZoom = model.currentZoom,
                        onZoomIn = { model.zoomIn() },
                        onZoomOut = { model.zoomOut() },
                        onResetZoom = { model.resetZoom() },
                        // NEW: Navigation parameters
                        currentPageIndex = model.selectedPageIndex,
                        totalPages = model.totalPages,
                        onPreviousPage = {
                            if (model.selectedPageIndex > 0) {
                                model.selectPage(model.selectedPageIndex - 1)
                            }
                        },
                        onNextPage = {
                            if (model.selectedPageIndex < model.totalPages - 1) {
                                model.selectPage(model.selectedPageIndex + 1)
                            }
                        },
                        showLoading = showPageLoading
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
                isVisible = searchVisible,
                onDismiss = {
                    searchVisible = false
                    onSearchVisibilityChange(false)
                    // Ensure main viewer regains focus so Ctrl+F works again
                    focusRequester.requestFocus()
                }
            )
            
            // Floating TTS close button (when TTS is active)
            ttsViewModel?.let { tts ->
                if (tts.isPlaying) {
                    TTSFloatingCloseButton(
                        onClose = { tts.stop() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    )
                }
            }

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