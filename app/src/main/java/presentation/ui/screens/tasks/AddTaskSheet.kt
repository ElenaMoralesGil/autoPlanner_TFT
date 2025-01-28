package com.elena.autoplanner.presentation.ui.screens.tasks


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.res.painterResource
import com.elena.autoplanner.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    onClose: () -> Unit,
    onAccept: (NewTaskData) -> Unit
) {
    // Basic fields
    var taskName by remember { mutableStateOf("") }
    var showTimeConfigSheet by remember { mutableStateOf(false) }
    var showPrioritySheet by remember { mutableStateOf(false) }
    var showListsSheet by remember { mutableStateOf(false) }
    var showSubtasksSheet by remember { mutableStateOf(false) }

    // Domain states for advanced features
    var timePlanningStart: TimePlanning? by remember { mutableStateOf(null) }
    var timePlanningEnd: TimePlanning? by remember { mutableStateOf(null) }
    var duration: DurationPlan? by remember { mutableStateOf(null) }
    var reminder: ReminderPlan? by remember { mutableStateOf(null) }
    var repeat: RepeatPlan? by remember { mutableStateOf(null) }
    // Priority, Subtasks, etc. can also be states if you want

    // The main bottom sheet
    ModalBottomSheet(onDismissRequest = onClose) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
            IconButton(onClick = {
                // Build newTask and pass it out
                val newTask = NewTaskData(
                    name = taskName,
                    priority = Priority.NONE,
                    startDateConf = timePlanningStart,
                    endDateConf = timePlanningEnd,
                    durationConf = duration,
                    reminderPlan = reminder,
                    repeatPlan = repeat,
                    subtasks = emptyList()
                )
                onAccept(newTask)
            }) {
                Icon(Icons.Default.Check, contentDescription = "Check")
            }
        }

        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = taskName,
                onValueChange = { taskName = it },
                label = { Text("Task name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))


            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                IconButton(onClick = { showTimeConfigSheet = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = "Calendar"
                    )
                }

                IconButton(onClick = { showPrioritySheet = true }) {
                    Icon(
                        painter = painterResource(R.drawable.priority),
                        contentDescription = "Priority"
                    )
                }

                IconButton(onClick = { showListsSheet = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lists),
                        contentDescription = "Lists"
                    )
                }

                IconButton(onClick = { showSubtasksSheet = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_subtasks),
                        contentDescription = "Subtasks"
                    )
                }
            }


        }

        // If user taps "Calendar" => open the TimeConfigSheet
        if (showTimeConfigSheet) {
            TimeConfigSheet(
                onClose = { showTimeConfigSheet = false },
                currentStart = timePlanningStart,
                currentEnd = timePlanningEnd,
                currentDuration = duration,
                currentReminder = reminder,
                currentRepeat = repeat
            ) { newStart, newEnd, newDur, newRem, newRep ->
                timePlanningStart = newStart
                timePlanningEnd = newEnd
                duration = newDur
                reminder = newRem
                repeat = newRep
            }
        }
    }
}

/**
 * A simplified data class to store the new task fields
 * before dispatching them to the ViewModel.
 */
data class NewTaskData(
    val name: String,
    val priority: Priority = Priority.NONE,
    val startDateConf: TimePlanning? = null,
    val endDateConf: TimePlanning? = null,
    val durationConf: DurationPlan? = null,
    val reminderPlan: ReminderPlan? = null,
    val repeatPlan: RepeatPlan? = null,
    val subtasks: List<Subtask> = emptyList()
)
