// Kotlin
package com.jholachhapdevs.pdfjuggler.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.core.ui.extendedColors

@Composable
fun JButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = JugglerButtonDefaults.shape,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    glowColor: Color = MaterialTheme.extendedColors.focusGlow,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current

    val containerColor by animateColorAsState(
        targetValue = if (isHovered && enabled) cs.primary else Color.Transparent,
        label = "containerColorAnim"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isHovered && enabled) cs.background else cs.primary,
        label = "contentColorAnim"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isHovered && enabled) Color.Transparent else cs.primary,
        label = "borderColorAnim"
    )

    val glowRadiusDp by animateDpAsState(
        targetValue = if (isHovered && enabled) 16.dp else 0.dp,
        label = "glowRadiusAnim"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isHovered && enabled) 1f else 0f,
        label = "glowAlphaAnim"
    )

    val glowModifier = if (glowAlpha > 0f && glowRadiusDp > 0.dp) {
        val radiusPx = with(density) { glowRadiusDp.toPx() }
        Modifier.dropShadow(shape = shape) {
            this.radius = radiusPx
            this.spread = 0f
            this.color = glowColor.copy(alpha = 0.7f * glowAlpha)
        }.pointerHoverIcon(PointerIcon.Hand)
    } else {
        Modifier
    }

    Box(modifier.then(glowModifier)) {
        Button(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = cs.surfaceVariant.copy(alpha = 0.12f),
                disabledContentColor = cs.onSurface.copy(alpha = 0.38f)
            ),
            border = BorderStroke(1.dp, borderColor),
            contentPadding = contentPadding,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
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

    val colors: ButtonColors
        @Composable get() = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        )
}
