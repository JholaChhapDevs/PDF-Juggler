// Kotlin
package com.jholachhapdevs.pdfjuggler.feature.pdf.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component.TabBar

@Composable
fun PdfTabComponent(
    model: PdfTabScreenModel
) {
    val stack = LocalNavigator.currentOrThrow

    // If no tabs, exit to the previous screen
    if (model.tabs.isEmpty()) {
        LaunchedEffect(Unit) { stack.pop() }
        return
    }

    TabNavigator(model.tabs.first()) {
        val tabNavigator = LocalTabNavigator.current

        // Keep Voyager's current tab in sync with model.current
        LaunchedEffect(model.current, model.tabs.size) {
            val desired = model.current ?: model.tabs.firstOrNull()
            if (desired != null && tabNavigator.current != desired) {
                tabNavigator.current = desired
            }
        }

        Scaffold(
            topBar = {
                TabBar(
                    tabs = model.tabs,
                    onAdd = { model.addTabFromPicker() },
                    onSelect = { tab -> model.selectTab(tab) },
                    onClose = { tab -> model.closeTab(tab) }
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                CurrentTab()
            }
        }
    }
}