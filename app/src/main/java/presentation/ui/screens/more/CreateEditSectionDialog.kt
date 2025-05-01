package com.elena.autoplanner.presentation.ui.screens.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.presentation.ui.utils.GeneralAlertDialog

@Composable
fun CreateEditSectionDialog(
    listName: String, // To show context
    existingSection: TaskSection? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit,
) {
    var name by remember { mutableStateOf(existingSection?.name ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }

    GeneralAlertDialog(
        title = { Text(if (existingSection == null) "Create Section in '$listName'" else "Edit Section") },
        content = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text("Section Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError != null
                )
                if (nameError != null) {
                    Text(
                        nameError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        onDismiss = onDismiss,
        onConfirm = {
            if (name.isBlank()) {
                nameError = "Section name cannot be empty"
            } else {
                onConfirm(name)
            }
        }
    )
}