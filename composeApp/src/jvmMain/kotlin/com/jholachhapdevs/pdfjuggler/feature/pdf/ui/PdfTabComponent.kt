package com.jholachhapdevs.pdfjuggler.feature.pdf.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component.AdvancedPrintOptionsDialog
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component.PrintProgressDialog
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component.TabBar
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component.SplitViewComponent
import com.jholachhapdevs.pdfjuggler.service.PdfGenerationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun PdfTabComponent(
    model: PdfTabScreenModel
) {
    val stack = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()

    var showPrintOptionsDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("Printing...") }

    val pdfGenerationService = remember { PdfGenerationService() }

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
                    onClose = { tab -> model.closeTab(tab) },
                    onPrint = { showPrintOptionsDialog = true },
                    isSplitViewEnabled = model.isSplitViewEnabled,
                    onToggleSplitView = { model.toggleSplitView() }
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (model.isSplitViewEnabled) {
                    // Split view mode
                    SplitViewComponent(
                        leftModel = model.getTabModel(model.splitViewLeftTab),
                        rightModel = model.getTabModel(model.splitViewRightTab),
                        availableTabs = model.tabs,
                        onLeftTabChange = { tab -> model.setSplitViewLeft(tab) },
                        onRightTabChange = { tab -> model.setSplitViewRight(tab) }
                    )
                } else {
                    // Normal single view mode
                    CurrentTab()
                }
            }
        }

        if (showPrintOptionsDialog) {
            AdvancedPrintOptionsDialog(
                onDismiss = { showPrintOptionsDialog = false },
                onConfirm = { printOptions ->
                    showPrintOptionsDialog = false
                    val currentTabModel = model.getCurrentTabModel()

                    if (currentTabModel != null) {
                        showProgressDialog = true
                        progressMessage = "Printing..."

                        scope.launch(Dispatchers.IO) {
                            try {
                                pdfGenerationService.generateAndPrint(
                                    sourcePath = currentTabModel.pdfFile.path,
                                    options = printOptions,
                                    copies = 1,
                                    duplex = false
                                )

                                withContext(Dispatchers.Main) {
                                    progressMessage = "Print completed successfully!"
                                    delay(2000)
                                    showProgressDialog = false
                                }

                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    progressMessage = "Error: ${e.message ?: "Unknown error occurred"}"
                                    delay(3000)
                                    showProgressDialog = false
                                }
                                println("ERROR: Could not execute the print command.")
                                e.printStackTrace()
                            }
                        }
                    } else {
                        println("Error: Could not get the current PDF to print.")
                    }
                }
            )
        }

        if (showProgressDialog) {
            PrintProgressDialog(
                message = progressMessage,
                onDismiss = {
                    // Only allow dismissal if not actively processing
                    if (progressMessage.contains("Error") ||
                        progressMessage.contains("success") ||
                        progressMessage.contains("cancelled") ||
                        progressMessage.contains("completed")) {
                        showProgressDialog = false
                    }
                }
            )
        }
    }
}