package com.elena.autoplanner.presentation.ui.screens.tasks.TasksScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeleteAction(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            Icons.Default.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .size(40.dp)
                .padding(8.dp)
        )
    }
}

@Composable
fun EditAction(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.CenterStart
    ) {
        Icon(
            Icons.Default.Edit,
            contentDescription = "Edit",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .size(40.dp)
                .padding(8.dp)
        )
    }
}