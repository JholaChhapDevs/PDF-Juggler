package com.jholachhapdevs.pdfjuggler

import App
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {

    // start with one empty Home tab (if you prefer)

    Window(
        onCloseRequest = ::exitApplication,
        title = "PDF-Juggler",
    ) {
        App()
    }
}