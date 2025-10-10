package com.jholachhapdevs.pdfjuggler.feature.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.PdfDisplayArea
import com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.TabScreenModel

@Composable
fun AiChatPdfComponent(
    tabScreenModel: TabScreenModel,
    aiScreenModel: AiScreenModel,
    modifier: Modifier = Modifier
) {
    // If there's a pending AI request (dictionary/translate), seed and send it
    val req = tabScreenModel.pendingAiRequest
    LaunchedEffect(req) {
        req?.let { r ->
            val text = r.text.trim()
            if (text.isNotEmpty()) {
                val base = "You are a helpful assistant for reading PDFs. First, provide a brief dictionary-style meaning/definition and part of speech for: \"$text\". Then ask the user: 'Into which language should I translate this?'. Do not translate until the user specifies the language. Keep it concise."
                aiScreenModel.updateInput(base)
                aiScreenModel.send()
            }
            tabScreenModel.clearPendingAiRequest()
        }
    }

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
            AiChatComponent(screenModel = aiScreenModel)
        }
    }
}