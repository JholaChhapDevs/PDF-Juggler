package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText
import com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.TableOfContentData

@Composable
fun TableOfContents(toc: List<TableOfContentData>, onItemSelected: (Int) -> Unit) {
    if (toc.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            JText("No table of contents found.", style = MaterialTheme.typography.bodySmall)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(toc) {
                TocEntry(it, onItemSelected)
            }
        }
    }
}

@Composable
private fun TocEntry(
    entry: TableOfContentData,
    onItemSelected: (Int) -> Unit,
    level: Int = 0
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemSelected(entry.pageIndex) }
                .padding(start = (level * 16).dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (entry.children.isNotEmpty()) {
                    val icon = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight
                    Icon(
                        imageVector = icon,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(16.dp).clickable { expanded = !expanded }
                    )
                    Spacer(Modifier.width(4.dp))
                }
                JText(
                    text = entry.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            JText(
                text = (entry.pageIndex + 1).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        if (expanded) {
            entry.children.forEach {
                TocEntry(it, onItemSelected, level + 1)
            }
        }
    }
}
