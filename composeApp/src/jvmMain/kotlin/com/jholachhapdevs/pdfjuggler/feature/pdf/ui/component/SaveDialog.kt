package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jholachhapdevs.pdfjuggler.core.pdf.ValidationResult
import com.jholachhapdevs.pdfjuggler.core.ui.components.JButton
import com.jholachhapdevs.pdfjuggler.core.ui.components.JText
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveDialog(
    isOpen: Boolean,
    currentFileName: String,
    isOverwrite: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onValidatePath: (String) -> ValidationResult
) {
    var selectedPath by remember { mutableStateOf("") }
    var validationResult by remember { mutableStateOf<ValidationResult?>(null) }

    if (isOpen) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        JText(
                            text = if (isOverwrite) "Save PDF" else "Save PDF As",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Current file info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            JText(
                                text = "Current File:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            JText(
                                text = File(currentFileName).name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // File selection section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = selectedPath,
                            onValueChange = { path ->
                                selectedPath = path
                                validationResult = if (path.isNotBlank()) {
                                    onValidatePath(path)
                                } else {
                                    null
                                }
                            },
                            label = { JText("Output Path") },
                            placeholder = { JText("Select where to save the PDF") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            isError = validationResult is ValidationResult.Error
                        )
                        
                        JButton(
                            onClick = {
                                val fileChooser = JFileChooser().apply {
                                    dialogTitle = "Save PDF As"
                                    fileSelectionMode = JFileChooser.FILES_ONLY
                                    fileFilter = FileNameExtensionFilter("PDF files", "pdf")
                                    
                                    // Set default name based on current file
                                    val currentFile = File(currentFileName)
                                    val baseName = currentFile.nameWithoutExtension
                                    val suggestedName = if (isOverwrite) {
                                        "$baseName.pdf"
                                    } else {
                                        "${baseName}_reordered.pdf"
                                    }
                                    selectedFile = File(currentFile.parent, suggestedName)
                                }
                                
                                if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    val file = fileChooser.selectedFile
                                    val path = if (file.extension.lowercase() == "pdf") {
                                        file.absolutePath
                                    } else {
                                        "${file.absolutePath}.pdf"
                                    }
                                    selectedPath = path
                                    validationResult = onValidatePath(path)
                                }
                            },
                            modifier = Modifier.height(56.dp)
                        ) {
                            JText(
                                text = "Browse",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Validation feedback
                    validationResult?.let { result ->
                        when (result) {
                            is ValidationResult.Error -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Warning,
                                            contentDescription = "Error",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        JText(
                                            text = result.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            is ValidationResult.Warning -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Warning,
                                            contentDescription = "Warning",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        JText(
                                            text = result.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            ValidationResult.Valid -> {
                                // Show nothing for valid paths
                            }
                        }
                    }
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        JButton(onClick = onDismiss) {
                            JText(
                                text = "Cancel",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        JButton(
                            onClick = { 
                                if (selectedPath.isNotBlank()) {
                                    onSave(selectedPath)
                                }
                            },
                            enabled = selectedPath.isNotBlank() && validationResult !is ValidationResult.Error
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Save",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            JText(
                                text = "Save",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaveResultDialog(
    result: com.jholachhapdevs.pdfjuggler.core.pdf.SaveResult?,
    onDismiss: () -> Unit
) {
    result?.let { saveResult ->
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                when (saveResult) {
                    is com.jholachhapdevs.pdfjuggler.core.pdf.SaveResult.Success -> {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    is com.jholachhapdevs.pdfjuggler.core.pdf.SaveResult.Error -> {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            title = {
                JText(
                    text = when (saveResult) {
                        is com.jholachhapdevs.pdfjuggler.core.pdf.SaveResult.Success -> "PDF Saved Successfully"
                        is com.jholachhapdevs.pdfjuggler.core.pdf.SaveResult.Error -> "Save Error"
                    }
                )
            },
            text = {
                when (saveResult) {
                    is com.jholachhapdevs.pdfjuggler.core.pdf.SaveResult.Success -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            JText(
                                text = "PDF successfully saved with ${saveResult.pageCount} pages.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            JText(
                                text = "Location: ${File(saveResult.outputPath).name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    is com.jholachhapdevs.pdfjuggler.core.pdf.SaveResult.Error -> {
                        JText(
                            text = saveResult.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                JButton(onClick = onDismiss) {
                    JText(
                        text = "OK",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}
