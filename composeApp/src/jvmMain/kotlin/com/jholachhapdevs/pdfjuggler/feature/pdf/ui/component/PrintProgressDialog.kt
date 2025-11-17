package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jholachhapdevs.pdfjuggler.core.ui.components.JButton
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText

@Composable
fun PrintProgressDialog(
    message: String,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(350.dp)
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = cs.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Icon based on message
                when {
                    message.contains("Error", ignoreCase = true) -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            modifier = Modifier.size(56.dp),
                            tint = cs.error
                        )
                    }
                    message.contains("success", ignoreCase = true) ||
                            message.contains("completed", ignoreCase = true) -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            modifier = Modifier.size(56.dp),
                            tint = cs.primary
                        )
                    }
                    message.contains("cancelled", ignoreCase = true) -> {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Cancelled",
                            modifier = Modifier.size(56.dp),
                            tint = cs.onSurfaceVariant
                        )
                    }
                    else -> {
                        // Animated printing icon
                        val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )

                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(rotation),
                                color = cs.primary,
                                strokeWidth = 4.dp
                            )
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Printing",
                                modifier = Modifier.size(28.dp),
                                tint = cs.primary
                            )
                        }
                    }
                }

                // Message
                JText(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = cs.onSurface
                )

                // Close button (only show when done or error)
                if (message.contains("Error", ignoreCase = true) ||
                    message.contains("success", ignoreCase = true) ||
                    message.contains("completed", ignoreCase = true) ||
                    message.contains("cancelled", ignoreCase = true)) {

                    JButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Close",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}