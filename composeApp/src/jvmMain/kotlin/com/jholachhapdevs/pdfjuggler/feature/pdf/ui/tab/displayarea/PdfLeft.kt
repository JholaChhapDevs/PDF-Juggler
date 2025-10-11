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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TableOfContentData
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.BookmarkData

@Composable
fun PdfLeft(
    modifier: Modifier = Modifier,
    thumbnails: List<ImageBitmap> = emptyList(),
    tableOfContents: List<TableOfContentData> = emptyList(),
    bookmarks: List<BookmarkData> = emptyList(),
    selectedIndex: Int = 0,
    onThumbnailClick: (Int) -> Unit = {},
    onMovePageUp: (Int) -> Unit = {},
    onMovePageDown: (Int) -> Unit = {},
    onAddBookmark: (BookmarkData) -> Unit = {},
    onRemoveBookmark: (Int) -> Unit = {},
    onRemoveBookmarkForPage: (Int) -> Unit = {},
    onSaveBookmarksToMetadata: () -> Unit = {},
    hasPageChanges: Boolean = false,
    hasUnsavedBookmarks: Boolean = false,
    listState: LazyListState
) {
    val cs = MaterialTheme.colorScheme
    var selectedTab by remember { mutableStateOf(0) }

    Surface(
        color = cs.surface,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Icon-based tab row
            Surface(
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier
                                    .tabIndicatorOffset(tabPositions[selectedTab])
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                                height = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    divider = {}
                ) {
                    // Thumbnails Tab
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Photo,
                                contentDescription = "Thumbnails",
                                modifier = Modifier.size(24.dp),
                                tint = if (selectedTab == 0)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        },
                        modifier = Modifier
                            .background(Color.Transparent)
                            .padding(vertical = 8.dp)
                            .width(40.dp)
                    )

                    // Table of Contents Tab
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.TableRows,
                                contentDescription = "Contents",
                                modifier = Modifier.size(24.dp),
                                tint = if (selectedTab == 1)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        },
                        modifier = Modifier
                            .background(Color.Transparent)
                            .padding(vertical = 8.dp)
                            .width(40.dp)
                    )

                    // Bookmarks Tab
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Box {
                                Icon(
                                    imageVector = if (bookmarks.isNotEmpty()) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Bookmarks",
                                    modifier = Modifier.size(24.dp),
                                    tint = if (selectedTab == 2)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                if (bookmarks.isNotEmpty()) {
                                    Badge(
                                        containerColor = if (hasUnsavedBookmarks) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-6).dp)
                                    ) {
                                        Text(
                                            text = if (bookmarks.size > 99) "99+" else "${bookmarks.size}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .background(Color.Transparent)
                            .padding(vertical = 8.dp)
                            .width(40.dp)
                    )
                }
            }

            when (selectedTab) {
                0 -> ThumbnailView(
                    thumbnails, selectedIndex, onThumbnailClick,
                    onMovePageUp, onMovePageDown, hasPageChanges, listState,
                    bookmarks, onAddBookmark, onRemoveBookmarkForPage
                )
                1 -> TableOfContentsView(tableOfContents, onThumbnailClick)
                2 -> BookmarksView(
                    bookmarks, thumbnails, selectedIndex,
                    onThumbnailClick, onRemoveBookmark,
                    onSaveBookmarksToMetadata, hasUnsavedBookmarks
                )
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
    listState: LazyListState,
    bookmarks: List<BookmarkData>,
    onAddBookmark: (BookmarkData) -> Unit,
    onRemoveBookmarkForPage: (Int) -> Unit
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
                val isBookmarked = bookmarks.any { it.pageIndex == index }
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
                                        contentDescription = "Move down",
                                        tint = if (index < thumbnails.size - 1) cs.primary else cs.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (isBookmarked) {
                                            // Remove bookmark for this page
                                            onRemoveBookmarkForPage(index)
                                        } else {
                                            // Add bookmark directly without dialog
                                            onAddBookmark(BookmarkData(index, "Page ${index + 1}", ""))
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                                        tint = if (isBookmarked) cs.tertiary else cs.primary,
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
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )

                            if (isBookmarked) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = "Bookmarked",
                                    tint = cs.tertiary,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(16.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
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

@Composable
private fun TableOfContentsView(
    tableOfContents: List<TableOfContentData>,
    onThumbnailClick: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    if (tableOfContents.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = cs.onSurface.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(8.dp))
                JText(
                    text = "No table of contents",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.surface)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(tableOfContents) { _, item ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThumbnailClick(item.pageIndex) },
                    shape = RoundedCornerShape(8.dp),
                    color = cs.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        JText(
                            text = item.title,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(8.dp))
                        JText(
                            text = "${item.pageIndex + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarksView(
    bookmarks: List<BookmarkData>,
    thumbnails: List<ImageBitmap>,
    selectedIndex: Int,
    onThumbnailClick: (Int) -> Unit,
    onRemoveBookmark: (Int) -> Unit,
    onSaveBookmarksToMetadata: () -> Unit,
    hasUnsavedBookmarks: Boolean
) {
    val cs = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize()) {
        if (hasUnsavedBookmarks && bookmarks.isNotEmpty()) {
            Button(
                onClick = onSaveBookmarksToMetadata,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                JText("Save Bookmarks", fontWeight = FontWeight.Medium)
            }
        }

        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = cs.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(8.dp))
                    JText(
                        text = "No bookmarks yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(4.dp))
                    JText(
                        text = "Add bookmarks from thumbnails",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cs.surface)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(bookmarks) { idx, bookmark ->
                    val isSelected = bookmark.pageIndex == selectedIndex

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThumbnailClick(bookmark.pageIndex) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) cs.primaryContainer else cs.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isSelected) 4.dp else 1.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail preview
                            if (bookmark.pageIndex < thumbnails.size) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp, 85.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .border(1.dp, cs.outline.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                ) {
                                    Image(
                                        bitmap = thumbnails[bookmark.pageIndex],
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Bookmark,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = cs.tertiary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    JText(
                                        text = bookmark.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isSelected) cs.onPrimaryContainer else cs.onSurface
                                    )
                                }

                                Spacer(Modifier.height(4.dp))

                                JText(
                                    text = "Page ${bookmark.pageIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.primary
                                )
                            }

                            IconButton(
                                onClick = { onRemoveBookmark(idx) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Remove bookmark",
                                    tint = cs.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
