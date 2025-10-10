package com.jholachhapdevs.pdfjuggler.feature.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.PdfDisplayArea
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.TabScreenModel

@Composable
fun AiChatPdfComponent(
    tabScreenModel: TabScreenModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        // PDF viewer on the left (70% width)
        Box(
            modifier = Modifier.weight(0.7f)
        ) {
            PdfDisplayArea(model = tabScreenModel)
        }

        // AI chat on the right (30% width)
        Box(
            modifier = Modifier
                .weight(0.3f)
                .padding(start = 8.dp)
        ) {
            AiChatComponent()
        }
    }
}