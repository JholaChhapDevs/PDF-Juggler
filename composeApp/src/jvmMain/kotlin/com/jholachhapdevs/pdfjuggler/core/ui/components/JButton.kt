package com.jholachhapdevs.pdfjuggler.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.core.ui.extendedColors

@Composable
fun JButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = JugglerButtonDefaults.shape,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = JugglerButtonDefaults.contentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isHovered by internalInteractionSource.collectIsHoveredAsState()

    val buttonGlowColor = MaterialTheme.colorScheme.primary

    // Animations
    val floatAnimSpec = tween<Float>(durationMillis = 250)
    val colorAnimSpec = tween<Color>(durationMillis = 250)

    val glowAlpha by animateFloatAsState(
        targetValue = if (isHovered && enabled) 0.7f else 0f,
        animationSpec = floatAnimSpec,
        label = "glowAlphaAnim"
    )

    val scale by animateFloatAsState(
        targetValue = if (isHovered && enabled) 1.05f else 1f,
        animationSpec = floatAnimSpec,
        label = "scaleAnim"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.extendedColors.disabledBackground
            isHovered -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = colorAnimSpec, label = "backgroundColorAnim"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.extendedColors.disabledContent
            isHovered -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = colorAnimSpec, label = "contentColorAnim"
    )

    val borderColor by animateColorAsState(
        targetValue = if (!enabled) MaterialTheme.extendedColors.disabledContent.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.primary,
        animationSpec = colorAnimSpec,
        label = "borderAnim"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        border = border ?: BorderStroke(1.dp, borderColor),
        modifier = modifier
            .hoverable(internalInteractionSource)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                if (glowAlpha > 0f) {
                    val glowColor = buttonGlowColor.copy(alpha = glowAlpha)
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = center,
                            radius = size.minDimension * 0.8f
                        ),
                        cornerRadius = CornerRadius(20f, 20f)
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

object JugglerButtonDefaults {
    val shape: Shape @Composable get() = RoundedCornerShape(8.dp)
    val contentPadding: PaddingValues = PaddingValues(
        horizontal = 24.dp,
        vertical = 12.dp
    )
}