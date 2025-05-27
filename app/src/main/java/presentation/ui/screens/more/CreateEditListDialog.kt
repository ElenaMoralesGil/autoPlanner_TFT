package com.elena.autoplanner.presentation.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.TaskList
import com.elena.autoplanner.presentation.ui.utils.GeneralAlertDialog

@Composable
fun CreateEditListDialog(
    existingList: TaskList? = null, 
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit,
) {
    var name by remember { mutableStateOf(existingList?.name ?: "") }
    val defaultColor = MaterialTheme.colorScheme.primary
    var selectedColor by remember {
        mutableStateOf(
            existingList?.colorHex?.let { hex ->
                try {
                    Color(android.graphics.Color.parseColor(hex))
                } catch (e: Exception) {
                    defaultColor
                }
            } ?: defaultColor
        )
    }
    var nameError by remember { mutableStateOf<String?>(null) }

    val predefinedColors = listOf(
        MaterialTheme.colorScheme.primary,
        Color(0xFFF44336),
        Color(0xFF4CAF50),
        Color(0xFF2196F3),
        Color(0xFFFFEB3B),
        Color(0xFF9C27B0),
        Color(0xFF000000),
        Color(0xFF9E9E9E)  
    )

    GeneralAlertDialog(
        title = { Text(if (existingList == null) "Create New List" else "Edit List") },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null 
                    },
                    label = { Text("List Name") },
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

                Text("Select Color:", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    predefinedColors.forEach { color ->
                        ColorCircle(
                            color = color,
                            isSelected = color == selectedColor,
                            onClick = { selectedColor = color },
                        )
                    }
                }
            }
        },
        onDismiss = onDismiss,
        onConfirm = {
            if (name.isBlank()) {
                nameError = "List name cannot be empty"
            } else {
                val colorHex = String.format("#%06X", 0xFFFFFF and selectedColor.toArgb())
                onConfirm(name, colorHex)
            }
        },

        )
}

@Composable
private fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.onPrimary, 
                modifier = Modifier.size(20.dp)
            )
        }
    }
}