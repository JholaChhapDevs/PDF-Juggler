package com.jholachhapdevs.pdfjuggler.feature.pdf.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.jholachhapdevs.pdfjuggler.core.datastore.RecentPdfsStore
import com.jholachhapdevs.pdfjuggler.core.ui.components.JButton
import com.jholachhapdevs.pdfjuggler.core.util.Env
import com.jholachhapdevs.pdfjuggler.feature.update.ui.UpdateFloatingChip
import com.jholachhapdevs.pdfjuggler.feature.update.ui.UpdateScreenModel
import com.jholachhapdevs.pdfjuggler.feature.update.data.repository.UpdateRepository
import com.jholachhapdevs.pdfjuggler.feature.update.domain.usecase.GetUpdatesUseCase
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import java.io.File

@Composable
fun PdfComponent(screenModel: PdfScreenModel) {
    val cs = MaterialTheme.colorScheme
    val navigator = LocalNavigator.currentOrThrow

    var recentPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        recentPaths = RecentPdfsStore.getRecentPaths()
    }
    LaunchedEffect(screenModel.pickedFile) {
        val file = screenModel.consumePickedFile() ?: return@LaunchedEffect
        navigator.push(PdfTabScreen(file))
        recentPaths = RecentPdfsStore.getRecentPaths()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .padding(16.dp)
    ) {
        // Branding (top-left)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                "PDF Juggler",
                style = MaterialTheme.typography.displaySmall,
                color = cs.primary
            )
            Text(
                "© ${java.time.Year.now().value} JholaChhapDevs",
                style = MaterialTheme.typography.bodySmall,
                color = cs.primary.copy(alpha = 0.75f)
            )
        }

        // API key (top-right)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            val hasKey = screenModel.apiKey.isNotBlank()
            JButton(onClick = { screenModel.openApiKeyDialog() }) {
                Text(if (hasKey) "Change API Key" else "Add API Key")
            }
            if (!hasKey) {
                Spacer(Modifier.height(4.dp))
                Text("AI features disabled (no key)", style = MaterialTheme.typography.bodySmall, color = cs.error)
            }
        }

        // Center card
        Surface(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.4f)),
            color = cs.background,
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(min = 360.dp, max = 600.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Load PDF", style = MaterialTheme.typography.headlineMedium, color = cs.primary)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.height(2.dp).width(56.dp).background(cs.primary.copy(alpha = 0.6f)))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pick a PDF to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onBackground.copy(alpha = 0.85f)
                    )
                }
                JButton(onClick = { screenModel.pickPdfFile() }) { Text("Pick PDF") }
                if (recentPaths.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Recent files", style = MaterialTheme.typography.titleSmall, color = cs.onBackground)
                        recentPaths.forEach { path ->
                            val name = File(path).name
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        path,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onBackground.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                JButton(onClick = {
                                    navigator.push(
                                        PdfTabScreen(
                                            com.jholachhapdevs.pdfjuggler.feature.pdf.domain.model.PdfFile(path, name)
                                        )
                                    )
                                }) { Text("Open") }
                            }
                        }
                    }
                }
            }
        }

        if (screenModel.showApiKeyDialog) {
            ApiKeyDialog(
                apiKey = screenModel.apiKey,
                onSave = { screenModel.saveApiKey(it); screenModel.closeApiKeyDialog() },
                onClear = { screenModel.clearApiKey(); screenModel.closeApiKeyDialog() },
                onDismiss = { screenModel.closeApiKeyDialog() }
            )
        }

        // Footer
        Text(
            "© ${java.time.Year.now().value} JholaChhapDevs",
            style = MaterialTheme.typography.bodySmall,
            color = cs.primary.copy(alpha = 0.75f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
        )

        // Update floating chip (single morphing Surface)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            // Update visibility and model
            val updateScreenModel = remember { UpdateScreenModel(GetUpdatesUseCase(UpdateRepository())) }
            val updateState = updateScreenModel.uiState
            val info = updateState.updateInfo
            val showUpdate = !updateState.loading && updateState.error == null && info != null && info.versionCode > Env.APP_VERSION_CODE && !info.downloadUrl.isNullOrBlank()

            AnimatedVisibility(visible = showUpdate, enter = fadeIn(), exit = fadeOut()) {
                UpdateFloatingChip(
                    currentVersionCode = Env.APP_VERSION_CODE,
                    screenModel = updateScreenModel
                )
            }
        }
    }
}
