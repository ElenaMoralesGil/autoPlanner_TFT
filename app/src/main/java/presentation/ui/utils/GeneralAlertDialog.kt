package com.elena.autoplanner.presentation.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun GeneralAlertDialog(
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onNeutral: (() -> Unit)? = null,

) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                title()
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp)
            ) {
                content()
            }
        },
        confirmButton = {

            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Ready", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {

            if (onNeutral != null) {
                TextButton(
                    onClick = onNeutral
                ) {
                    Text("None")
                }
            } else {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }
            }
        },
        shape = RoundedCornerShape(5.dp)
    )
}
