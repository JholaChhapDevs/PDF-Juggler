package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Manages page operations for the PDF viewer
 */
class PageManager(
    private val onThumbnailsUpdate: suspend () -> Unit
) {
    // Page ordering - maps display order to original page indices
    var pageOrder by mutableStateOf<List<Int>>(emptyList())
        private set
    
    // Track if pages have been reordered
    var hasPageChanges by mutableStateOf(false)
        private set

    var selectedPageIndex by mutableStateOf(0)
        private set

    /**
     * Initialize page order with original sequence
     */
    fun initializePageOrder(totalPages: Int) {
        pageOrder = (0 until totalPages).toList()
        hasPageChanges = false
    }

    /**
     * Select a page by display index
     */
    fun selectPage(pageIndex: Int): Boolean {
        if (pageIndex < 0 || pageIndex >= pageOrder.size) return false
        selectedPageIndex = pageIndex
        return true
    }

    /**
     * Get the original page index for a given display index
     */
    fun getOriginalPageIndex(displayIndex: Int): Int {
        return if (displayIndex >= 0 && displayIndex < pageOrder.size) {
            pageOrder[displayIndex]
        } else {
            displayIndex
        }
    }

    /**
     * Move a page up in the order (decrease display position)
     */
    suspend fun movePageUp(displayIndex: Int) {
        if (displayIndex > 0 && displayIndex < pageOrder.size) {
            val newOrder = pageOrder.toMutableList()
            // Swap with previous item
            val temp = newOrder[displayIndex]
            newOrder[displayIndex] = newOrder[displayIndex - 1]
            newOrder[displayIndex - 1] = temp
            
            pageOrder = newOrder
            hasPageChanges = true
            
            // Update thumbnails to reflect new order
            onThumbnailsUpdate()
            
            // If the selected page was moved, update the selection
            if (selectedPageIndex == displayIndex) {
                selectedPageIndex = displayIndex - 1
            } else if (selectedPageIndex == displayIndex - 1) {
                selectedPageIndex = displayIndex
            }
        }
    }
    
    /**
     * Move a page down in the order (increase display position)
     */
    suspend fun movePageDown(displayIndex: Int) {
        if (displayIndex >= 0 && displayIndex < pageOrder.size - 1) {
            val newOrder = pageOrder.toMutableList()
            // Swap with next item
            val temp = newOrder[displayIndex]
            newOrder[displayIndex] = newOrder[displayIndex + 1]
            newOrder[displayIndex + 1] = temp
            
            pageOrder = newOrder
            hasPageChanges = true
            
            // Update thumbnails to reflect new order
            onThumbnailsUpdate()
            
            // If the selected page was moved, update the selection
            if (selectedPageIndex == displayIndex) {
                selectedPageIndex = displayIndex + 1
            } else if (selectedPageIndex == displayIndex + 1) {
                selectedPageIndex = displayIndex
            }
        }
    }
    
    /**
     * Move a page to a specific position in the order
     */
    suspend fun movePageToPosition(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0 || 
            fromIndex >= pageOrder.size || toIndex >= pageOrder.size) {
            return
        }
        
        val newOrder = pageOrder.toMutableList()
        val pageToMove = newOrder.removeAt(fromIndex)
        newOrder.add(toIndex, pageToMove)
        
        pageOrder = newOrder
        hasPageChanges = true
        
        // Update thumbnails to reflect new order
        onThumbnailsUpdate()
        
        // Update selected page index if necessary
        when {
            selectedPageIndex == fromIndex -> selectedPageIndex = toIndex
            selectedPageIndex in (minOf(fromIndex, toIndex) + 1)..maxOf(fromIndex, toIndex) -> {
                if (fromIndex < toIndex) selectedPageIndex-- else selectedPageIndex++
            }
        }
    }
    
    /**
     * Reset page order to original sequence
     */
    suspend fun resetPageOrder(totalPages: Int) {
        pageOrder = (0 until totalPages).toList()
        hasPageChanges = false
        onThumbnailsUpdate()
        // Keep current selected index (it will now refer to a different original page)
    }

    /**
     * Mark page changes as saved
     */
    fun markPageChangesSaved() {
        hasPageChanges = false
    }

    /**
     * Check if there are unsaved page changes
     */
    fun hasUnsavedPageChanges(): Boolean = hasPageChanges

    /**
     * Get current selected page display index
     */
    fun getCurrentSelectedPageIndex(): Int = selectedPageIndex

    /**
     * Get total pages count based on page order
     */
    fun getTotalPages(): Int = pageOrder.size
}