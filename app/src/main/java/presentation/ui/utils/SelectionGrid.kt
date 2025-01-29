package com.elena.autoplanner.presentation.ui.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelectionGrid(
    items: List<Pair<String, Boolean>>,
    onSelect: (Int) -> Unit,
    onPersonalized: (() -> Unit)? = null,
    isPersonalizedSelected: Boolean = false
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEachIndexed { index, (label, isSelected) ->
            Chip(
                label = label,
                selected = isSelected,
                onClick = { onSelect(index) }
            )
        }

        onPersonalized?.let {
            Chip(
                label = "Custom...",
                selected = isPersonalizedSelected,
                modifier = Modifier.fillMaxWidth(),
                onClick = it,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                }
            )
        }
    }
}