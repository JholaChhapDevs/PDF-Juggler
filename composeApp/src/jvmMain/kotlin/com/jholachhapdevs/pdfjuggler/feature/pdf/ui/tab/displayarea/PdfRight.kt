package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.feature.ai.ui.AiComponent
import com.jholachhapdevs.pdfjuggler.feature.ai.ui.AiScreenModel

@Composable
fun PdfRight(
    screenModel: AiScreenModel,
    modifier : Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        modifier = modifier
    )  {
        AiComponent(screenModel)
    }
}