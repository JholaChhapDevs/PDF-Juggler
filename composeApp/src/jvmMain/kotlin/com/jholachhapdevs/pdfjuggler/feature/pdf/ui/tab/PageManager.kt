package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * PageManager: reordering removed. This manager now maintains a stable identity page order
 * and exposes selection helpers. All reordering APIs are intentionally no-ops to remove
 * the page reordering feature while keeping the rest of the UI/API stable.
 */
class PageManager {
    // Page ordering - maps display order to original page indices
    var pageOrder by mutableStateOf<List<Int>>(emptyList())
        private set
    
    // Track if pages have been reordered (feature removed -> always false)
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
     * Move a page up in the order (no-op since reordering is removed)
     */
    fun movePageUp(displayIndex: Int) {
        // Reordering disabled: do nothing and keep state unchanged
    }
    
    /**
     * Move a page down in the order (no-op since reordering is removed)
     */
    fun movePageDown(displayIndex: Int) {
        // Reordering disabled: do nothing
    }
    
    /**
     * Move a page to a specific position in the order (no-op)
     */
    fun movePageToPosition(fromIndex: Int, toIndex: Int) {
        // Reordering disabled: ignore
    }
    
    /**
     * Reset page order to original sequence
     */
    fun resetPageOrder(totalPages: Int) {
        // Already identity - reinitialize to be safe
        pageOrder = (0 until totalPages).toList()
        hasPageChanges = false
    }

    /**
     * Mark page changes as saved (no-op)
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