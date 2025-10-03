package com.jholachhapdevs.pdfjuggler.ui.pdf

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * LeftPanel shows a vertical list of page thumbnails.
 *
 * - thumbnails: list of optional ImageBitmap (one per page). If an item is null,
 *   a placeholder surface with "Page n" will be rendered.
 * - currentPage: index of active page (0-based)
 * - onPageClick: called with page index when a thumbnail is clicked
 */
@Composable
fun LeftPanel(
    thumbnails: List<ImageBitmap?>,
    currentPage: Int,
    onPageClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxHeight()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (thumbnails.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        tonalElevation = 2.dp,
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("No pages", fontSize = 14.sp)
                        }
                    }
                }
            } else {
                itemsIndexed(thumbnails) { index, bmp ->
                    val isSelected = index == currentPage
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clickable { onPageClick(index) },
                        tonalElevation = if (isSelected) 8.dp else 2.dp,
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) Color(0xFF006AFF) else Color.Gray
                        )
                    ) {
                        if (bmp != null) {
                            // Thumbnail image available
                            Image(
                                bitmap = bmp,
                                contentDescription = "Thumbnail page ${index + 1}",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Placeholder if thumbnail not ready
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Page ${index + 1}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
