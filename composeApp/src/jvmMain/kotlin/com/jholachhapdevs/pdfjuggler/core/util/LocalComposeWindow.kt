package com.jholachhapdevs.pdfjuggler.core.util

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.awt.ComposeWindow

val LocalComposeWindow = staticCompositionLocalOf<ComposeWindow> {
    error("No ComposeWindow provided")
}
