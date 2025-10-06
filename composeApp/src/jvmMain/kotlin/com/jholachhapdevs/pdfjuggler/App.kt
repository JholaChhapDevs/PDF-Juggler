package com.jholachhapdevs.pdfjuggler

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.jholachhapdevs.pdfjuggler.core.ui.PdfJugglerTheme
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.PdfScreen
import com.jholachhapdevs.pdfjuggler.feature.update.ui.UpdateScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    PdfJugglerTheme {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
                .safeContentPadding(),
//            contentAlignment = Alignment.Center
        ) {
            Navigator(PdfScreen) {
                SlideTransition(it)
            }
        }
    }
}