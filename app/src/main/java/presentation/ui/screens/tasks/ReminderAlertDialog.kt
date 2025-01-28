package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.presentation.ui.utils.GeneralAlertDialog
import com.elena.autoplanner.presentation.ui.utils.SelectionGrid
import kotlin.math.roundToInt

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
                    "5 min early" to (localRem.mode == ReminderMode.PRESET_OFFSET && localRem.offsetMinutes == 5),
                    "30 min early" to (localRem.mode == ReminderMode.PRESET_OFFSET && localRem.offsetMinutes == 30),
                    "1 day early" to (localRem.mode == ReminderMode.PRESET_OFFSET && localRem.offsetMinutes == 1440),
                    "1 week early" to (localRem.mode == ReminderMode.PRESET_OFFSET && localRem.offsetMinutes == 10080)
                ),
                onSelect = { index ->
                    val offsets = listOf(0, 5, 30, 1440, 10080)
                    localRem = ReminderPlan(mode = ReminderMode.PRESET_OFFSET, offsetMinutes = offsets[index])
                },
                onPersonalized = { showPersonalized = true }
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
    // Pestañas: 0 => “Days before”, 1 => “Weeks before”
    var selectedTab by remember { mutableStateOf(0) }

    // Estados para offset en días/semanas
    var selectedDayIndex by remember { mutableStateOf(0) }
    var selectedWeekIndex by remember { mutableStateOf(0) }

    // Estados para la hora/minuto
    var selectedHour by remember { mutableStateOf(8) }
    var selectedMinute by remember { mutableStateOf(0) }

    // Listas de días/semanas
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
            // Barrita de pestañas
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
            // Tres columnas: offset (días o semanas), hour, minute
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // COLUMNA 1: Offset en días o semanas
                if (selectedTab == 0) {
                    // Muestra la lista de dayOffsets con scroll
                    ScrollingStringPickerColumn(
                        items = dayOffsets,
                        selectedIndex = selectedDayIndex,
                        onIndexChange = { selectedDayIndex = it },
                        label = "Days"
                    )
                } else {
                    // Muestra la lista de weekOffsets con scroll
                    ScrollingStringPickerColumn(
                        items = weekOffsets,
                        selectedIndex = selectedWeekIndex,
                        onIndexChange = { selectedWeekIndex = it },
                        label = "Weeks"
                    )
                }
                Row{

                    // COLUMNA 2: Hora (0..23)
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
                offsetMinutes = 0,
                exactDateTime = null
            )
            onReady(finalPlan)
        },
        onNeutral = {
            onDismiss()
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollingStringPickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    label: String
) {
    val itemHeight = 40.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val flingBehavior = rememberSnapFlingBehavior(listState)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .height(itemHeight * 5)
                .width(110.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = itemHeight * 2),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items.size) { index ->
                    val isSelected = (index == selectedIndex)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = items[index],
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // Ventana de selección centrada
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.8f)
                    .height(itemHeight)
                    .shapeAndBg()
            )
        }
    }

    // Snap al ítem central
    LaunchedEffect(listState, itemHeightPx) {
        snapshotFlow {
            val firstVisible = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            val offsetFirst = firstVisible?.offset ?: 0
            val offsetItems = offsetFirst / itemHeightPx

            listState.firstVisibleItemIndex to offsetItems
        }.collect { (firstIndex, offsetItems) ->
            val centerIndex = (firstIndex + offsetItems + 2.5f).roundToInt()
                .coerceIn(0, items.size - 1)

            if (centerIndex != selectedIndex) {
                onIndexChange(centerIndex)
            }
        }
    }

    // Forzar scroll inicial
    LaunchedEffect(selectedIndex) {
        listState.scrollToItem(selectedIndex)
    }
}

// Helper para "caja" del ítem seleccionado
@Composable
private fun Modifier.shapeAndBg() = this
    .background(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    )

