package com.jholachhapdevs.pdfjuggler.legacy

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.core.ui.components.JugglerButtonDefaults

@Composable
fun JButtonOld(
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
    val isPressed by internalInteractionSource.collectIsPressedAsState()

    // Scale animation
    val targetScale = when {
        isPressed -> 0.95f
        isHovered -> 1.05f
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 150),
        label = "buttonScale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    // Swap container and content colors on hover using primary color
    val containerColor by animateColorAsState(
        targetValue = if (isHovered && enabled) primaryColor
        else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "containerColorAnim"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isHovered && enabled) Color.White
        else primaryColor,
        animationSpec = tween(durationMillis = 150),
        label = "contentColorAnim"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = shape,
        border = border ?: BorderStroke(1.dp, primaryColor),
        contentPadding = contentPadding,
        interactionSource = internalInteractionSource,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        content = content
    )
}
