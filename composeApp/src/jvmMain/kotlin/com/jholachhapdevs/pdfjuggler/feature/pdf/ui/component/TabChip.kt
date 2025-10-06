package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText

@Composable
fun TabChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(999.dp) // pill
    val interaction = remember { MutableInteractionSource() }
    val hovered = interaction.collectIsHoveredAsState().value

    // Colors and elevation react to state
    val targetBg = when {
        selected -> cs.background
        hovered -> cs.surfaceVariant
        else -> cs.surface
    }
    val targetBorder = when {
        selected -> cs.primary
        hovered -> cs.outline.copy(alpha = 0.6f)
        else -> cs.outline.copy(alpha = 0.35f)
    }
    val targetText = if (selected) cs.primary else cs.onSurface

    val bg = animateColorAsState(targetBg, tween(150), label = "tab-bg").value
    val border = animateColorAsState(targetBorder, tween(150), label = "tab-border").value
    val elevation = animateDpAsState(if (hovered || selected) 4.dp else 0.dp, tween(150), label = "tab-elev").value

    Surface(
        color = bg,
        contentColor = targetText,
        tonalElevation = elevation,
        shape = shape,
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .height(30.dp)
            .padding(horizontal = 4.dp)
            .hoverable(interaction)
//            .indication(interaction, rememberRipple(bounded = true))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .defaultMinSize(minWidth = 120.dp)
                .padding(horizontal = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .fillMaxHeight()
                    .wrapContentHeight(Alignment.CenterVertically)
            ) {
                JText(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AnimatedVisibility(visible = hovered || selected) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close tab",
                        tint = targetText
                    )
                }
            }
        }
    }
}