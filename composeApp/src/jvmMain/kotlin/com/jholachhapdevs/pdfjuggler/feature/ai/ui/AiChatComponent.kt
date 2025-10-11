package com.jholachhapdevs.pdfjuggler.feature.ai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.model.ChatMessage
import com.mikepenz.markdown.m3.Markdown

@Composable
fun AiChatComponent(screenModel: AiScreenModel) {
    val ui = screenModel.uiState
    val cs = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current

    // Auto-scroll to newest message on change
    LaunchedEffect(ui.messages.size) {
        if (ui.messages.isNotEmpty()) {
            listState.animateScrollToItem(ui.messages.lastIndex)
        }
    }

    // Show jump-to-bottom when not at end
    val showJumpToBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            ui.messages.isNotEmpty() && lastVisible < ui.messages.lastIndex
        }
    }

    var jumpToBottom by remember { mutableStateOf(false) }
    if (jumpToBottom) {
        LaunchedEffect(Unit) {
            if (ui.messages.isNotEmpty()) listState.scrollToItem(ui.messages.lastIndex)
            jumpToBottom = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Outlined container card
        Surface(
            color = cs.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, cs.outlineVariant),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                // Compact header row (outlined style, no TopAppBar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = screenModel.assistantName,
                        style = MaterialTheme.typography.titleLarge,
                        color = cs.onSurface
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { screenModel.generateCheatSheet() },
                        enabled = !ui.isSending
                    ) {
                        Icon(Icons.Outlined.MenuBook, contentDescription = "Generate Cheat Sheet")
                    }
                    IconButton(
                        onClick = { screenModel.clearMessages() },
                        enabled = ui.messages.isNotEmpty() && !ui.isSending
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Clear chat")
                    }
                }

                // Error banner (outlined)
                ui.error?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = cs.surface,
                        tonalElevation = 0.dp,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, cs.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = "Error",
                                tint = cs.error
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurface
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { screenModel.dismissError() }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Dismiss")
                            }
                        }
                    }
                }

                // Messages
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ui.messages) { message ->
                        MessageBubble(
                            message = message,
                            onCopy = {
                                clipboard.setText(AnnotatedString(message.text))
                            }
                        )
                    }
                    // Typing indicator for AI
                    if (ui.isSending) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    color = cs.surface,
                                    tonalElevation = 0.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, cs.outlineVariant),
                                    modifier = Modifier.widthIn(max = 640.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = "Juggling...",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Input area (outlined)
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = cs.surface,
                    tonalElevation = 0.dp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ui.input,
                            onValueChange = { screenModel.updateInput(it) },
                            modifier = Modifier
                                .weight(1f)
                                .onPreviewKeyEvent { event ->
                                    if (
                                        event.key == Key.Enter &&
                                        event.type == KeyEventType.KeyDown &&
                                        !event.isShiftPressed &&
                                        !ui.isSending
                                    ) {
                                        screenModel.send()
                                        true
                                    } else {
                                        false
                                    }
                                },
                            placeholder = {
                                Text(
                                    text = "Ask anything about this PDF...",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            minLines = 1,
                            maxLines = 8
                        )
                        Spacer(Modifier.width(8.dp))
                        if (ui.isSending) {
                            IconButton(onClick = { screenModel.cancelSending() }) {
                                Icon(Icons.Outlined.Stop, contentDescription = "Cancel")
                            }
                        } else {
                            IconButton(
                                onClick = { screenModel.send() },
                                enabled = ui.input.isNotBlank()
                            ) {
                                Icon(Icons.Outlined.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            }
        }

        // Floating jump-to-bottom (outlined)
        if (showJumpToBottom) {
            Surface(
                shape = CircleShape,
                color = cs.surface,
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, cs.outlineVariant),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                IconButton(onClick = { jumpToBottom = true }) {
                    Icon(
                        Icons.Outlined.ArrowDownward,
                        contentDescription = "Jump to bottom"
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onCopy: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isUser = message.role == "user"

    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        val borderColor = if (isUser) cs.primary else cs.outlineVariant

        Surface(
            color = cs.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp
            ),
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .widthIn(max = 640.dp)
                .hoverable(interactionSource = interaction)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                SelectionContainer {
                    Markdown(
                        content = message.text,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}