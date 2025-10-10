package com.jholachhapdevs.pdfjuggler.feature.pdf.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedPrintOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (PrintOptions) -> Unit
) {
    var pagesPerSheet by remember { mutableStateOf(1) }
    var bookletFormat by remember { mutableStateOf(false) }
    var includeAnnotations by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Advanced Print Options") },
        text = {
            Column {
                // Pages per sheet dropdown
                Box {
                    TextField(
                        value = "$pagesPerSheet",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pages per Sheet") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                "Dropdown",
                                Modifier.clickable { expanded = true })
                        }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf(1, 2, 4, 8).forEach { numPages ->
                            DropdownMenuItem(
                                text = { Text("$numPages") },
                                onClick = {
                                    pagesPerSheet = numPages
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Booklet format checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = bookletFormat,
                        onCheckedChange = { bookletFormat = it }
                    )
                    Text("Booklet Format", modifier = Modifier.padding(start = 8.dp))
                }

                // Include annotations checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeAnnotations,
                        onCheckedChange = { includeAnnotations = it }
                    )
                    Text("Include Annotations", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        PrintOptions(
                            pagesPerSheet = pagesPerSheet,
                            bookletFormat = bookletFormat,
                            includeAnnotations = includeAnnotations
                        )
                    )
                }
            ) {
                Text("Print")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

data class PrintOptions(
    val pagesPerSheet: Int,
    val bookletFormat: Boolean,
    val includeAnnotations: Boolean
)
