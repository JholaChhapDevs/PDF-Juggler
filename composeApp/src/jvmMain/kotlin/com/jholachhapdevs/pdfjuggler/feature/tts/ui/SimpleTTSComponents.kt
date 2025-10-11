package com.jholachhapdevs.pdfjuggler.feature.tts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.feature.tts.domain.TTSState
import androidx.compose.animation.*
import androidx.compose.animation.core.*

/**
 * Floating close button that appears when TTS is active
 */
@Composable
fun TTSFloatingCloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulsing animation for visual feedback
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    FloatingActionButton(
        onClick = onClose,
        modifier = modifier
            .size(56.dp)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                alpha = alpha
            ),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = CircleShape
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Stop TTS",
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Simple linear progress bar for TTS playback based on current word index
 */
@Composable
fun TTSProgressBar(
    isActive: Boolean,
    currentWords: List<String>,
    currentWordIndex: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        val progress = remember(currentWords.size, currentWordIndex) {
            if (currentWords.isNotEmpty() && currentWordIndex >= 0) {
                ((currentWordIndex + 1).toFloat() / currentWords.size).coerceIn(0f, 1f)
            } else null
        }
        // Minimal paddings and full width bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Visual feedback overlay showing word-by-word TTS progress
 */
@Composable
fun TTSVisualFeedback(
    isActive: Boolean,
    currentWords: List<String>,
    currentWordIndex: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        if (currentWords.isNotEmpty()) {
            // Text display showing current words with highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        currentWords.forEachIndexed { index, word ->
                            if (index == currentWordIndex) {
                                withStyle(
                                    style = SpanStyle(
                                        background = MaterialTheme.colorScheme.primary,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    append(word)
                                }
                            } else {
                                append(word)
                            }
                            if (index < currentWords.size - 1) append(" ")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
