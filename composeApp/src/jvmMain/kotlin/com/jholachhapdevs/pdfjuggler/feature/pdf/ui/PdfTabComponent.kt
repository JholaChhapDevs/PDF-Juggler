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
import com.jholachhapdevs.pdfjuggler.feature.ai.data.remote.GeminiRemoteDataSource
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.usecase.SendPromptUseCase
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.usecase.UploadFileUseCase
import com.jholachhapdevs.pdfjuggler.feature.ai.ui.AiScreenModel
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
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.PdfDisplayArea
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.PdfTab
import com.jholachhapdevs.pdfjuggler.service.PdfGenerationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.jholachhapdevs.pdfjuggler.feature.tts.rememberTTSViewModel

@Composable
fun PdfTabComponent(
    model: PdfTabScreenModel
) {
    val stack = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()

    var showPrintOptionsDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("Printing...") }
    var isSearchVisible by remember { mutableStateOf(false) }

    val pdfGenerationService = remember { PdfGenerationService() }

    // Create a single TTS ViewModel scoped to this component
    val ttsViewModel = rememberTTSViewModel()

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

        // Observe pending AI request on the current tab and auto-enable AI chat panel
        val currentTabModel = model.getCurrentTabModel()
        val pendingAi = currentTabModel?.pendingAiRequest
        LaunchedEffect(pendingAi) {
            if (pendingAi != null) {
                println("DEBUG: Pending AI request detected: ${pendingAi.mode} for text: '${pendingAi.text.take(50)}...'")
                model.setAiChatVisible(true)
                println("DEBUG: AI chat enabled: ${model.isAiChatEnabled}")
            }
        }

        Scaffold(
            topBar = {
                // Get fresh tab model on every recomposition
                val currentTabModel = (model.current as? PdfTab)?.let { model.getTabModel(it) }

                TabBar(
                    tabs = model.tabs,
                    onAdd = { model.addTabFromPicker() },
                    onSelect = { tab ->
                        
                        model.selectTab(tab)
                    },
                    onClose = { tab -> model.closeTab(tab) },
                    onPrint = { showPrintOptionsDialog = true },
                    isSplitViewEnabled = model.isSplitViewEnabled,
                    onToggleSplitView = {
                        println("DEBUG: Toggle split view clicked")
                        model.toggleSplitView()
                    },
                    isAiChatEnabled = model.isAiChatEnabled,
                    onToggleAiChat = {
                        println("DEBUG: Toggle AI chat clicked")
                        model.toggleAiChat()
                    },
                    // PDF Viewer controls
                    zoomFactor = currentTabModel?.currentZoom ?: 1f,
                    minZoom = 0.25f,
                    isFullscreen = currentTabModel?.isFullscreen ?: false,
                    onZoomIn = {
                        val tabModel = (model.current as? PdfTab)?.let { model.getTabModel(it) }
                        println("DEBUG: Zoom in clicked - tabModel exists: ${tabModel != null}")
                        tabModel?.let {
                            println("DEBUG: Calling zoomIn() - current zoom: ${it.currentZoom}")
                            it.zoomIn()
                        } ?: println("ERROR: tabModel is null for zoomIn!")
                    },
                    onZoomOut = {
                        val tabModel = (model.current as? PdfTab)?.let { model.getTabModel(it) }
                        println("DEBUG: Zoom out clicked - tabModel exists: ${tabModel != null}")
                        tabModel?.let {
                            println("DEBUG: Calling zoomOut() - current zoom: ${it.currentZoom}")
                            it.zoomOut()
                        } ?: println("ERROR: tabModel is null for zoomOut!")
                    },
                    onResetZoom = {
                        val tabModel = (model.current as? PdfTab)?.let { model.getTabModel(it) }
                        println("DEBUG: Reset zoom clicked - tabModel exists: ${tabModel != null}")
                        tabModel?.let {
                            println("DEBUG: Calling resetZoom() - current zoom: ${it.currentZoom}")
                            it.resetZoom()
                        } ?: println("ERROR: tabModel is null for resetZoom!")
                    },
                    onRotateClockwise = {
                        val tabModel = (model.current as? PdfTab)?.let { model.getTabModel(it) }
                        println("DEBUG: Rotate clockwise clicked - tabModel exists: ${tabModel != null}")
                        tabModel?.let {
                            println("DEBUG: Calling rotateClockwise() - current rotation: ${it.currentRotation}")
                            it.rotateClockwise()
                        } ?: println("ERROR: tabModel is null for rotateClockwise!")
                    },
                    onRotateCounterClockwise = {
                        val tabModel = (model.current as? PdfTab)?.let { model.getTabModel(it) }
                        println("DEBUG: Rotate counter-clockwise clicked - tabModel exists: ${tabModel != null}")
                        tabModel?.let {
                            println("DEBUG: Calling rotateCounterClockwise() - current rotation: ${it.currentRotation}")
                            it.rotateCounterClockwise()
                        } ?: println("ERROR: tabModel is null for rotateCounterClockwise!")
                    },
                    onToggleFullscreen = {
                        val tabModel = (model.current as? PdfTab)?.let { model.getTabModel(it) }
                        println("DEBUG: Toggle fullscreen clicked - tabModel exists: ${tabModel != null}")
                        tabModel?.let {
                            println("DEBUG: Calling toggleFullscreen() - is fullscreen: ${it.isFullscreen}")
                            it.toggleFullscreen()
                        } ?: println("ERROR: tabModel is null for toggleFullscreen!")
                    },
                    onSearchClick = {
                        println("DEBUG: Search clicked - toggling visibility")
                        isSearchVisible = !isSearchVisible
                    }
                )
            }
        ) { padding ->
            // Get fresh tab model for content area
            val currentTabModel = (model.current as? PdfTab)?.let { model.getTabModel(it) }

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (model.isSplitViewEnabled) {
                    // Split view mode
                    SplitViewComponent(
                        leftModel = model.getTabModel(model.splitViewLeftTab),
                        rightModel = model.getTabModel(model.splitViewRightTab),
                        availableTabs = model.tabs,
                        onLeftTabChange = { tab -> model.setSplitViewLeft(tab) },
                        onRightTabChange = { tab -> model.setSplitViewRight(tab) },
                        ttsViewModel = ttsViewModel
                    )
                } else if (model.isAiChatEnabled) {
                    // AI chat mode - show PDF with AI chat panel
                    if (currentTabModel != null) {
                        // Create AiScreenModel for the current tab
                        val remote = remember { GeminiRemoteDataSource() }
                        val aiScreenModel = remember(currentTabModel.pdfFile.path) {
                            AiScreenModel(
                                pdfFile = currentTabModel.pdfFile,
                                sendPromptUseCase = SendPromptUseCase(remote),
                                uploadFileUseCase = UploadFileUseCase(remote),
                                initialSelectedPageIndex = currentTabModel.selectedPageIndex
                            )
                        }

                        PdfDisplayArea(
                            model = currentTabModel,
                            aiScreenModel = aiScreenModel,
                            ttsViewModel = ttsViewModel,
                            isSearchVisible = isSearchVisible,
                            onSearchVisibilityChange = { isSearchVisible = it }
                        )
                    } else {
                        CurrentTab()
                    }
                } else {
                    // Normal single view mode
                    if (currentTabModel != null) {
                        PdfDisplayArea(
                            model = currentTabModel,
                            aiScreenModel = null,
                            ttsViewModel = ttsViewModel,
                            isSearchVisible = isSearchVisible,
                            onSearchVisibilityChange = { isSearchVisible = it }
                        )
                    } else {
                        CurrentTab()
                    }
                }
            }
        }

        if (showPrintOptionsDialog) {
            AdvancedPrintOptionsDialog(
                onDismiss = { showPrintOptionsDialog = false },
                onConfirm = { printOptions ->
                    showPrintOptionsDialog = false
                    val currentTabModel = (model.current as? PdfTab)?.let { model.getTabModel(it) }

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
                                e.printStackTrace()
                            }
                        }
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
                        progressMessage.contains("completed")
                    ) {
                        showProgressDialog = false
                    }
                }
            )
        }
    }
}