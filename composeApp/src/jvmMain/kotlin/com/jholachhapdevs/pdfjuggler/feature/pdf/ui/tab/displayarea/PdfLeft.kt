package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onMovePageUp: (Int) -> Unit = {},
    onMovePageDown: (Int) -> Unit = {},
    hasPageChanges: Boolean = false,
    listState: LazyListState
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        color = cs.surface,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with changes indicator
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
                        // Page reordering controls - shown on hover or selection
                        if (isHovered || isSelected) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Move up button
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
                                
                                // Move down button
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
                        
                        // Thumbnail
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
                                .hoverable(
                                    interactionSource = remember { MutableInteractionSource() }
                                )
                                .padding(4.dp)
                        ) {
                            // Detect hover state
                            LaunchedEffect(Unit) {
                                isHovered = false
                            }
                            
                            Image(
                                bitmap = thumbnail,
                                contentDescription = "Page ${index + 1}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .hoverable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        enabled = true
                                    ),
                                contentScale = ContentScale.Fit
                            )
                            
                            // Hover detection overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .hoverable(
                                        interactionSource = remember { MutableInteractionSource() }
                                    )
                                    .clickable { 
                                        isHovered = !isHovered
                                        onThumbnailClick(index) 
                                    }
                            )
                        }
                        
                        Spacer(Modifier.height(4.dp))
                        
                        // Page number
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
}