package com.elena.autoplanner.presentation.ui.screens.tasks.modificationTaskSheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.R

@Composable
fun ConfigurationOptionsRow(
    onTimeClick: () -> Unit,
    onPriorityClick: () -> Unit,
    onListsClick: () -> Unit,
    onSubtasksClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton(
            icon = painterResource(R.drawable.ic_calendar),
            label = "Time",
            onClick = onTimeClick
        )

        ActionButton(
            icon = painterResource(R.drawable.priority),
            label = "Priority",
            onClick = onPriorityClick
        )

        ActionButton(
            icon = painterResource(R.drawable.ic_lists),
            label = "Lists",
            onClick = onListsClick
        )

        ActionButton(
            icon = painterResource(R.drawable.ic_subtasks),
            label = "Subtasks",
            onClick = onSubtasksClick
        )
    }
}

@Composable
private fun ActionButton(
    icon: Painter,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            painter = icon,
            contentDescription = label,
            modifier = Modifier
                .size(24.dp)
                .padding(4.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}