package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TextPositionData

/**
 * Manages search functionality for the PDF viewer
 */
class SearchManager(
    private val getAllTextDataWithCoordinates: () -> Map<Int, List<TextPositionData>>,
    private val getTotalPages: () -> Int,
    private val getPageOrder: () -> List<Int>,
    private val onNavigateToPage: (displayIndex: Int) -> Unit,
    private val onScrollToMatch: () -> Unit
) {
    var searchQuery by mutableStateOf("")
        private set
        
    var searchMatches by mutableStateOf<List<SearchMatch>>(emptyList())
        private set
        
    var currentSearchIndex by mutableStateOf(-1)
        private set

    /**
     * Update search query and recompute matches
     */
    fun updateSearchQuery(query: String) {
        searchQuery = query
        if (query.isBlank()) {
            clearSearch()
        } else {
            recomputeSearchMatches()
        }
    }

    /**
     * Clear search results
     */
    fun clearSearch() {
        searchMatches = emptyList()
        currentSearchIndex = -1
    }

    /**
     * Go to the next search match
     */
    fun goToNextMatch() {
        if (searchMatches.isEmpty()) return
        currentSearchIndex = (currentSearchIndex + 1) % searchMatches.size
        navigateToCurrentMatch()
    }

    /**
     * Go to the previous search match
     */
    fun goToPreviousMatch() {
        if (searchMatches.isEmpty()) return
        currentSearchIndex = if (currentSearchIndex <= 0) {
            searchMatches.size - 1
        } else {
            currentSearchIndex - 1
        }
        navigateToCurrentMatch()
    }

    /**
     * Get current search match for the displayed page
     */
    fun getCurrentMatchForDisplayedPage(selectedPageIndex: Int, getOriginalPageIndex: (Int) -> Int): List<TextPositionData> {
        val idx = currentSearchIndex
        if (idx < 0 || idx >= searchMatches.size) return emptyList()
        val match = searchMatches[idx]
        val displayedOriginal = getOriginalPageIndex(selectedPageIndex)
        return if (match.pageIndex == displayedOriginal) match.positions else emptyList()
    }

    /**
     * Recompute search matches when data or query changes
     */
    fun recomputeSearchMatches() {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            clearSearch()
            return
        }
        
        val matches = mutableListOf<SearchMatch>()
        val allTextData = getAllTextDataWithCoordinates()
        val totalPages = getTotalPages()
        val pageOrder = getPageOrder()
        
        for (originalPage in 0 until totalPages) {
            val items = allTextData[originalPage] ?: continue
            var i = 0
            while (i < items.size) {
                var k = 0
                var j = i
                val collected = mutableListOf<TextPositionData>()
                while (j < items.size && k < q.length) {
                    val t = items[j].text.lowercase()
                    val remaining = q.substring(k)
                    if (remaining.startsWith(t)) {
                        collected.add(items[j])
                        k += t.length
                        j++
                    } else {
                        break
                    }
                }
                if (k == q.length && collected.isNotEmpty()) {
                    matches.add(SearchMatch(originalPage, collected.toList()))
                    i = j
                } else {
                    i++
                }
            }
        }
        
        searchMatches = matches
        currentSearchIndex = if (matches.isNotEmpty()) 0 else -1
        
        // If first match exists and is on another page, navigate there
        val first = matches.firstOrNull()
        if (first != null) {
            val displayIdx = pageOrder.indexOf(first.pageIndex)
            if (displayIdx >= 0) {
                onNavigateToPage(displayIdx)
            }
        }
    }

    private fun navigateToCurrentMatch() {
        val match = searchMatches[currentSearchIndex]
        val pageOrder = getPageOrder()
        val displayIdx = pageOrder.indexOf(match.pageIndex)
        if (displayIdx >= 0) {
            onNavigateToPage(displayIdx)
            onScrollToMatch()
        }
    }
}