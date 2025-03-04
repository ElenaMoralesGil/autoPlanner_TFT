package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.presentation.ui.utils.GeneralAlertDialog
import com.elena.autoplanner.presentation.ui.utils.SelectionGrid
import com.elena.autoplanner.presentation.ui.utils.StringPicker


@Composable
fun ReminderAlertDialog(
    existing: ReminderPlan?,
    onDismiss: () -> Unit,
    onReady: (ReminderPlan?) -> Unit
) {
    var showPersonalized by remember { mutableStateOf(false) }
    var localRem by remember {
        mutableStateOf(existing ?: ReminderPlan(mode = ReminderMode.NONE))
    }

    GeneralAlertDialog(
        title = { Text("Reminder") },
        content = {
            SelectionGrid(
                items = listOf(
                    "On time" to (localRem.mode == ReminderMode.PRESET_OFFSET && localRem.offsetMinutes == 0),
                    "5 min before" to (localRem.mode == ReminderMode.PRESET_OFFSET && localRem.offsetMinutes == 5),
                    "30 min before" to (localRem.mode == ReminderMode.PRESET_OFFSET && localRem.offsetMinutes == 30),
                    "1 day before" to (localRem.mode == ReminderMode.PRESET_OFFSET && localRem.offsetMinutes == 1440),
                    "1 week before" to (localRem.mode == ReminderMode.PRESET_OFFSET && localRem.offsetMinutes == 10080)
                ),
                onSelect = { index ->
                    val offsets = listOf(0, 5, 30, 1440, 10080)
                    localRem = ReminderPlan(mode = ReminderMode.PRESET_OFFSET, offsetMinutes = offsets[index])
                },
                onPersonalized = { showPersonalized = true },
                isPersonalizedSelected = localRem.mode == ReminderMode.CUSTOM
            )

        },
        onDismiss = onDismiss,
        onConfirm = {
            onReady(localRem.takeIf { it.mode != ReminderMode.NONE })
        },
        onNeutral = {
            localRem = ReminderPlan(mode = ReminderMode.NONE)
            onReady(null)
            onDismiss()
        }
    )

    if (showPersonalized) {
        ReminderPersonalizedAlertDialog(
            existing = localRem,
            onDismiss = { showPersonalized = false },
            onReady = { newVal ->
                localRem = newVal
                showPersonalized = false
            }
        )
    }
}


@Composable
fun ReminderPersonalizedAlertDialog(
    existing: ReminderPlan,
    onDismiss: () -> Unit,
    onReady: (ReminderPlan) -> Unit
) {

    var selectedTab by remember { mutableIntStateOf(0) }

    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var selectedWeekIndex by remember { mutableIntStateOf(0) }


    var selectedHour by remember { mutableIntStateOf(8) }
    var selectedMinute by remember { mutableIntStateOf(0) }

    val dayOffsets = listOf(
        "Same day",
        "1 day before",
        "2 days before",
        "3 days before",
        "4 days before",
        "5 days before",
        "6 days before",
        "7 days before"
    )
    val weekOffsets = listOf(
        "1 week before",
        "2 weeks before",
        "3 weeks before",
        "4 weeks before"
    )

    GeneralAlertDialog(
        title = {

        TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = (selectedTab == 0),
                    onClick = { selectedTab = 0 },
                    text = { Text("Days before") }
                )
                Tab(
                    selected = (selectedTab == 1),
                    onClick = { selectedTab = 1 },
                    text = { Text("Weeks before") }
                )
            }
        },
        content = {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selectedTab == 0) {

                        ScrollingStringPickerColumn(
                            items = dayOffsets,
                            selectedIndex = selectedDayIndex,
                            onIndexChange = { selectedDayIndex = it },
                            label = "Days"
                        )
                    } else {

                        ScrollingStringPickerColumn(
                            items = weekOffsets,
                            selectedIndex = selectedWeekIndex,
                            onIndexChange = { selectedWeekIndex = it },
                            label = "Weeks"
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {

                    TimePickerColumn(
                        range = 0..23,
                        selectedValue = selectedHour,
                        onValueChange = { selectedHour = it },
                        label = "Hour"
                    )

                    TimePickerColumn(
                        range = 0..59,
                        selectedValue = selectedMinute,
                        onValueChange = { selectedMinute = it },
                        label = "Min"
                    )
                }
                }
        },
        onDismiss = onDismiss,
        onConfirm = {
            val finalPlan = existing.copy(
                mode = ReminderMode.CUSTOM,
                customDayOffset = if (selectedTab == 0) selectedDayIndex else null,
                customWeekOffset = if (selectedTab == 1) selectedWeekIndex + 1 else null,
                customHour = selectedHour,
                customMinute = selectedMinute
            )
            onReady(finalPlan)
        },
        onNeutral = {
            onDismiss()
        }
    )
}


@Composable
fun ScrollingStringPickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        Spacer(Modifier.height(8.dp))
        StringPicker(
            items = items,
            selectedIndex = selectedIndex,
            onValueChange = onIndexChange,
            modifier = Modifier.width(110.dp)
        )
    }
}

