package com.jholachhapdevs.pdfjuggler

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.jholachhapdevs.pdfjuggler.core.util.LocalComposeWindow

fun main() = application {

    // start with one empty Home tab (if you prefer)
    val windowState = WindowState(width = 1200.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "PDF-Juggler",
        state = windowState
    ) {
        val window = this.window
        CompositionLocalProvider(LocalComposeWindow provides window) {
            App()
        }
    }
}