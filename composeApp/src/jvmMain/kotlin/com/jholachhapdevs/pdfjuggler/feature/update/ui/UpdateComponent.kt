package com.jholachhapdevs.pdfjuggler.feature.update.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.jholachhapdevs.pdfjuggler.core.ui.components.JButton
import com.jholachhapdevs.pdfjuggler.core.ui.components.noRippleClickable
import com.mikepenz.markdown.m3.Markdown

// --------------------------------------------------------------------
// FLOATING CHIP THAT EXPANDS FROM AND COLLAPSES TO BOTTOM-RIGHT
// --------------------------------------------------------------------

@Composable
fun UpdateFloatingChip(
    modifier: Modifier = Modifier,
    currentVersionCode: Int,
    screenModel: UpdateScreenModel
) {
    // state isn't needed at the floating chip level; details are handled in the expanded banner
    val cs = MaterialTheme.colorScheme

    var expanded by remember { mutableStateOf(false) }

    val pointerIconModifier = if (!expanded) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant),
        tonalElevation = 0.dp,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(pointerIconModifier)
            .animateContentSize(
                animationSpec = spring(
                    stiffness = 500f,
                    dampingRatio = 0.8f
                )
            )
            .noRippleClickable {
                if (!expanded) expanded = true
            },
    ) {
        if (!expanded) {
            // Collapsed: icon only
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = "Show update",
                    tint = cs.primary
                )
            }
        } else {
            // Expanded: full banner
            UpdateHomeBanner(
                currentVersionCode = currentVersionCode,
                onClose = { expanded = false },
                screenModel = screenModel,
                modifier = Modifier.widthIn(min = 280.dp, max = 460.dp)
            )
        }
    }
}

// Invisible wrapper to reserve space for spring overshoot without shifting visual anchor
@Composable
fun OvershootSafeArea(
    overshoot: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.padding(overshoot)) {
        Box(Modifier.offset(x = -overshoot, y = -overshoot)) {
            content()
        }
    }
}

@Composable
fun UpdateHomeBanner(
    modifier: Modifier = Modifier,
    currentVersionCode: Int,
    onClose: () -> Unit,
    screenModel: UpdateScreenModel
) {
    val state = screenModel.uiState
    val info = state.updateInfo ?: return
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Update available", style = MaterialTheme.typography.titleMedium)

            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Dismiss", tint = cs.primary)
            }
        }

        Text("Latest: ${info.versionName}", style = MaterialTheme.typography.bodySmall)

        // Show any user-visible error (checksum mismatch or download error)
        state.error?.let { err ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = err,
                style = MaterialTheme.typography.bodySmall,
                color = cs.error
            )
        }

        if (state.isDownloading) {
            DownloadProgressSection(state)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. SHOW OPEN (IF DOWNLOADED)
            state.downloadedPath?.let { path ->
                JButton(onClick = {
                    try {
                        java.awt.Desktop.getDesktop().open(java.io.File(path))
                        // Close app immediately after opening installer to avoid lock issues
                        kotlin.system.exitProcess(0)
                    } catch (_: Throwable) {}
                }) {
                    Text("Open")
                }
                Spacer(Modifier.width(8.dp))
            }

            // 2. SHOW DOWNLOAD ONLY IF NOT DOWNLOADED AND URL IS AVAILABLE
            val downloadUrl = info.downloadUrl
            if (state.downloadedPath == null && downloadUrl != null) {
                JButton(onClick = {
                    if (!state.isDownloading)
                        screenModel.downloadUpdate(downloadUrl, expectedChecksum = info.checksum)
                }) {
                    Text(if (state.isDownloading) "Downloading" else "Download")
                }
                Spacer(Modifier.width(8.dp))
            }

            // 3. CHANGELOG LAST
            JButton(onClick = { screenModel.showChangelog(true) }) {
                Text("Changelog")
            }
        }
    }

    if (state.showChangelog) {
        ChangelogDialog(
            versionName = info.versionName,
            markdown = info.changelogMarkdown,
            onDismiss = { screenModel.showChangelog(false) }
        )
    }
}

// --------------------------------------------------------------------
// PROGRESS BAR
// --------------------------------------------------------------------

@Composable
private fun DownloadProgressSection(state: UpdateUiState) {
    val progress = state.downloadProgress
    var stable by remember { mutableStateOf<Float?>(null) }

    val indeterminate = progress == null || stable == progress

    if (indeterminate) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    } else {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
    }

    LaunchedEffect(progress) {
        if (progress != null) stable = progress
    }

    val d = state.downloadedBytes
    val t = state.totalBytes
    val started = state.downloadStartedAtMillis ?: System.currentTimeMillis()
    val elapsed = ((System.currentTimeMillis() - started) / 1000.0).coerceAtLeast(0.001)
    val speed = d / elapsed
    val eta = if (t != null && speed > 0) (t - d) / speed else null
    val percent = if (t != null && t > 0) (d.toDouble() / t.toDouble()) * 100 else null

    Text(
        buildString {
            append(formatBytes(d))
            if (t != null) append(" / ").append(formatBytes(t))
            append(" • ").append(formatSpeed(speed))
            percent?.let { append(" • ").append(String.format("%.0f%%", it)) }
            eta?.let { append(" • ").append(formatEta(it)) }
        },
        style = MaterialTheme.typography.bodySmall
    )
}

// --------------------------------------------------------------------
// CHANGELOG DIALOG
// --------------------------------------------------------------------

@Composable
private fun ChangelogDialog(
    versionName: String,
    markdown: String,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, cs.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = cs.surface),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Changelog $versionName", style = MaterialTheme.typography.titleMedium)

                Box(
                    modifier = Modifier
                        .heightIn(min = 120.dp, max = 460.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Markdown(markdown)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    JButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

// --------------------------------------------------------------------
// Helpers
// --------------------------------------------------------------------

private fun formatBytes(b: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        b >= gb -> "%.2f GB".format(b / gb)
        b >= mb -> "%.2f MB".format(b / mb)
        b >= kb -> "%.1f KB".format(b / kb)
        else -> "$b B"
    }
}

private fun formatSpeed(bytesPerSec: Double) =
    formatBytes(bytesPerSec.toLong()) + "/s"

private fun formatEta(seconds: Double): String {
    val s = seconds.toLong()
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return when {
        h > 0 -> "%dh %02dm %02ds left".format(h, m, sec)
        m > 0 -> "%dm %02ds left".format(m, sec)
        else -> "%ds left".format(sec)
    }
}
