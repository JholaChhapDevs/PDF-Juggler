package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab

@Composable
fun TabBar(
    tabs: List<Tab>,
    onAdd: () -> Unit,
    onSelect: (Tab) -> Unit,
    onClose: (Tab) -> Unit
) {
    val navigator = LocalTabNavigator.current
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = navigator.current == tab
                    TabChip(
                        title = tab.options.title,
                        selected = selected,
                        onClick = { onSelect(tab) },
                        onClose = { onClose(tab) }
                    )
                }
            }
            IconButton(onClick = onAdd) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "New tab",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}