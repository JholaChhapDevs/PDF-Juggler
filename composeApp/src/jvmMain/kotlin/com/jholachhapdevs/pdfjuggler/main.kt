package com.jholachhapdevs.pdfjuggler

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "PDF-Juggler",
    ) {
        App()
    }
}