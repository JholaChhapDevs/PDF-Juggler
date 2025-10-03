package com.jholachhapdevs.pdfjuggler.ui.pdf


import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp

/**
 * MiddlePanel displays the currently selected page (full-size image if available)
 * and simple Previous / Next navigation controls.
 *
 * - pageBitmap: optional ImageBitmap for the current page.
 * - currentPage & totalPages: used for button enable/disable and title.
 */
@Composable
fun MiddlePanel(
    pageBitmap: ImageBitmap?,
    currentPage: Int,
    totalPages: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Page display area (takes most vertical space)
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color.LightGray),
            tonalElevation = 2.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (pageBitmap != null) {
                    Image(
                        bitmap = pageBitmap,
                        contentDescription = "Page ${currentPage + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Placeholder when the bitmap is not yet available
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PDF not loaded", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (totalPages > 0) "Page ${currentPage + 1} of $totalPages" else "No pages", fontSize = 14.sp)
                    }
                }
            }
        }

        // Navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onPrev, enabled = totalPages > 0 && currentPage > 0) {
                Text("Previous")
            }

            Text(
                text = if (totalPages == 0) "— / —" else "${currentPage + 1} / $totalPages"
            )

            Button(onClick = onNext, enabled = totalPages > 0 && currentPage < totalPages - 1) {
                Text("Next")
            }
        }
    }
}
