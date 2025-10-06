package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText

@Composable
fun PdfLeft(
    modifier: Modifier = Modifier,
    thumbnails: List<ImageBitmap> = emptyList(),
    selectedIndex: Int = 0,
    onThumbnailClick: (Int) -> Unit = {},
    listState: LazyListState
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        color = cs.surface,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(cs.surface)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(thumbnails) { index, thumbnail ->
                val isSelected = index == selectedIndex
                val borderColor = if (isSelected) cs.primary else cs.outline.copy(alpha = 0.3f)
                val backgroundColor = if (isSelected) cs.primaryContainer.copy(alpha = 0.2f) else cs.surface

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.707f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(backgroundColor)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onThumbnailClick(index) }
                            .padding(4.dp)
                    ) {
                        Image(
                            bitmap = thumbnail,
                            contentDescription = "Page ${index + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    JText(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) cs.primary else cs.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}