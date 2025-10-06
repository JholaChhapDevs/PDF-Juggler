package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.tab.displayarea

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText

@Composable
fun PdfMid(
    modifier: Modifier = Modifier,
    pageImage: ImageBitmap? = null
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        color = cs.background,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (pageImage != null) {
                Surface(
                    color = cs.surface,
                    tonalElevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxSize(0.95f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = pageImage,
                            contentDescription = "Current Page",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            } else {
                JText(
                    text = "No page to display",
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}