package com.jholachhapdevs.pdfjuggler.ui.pdf

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.domain.model.PdfTab


@Composable
fun TabBar(
    tabs: List<PdfTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onNewTab: () -> Unit,
    onCloseTab: (Int) -> Unit
) {

    Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onTabSelected(index) }
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(tab.title, modifier = Modifier.padding(end = 8.dp))
                    IconButton(onClick = { onCloseTab(index) }) {
                        Text("x")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onNewTab) {
            Text("+")
        }
    }
}