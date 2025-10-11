package com.jholachhapdevs.pdfjuggler.feature.pdf.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jholachhapdevs.pdfjuggler.core.ui.components.JButton
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText

@Composable
fun PdfComponent(
    screenModel: PdfScreenModel
) {
    val cs = MaterialTheme.colorScheme
    val navigator = LocalNavigator.currentOrThrow

    // Navigate once and clear the picked file so back doesn't re-push
    LaunchedEffect(screenModel.pickedFile) {
        val file = screenModel.consumePickedFile() ?: return@LaunchedEffect
        navigator.push(PdfTabScreen(file))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .padding(24.dp)
            .padding(top = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = cs.background,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.40f)),
            modifier = Modifier
                .widthIn(min = 460.dp)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    JText(
                        text = "Load PDF",
                        style = MaterialTheme.typography.headlineMedium,
                        color = cs.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .height(2.dp)
                            .width(56.dp)
                            .background(cs.primary.copy(alpha = 0.6f))
                    )
                    Spacer(Modifier.height(10.dp))
                    JText(
                        text = "Pick a PDF to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onBackground.copy(alpha = 0.85f)
                    )
                }

                JButton(onClick = { screenModel.pickPdfFile() }) {
                    Text(
                        text = "Pick PDF",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}