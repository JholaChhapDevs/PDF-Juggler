package com.jholachhapdevs.pdfjuggler.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    return this.then(
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.buttons.isPrimaryPressed
                    if (pressed) onClick()
                }
            }
        }
    )
}
