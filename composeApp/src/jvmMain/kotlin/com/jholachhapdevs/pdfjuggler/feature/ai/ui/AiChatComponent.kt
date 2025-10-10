package com.jholachhapdevs.pdfjuggler.feature.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText
import com.jholachhapdevs.pdfjuggler.feature.ai.domain.model.ChatMessage
import kotlinx.coroutines.launch

@Composable
fun AiChatComponent(
    modifier: Modifier = Modifier
) {
    val screenModel = remember { AiScreenModel() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages are added
    LaunchedEffect(screenModel.messages.size) {
        if (screenModel.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(screenModel.messages.size - 1)
            }
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                JText(
                    text = "AI Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = { screenModel.clearChat() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear chat"
                    )
                }
            }

            HorizontalDivider()

            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = screenModel.messages,
                    key = { it.id }
                ) { message ->
                    ChatMessageItem(message = message)
                }
            }

            // Input section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = screenModel.currentInput,
                        onValueChange = screenModel::updateInput,
                        modifier = Modifier.weight(1f),
                        placeholder = { JText("Ask me anything about your PDF...") },
                        enabled = !screenModel.isLoading,
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = screenModel::sendMessage,
                        enabled = screenModel.currentInput.isNotBlank() && !screenModel.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Box(
                modifier = Modifier.padding(12.dp)
            ) {
                if (message.isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        JText(
                            text = "Thinking...",
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    JText(
                        text = message.text,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}