package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TableOfContentData

@Composable
fun PdfLeft(
    modifier: Modifier = Modifier,
    thumbnails: List<ImageBitmap> = emptyList(),
    tableOfContents: List<TableOfContentData> = emptyList(),
    selectedIndex: Int = 0,
    onThumbnailClick: (Int) -> Unit = {},
    onMovePageUp: (Int) -> Unit = {},
    onMovePageDown: (Int) -> Unit = {},
    hasPageChanges: Boolean = false,
    listState: LazyListState
) {
    val cs = MaterialTheme.colorScheme
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Thumbnails", "Contents")

    Surface(
        color = cs.surface,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { JText(title, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }

            when (selectedTab) {
                0 -> ThumbnailView(thumbnails, selectedIndex, onThumbnailClick, onMovePageUp, onMovePageDown, hasPageChanges, listState)
                1 -> TableOfContents(tableOfContents, onThumbnailClick)
            }
        }
    }
}

@Composable
private fun ThumbnailView(
    thumbnails: List<ImageBitmap>,
    selectedIndex: Int,
    onThumbnailClick: (Int) -> Unit,
    onMovePageUp: (Int) -> Unit,
    onMovePageDown: (Int) -> Unit,
    hasPageChanges: Boolean,
    listState: LazyListState
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize()) {
        if (hasPageChanges) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = cs.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                JText(
                    text = "Pages Reordered",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.primary
                )
            }
        }

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
                var isHovered by remember { mutableStateOf(false) }
                val isSelected = index == selectedIndex
                val borderColor = if (isSelected) cs.primary else cs.outline.copy(alpha = 0.3f)
                val backgroundColor = if (isSelected) cs.primaryContainer.copy(alpha = 0.2f) else cs.surface

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isHovered || isSelected) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(
                                onClick = { onMovePageUp(index) },
                                enabled = index > 0,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Move page up",
                                    tint = if (index > 0) cs.primary else cs.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { onMovePageDown(index) },
                                enabled = index < thumbnails.size - 1,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Move page down",
                                    tint = if (index < thumbnails.size - 1) cs.primary else cs.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

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
                            .hoverable(remember { MutableInteractionSource() })
                            .padding(4.dp)
                    ) {
                        LaunchedEffect(Unit) {
                            isHovered = false
                        }

                        Image(
                            bitmap = thumbnail,
                            contentDescription = "Page ${index + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .hoverable(remember { MutableInteractionSource() }, enabled = true),
                            contentScale = ContentScale.Fit
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .hoverable(remember { MutableInteractionSource() })
                                .clickable { 
                                    isHovered = !isHovered
                                    onThumbnailClick(index) 
                                }
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
