package com.elena.autoplanner.presentation.ui.screens.tasks


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.DayOfWeek
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.IntervalUnit
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.presentation.ui.utils.GeneralAlertDialog
import com.elena.autoplanner.presentation.ui.utils.NumberPicker
import com.elena.autoplanner.presentation.ui.utils.AnimatedSection
import com.elena.autoplanner.presentation.ui.utils.SelectionGrid


@Composable
fun RepeatAlertDialog(
    existing: RepeatPlan?,
    onDismiss: () -> Unit,
    onReady: (RepeatPlan?) -> Unit
) {
    var showPersonalized by remember { mutableStateOf(false) }
    var localRepeat by remember {
        mutableStateOf(existing ?: RepeatPlan(frequencyType = FrequencyType.NONE))
    }

    GeneralAlertDialog(
        title = { Text("Repeat") },
        content = {
            SelectionGrid(
                items = listOf(
                    "Daily" to (localRepeat.frequencyType == FrequencyType.DAILY),
                    "Weekly" to (localRepeat.frequencyType == FrequencyType.WEEKLY),
                    "Monthly" to (localRepeat.frequencyType == FrequencyType.MONTHLY),
                    "Yearly" to (localRepeat.frequencyType == FrequencyType.YEARLY)
                ),
                onSelect = { index ->
                    val types = listOf(FrequencyType.DAILY, FrequencyType.WEEKLY, FrequencyType.MONTHLY, FrequencyType.YEARLY)
                    localRepeat = RepeatPlan(frequencyType = types[index])
                },
                onPersonalized = { showPersonalized = true },
                isPersonalizedSelected = localRepeat.frequencyType == FrequencyType.CUSTOM
            )

            AnimatedSection(visible = localRepeat.frequencyType == FrequencyType.WEEKLY) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
                    Text("On which days?", style = MaterialTheme.typography.bodyLarge)
                    DaysOfWeekSelector(
                        selectedDays = localRepeat.selectedDays.map { it.ordinal }.toSet(),
                        onDaySelected = { index ->
                            val day = DayOfWeek.entries[index]
                            localRepeat = localRepeat.copy(
                                selectedDays = if (localRepeat.selectedDays.contains(day)) {
                                    localRepeat.selectedDays - day
                                } else {
                                    localRepeat.selectedDays + day
                                }
                            )
                        }
                    )
                }
            }
        },
        onDismiss = onDismiss,
        onConfirm = {
            onReady(localRepeat.takeIf { it.frequencyType != FrequencyType.NONE })
        },
        onNeutral = {
            localRepeat = RepeatPlan(frequencyType = FrequencyType.NONE)
            onReady(null)
            onDismiss()
        }
    )

    if (showPersonalized) {
        RepeatPersonalizedAlertDialog(
            existing = localRepeat,
            onDismiss = { showPersonalized = false },
            onReady = { newVal ->
                localRepeat = newVal
                showPersonalized = false
            }
        )
    }
}


@Composable
private fun DaysOfWeekSelector(
    selectedDays: Set<Int>,
    onDaySelected: (Int) -> Unit
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        days.forEachIndexed { index, day ->
            DayChip(
                label = day,
                selected = selectedDays.contains(index),
                onClick = { onDaySelected(index) }
            )
        }
    }
}
@Composable
private fun RepeatPersonalizedAlertDialog(
    existing: RepeatPlan,
    onDismiss: () -> Unit,
    onReady: (RepeatPlan) -> Unit
) {
    var interval by remember { mutableIntStateOf(existing.interval ?: 1) }
    var intervalUnit by remember { mutableStateOf(existing.intervalUnit ?: IntervalUnit.WEEK) }
    val selectedDays by remember { mutableStateOf(existing.selectedDays) }

    GeneralAlertDialog(
        title = { Text("Custom Repeat") },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                IntervalSelector(
                    interval = interval,
                    unit = intervalUnit,
                    onIntervalChange = { interval = it },
                    onUnitChange = { intervalUnit = it }
                )
            }
        },
        onDismiss = onDismiss,
        onConfirm = {
            onReady(
                existing.copy(
                    frequencyType = FrequencyType.CUSTOM,
                    interval = if (selectedDays.isEmpty()) interval else null,
                    intervalUnit = if (selectedDays.isEmpty()) intervalUnit else null,
                    selectedDays = selectedDays
                )
            )
        }
    )
}

@Composable
private fun IntervalSelector(
    interval: Int,
    unit: IntervalUnit,
    onIntervalChange: (Int) -> Unit,
    onUnitChange: (IntervalUnit) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Every", style = MaterialTheme.typography.bodyMedium)

        NumberPicker(
            value = interval,
            range = 1..30,
            onValueChange = onIntervalChange,
            modifier = Modifier.weight(1f)
        )

        SegmentedControlColumn(
            options = IntervalUnit.entries.map {
                it.name.lowercase().replaceFirstChar { ch -> ch.titlecase() }
            },
            selectedIndex = unit.ordinal,
            onSelectionChanged = { idx ->
                onUnitChange(IntervalUnit.entries[idx])
            },
            modifier = Modifier.weight(1.5f)
        )
    }
}

@Composable
private fun FrequencyChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.secondary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSecondary),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onSecondary
                else MaterialTheme.colorScheme.onSurface
            )
            trailingIcon?.invoke()
        }
    }
}

@Composable
private fun DayChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
fun SegmentedControlColumn(
    options: List<String>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex


                val shapeItem = when (index) {
                    0 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                    options.lastIndex -> RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                    else -> RoundedCornerShape(0.dp)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = shapeItem
                        )
                        .clickable { onSelectionChanged(index) }
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}





