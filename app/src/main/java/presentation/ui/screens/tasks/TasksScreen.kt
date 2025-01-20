package presentation.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.presentation.intents.TaskIntent
import domain.models.*
import org.koin.androidx.compose.koinViewModel
import presentation.states.TaskState
import presentation.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    navController: NavHostController,
    viewModel: TaskViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddTaskSheet by remember { mutableStateOf(false) }

    // Load tasks once
    LaunchedEffect(Unit) {
        viewModel.triggerEvent(TaskIntent.LoadTasks)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All tasks", fontSize = 20.sp) },
                actions = {
                    IconButton(onClick = { /* future menu */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            } else {
                state.error?.let { err ->
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            text = err,
                            color = Color.Red,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } ?: TaskListSection(state)
            }
        }

        // The add-task sheet
        if (showAddTaskSheet) {
            AddTaskSheet(
                onClose = { showAddTaskSheet = false },
                onAccept = { newTaskData ->
                    // pass to ViewModel as an AddTask intent
                    val newTask = NewTaskData(
                        name = newTaskData.name,
                        priority = newTaskData.priority,
                        startDateConf = newTaskData.startDateConf,
                        endDateConf = newTaskData.endDateConf,
                        durationConf = newTaskData.durationConf,
                        reminderPlan = newTaskData.reminderPlan,
                        repeatPlan = newTaskData.repeatPlan,
                        subtasks = newTaskData.subtasks
                    )

                    viewModel.triggerEvent(
                        TaskIntent.AddTask(
                            name = newTask.name,
                            priority = newTask.priority,
                            startDateConf = newTask.startDateConf,
                            endDateConf = newTask.endDateConf,
                            durationConf = newTask.durationConf,
                            reminderPlan = newTask.reminderPlan,
                            repeatPlan = newTask.repeatPlan,
                            subtasks = newTask.subtasks
                        )
                    )

                    showAddTaskSheet = false
                }
            )
        }
    }
}

@Composable
fun TaskListSection(state: TaskState) {
    if (
        state.notCompletedTasks.isEmpty() &&
        state.completedTasks.isEmpty() &&
        state.expiredTasks.isEmpty()
    ) {
        Box(Modifier.fillMaxSize()) {
            Text("No tasks", Modifier.align(Alignment.Center))
        }
    } else {
        LazyColumn(Modifier.fillMaxWidth()) {
            item {
                TaskSection("Not Completed", state.notCompletedTasks)
            }
            item { Spacer(Modifier.height(16.dp)) }
            item {
                TaskSection("Completed", state.completedTasks)
            }
            item { Spacer(Modifier.height(16.dp)) }
            item {
                TaskSection("Expired", state.expiredTasks)
            }
        }
    }
}

@Composable
fun TaskSection(title: String, tasks: List<Task>) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(title, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        tasks.forEach { task ->
            TaskItemComposable(task)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun TaskItemComposable(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = {
                    // Could dispatch an intent to mark completed
                }
            )
            Spacer(Modifier.width(8.dp))

            Column(Modifier.weight(1f)) {
                Text(task.name, fontSize = 16.sp)
                // If there's a start date, end date, or something, display them as text
                task.startDateConf?.dateTime?.let {
                    Text("Start: $it", fontSize = 12.sp, color = Color.Gray)
                }
                task.endDateConf?.dateTime?.let {
                    Text("End: $it", fontSize = 12.sp, color = Color.Gray)
                }
                task.durationConf?.totalMinutes?.let {
                    val hours = it / 60
                    val mins = it % 60
                    val label = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                    Text("Duration: $label", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

/**
 * The default sheet with 4 icons:
 * 1) Calendar (which opens the time-related window)
 * 2) Priority
 * 3) Lists/Reminders
 * 4) Subtasks
 *
 * And at the top, a textfield for the task name, plus close & accept icons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    onClose: () -> Unit,
    onAccept: (NewTaskData) -> Unit
) {
    var taskName by remember { mutableStateOf("") }
    var showTimeConfigSheet by remember { mutableStateOf(false) }

    // We keep track of everything that might eventually go in the domain
    // but for now we only show the final "Accept" with name.
    // The rest is optional or goes in sub-screens.
    var timePlanningStart: TimePlanning? by remember { mutableStateOf(null) }
    var timePlanningEnd: TimePlanning? by remember { mutableStateOf(null) }
    var duration: DurationPlan? by remember { mutableStateOf(null) }
    var reminder: ReminderPlan? by remember { mutableStateOf(null) }
    var repeat: RepeatPlan? by remember { mutableStateOf(null) }

    ModalBottomSheet(
        onDismissRequest = onClose
    ) {
        // Top bar with close & check
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
                // Build the new task data object
                val newTask = NewTaskData(
                    name = taskName,
                    startDateConf = timePlanningStart,
                    endDateConf = timePlanningEnd,
                    durationConf = duration,
                    reminderPlan = reminder,
                    repeatPlan = repeat
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

            // The 4 icons row
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // 1) Calendar => opens time config
                IconButton(onClick = { showTimeConfigSheet = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = "Calendar"
                    )
                }
                // 2) Priority
                IconButton(onClick = { /* handle priority, or open another sub-sheet */ }) {
                    Icon(
                        painter = painterResource(R.drawable.priority),
                        contentDescription = "Priority"
                    )
                }
                // 3) Lists or Reminders
                IconButton(onClick = { /* handle lists or reminders?? you decide */ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lists),
                        contentDescription = "Lists"
                    )
                }
                // 4) Subtasks
                IconButton(onClick = { /* handle subtasks?? */ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_subtasks),
                        contentDescription = "Subtasks"
                    )
                }
            }
        }

        // If the user tapped the calendar icon, show the next sub-sheet
        if (showTimeConfigSheet) {
            TimeConfigSheet(
                onClose = { showTimeConfigSheet = false },
                currentStart = timePlanningStart,
                currentEnd = timePlanningEnd,
                currentDuration = duration,
                currentReminder = reminder,
                currentRepeat = repeat,
                onSaveAll = { newStart, newEnd, newDur, newRem, newRep ->
                    // Update local states
                    timePlanningStart = newStart
                    timePlanningEnd = newEnd
                    duration = newDur
                    reminder = newRem
                    repeat = newRep
                }
            )
        }
    }
}

/**
 * The sub-sheet that appears AFTER the user taps the calendar icon.
 * It has 5 rows:
 *  1) Start date
 *  2) End date
 *  3) Duration
 *  4) Reminder
 *  5) Repeat
 *
 * Tapping any of these opens a small floating dialog with the respective config.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeConfigSheet(
    onClose: () -> Unit,
    currentStart: TimePlanning?,
    currentEnd: TimePlanning?,
    currentDuration: DurationPlan?,
    currentReminder: ReminderPlan?,
    currentRepeat: RepeatPlan?,
    onSaveAll: (
        TimePlanning?,
        TimePlanning?,
        DurationPlan?,
        ReminderPlan?,
        RepeatPlan?
    ) -> Unit
) {
    // We copy local states so we can discard changes if the user closes
    var localStart by remember { mutableStateOf(currentStart) }
    var localEnd by remember { mutableStateOf(currentEnd) }
    var localDuration by remember { mutableStateOf(currentDuration) }
    var localReminder by remember { mutableStateOf(currentReminder) }
    var localRepeat by remember { mutableStateOf(currentRepeat) }

    var openWhichDialog by remember { mutableStateOf<TimeDialogType?>(null) }

    ModalBottomSheet(onDismissRequest = onClose) {
        // The "title bar"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close time config")
            }
            Text("Time options", fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterVertically))
            IconButton(onClick = {
                // Save the local changes to the parent
                onSaveAll(localStart, localEnd, localDuration, localReminder, localRepeat)
                onClose()
            }) {
                Icon(Icons.Default.Check, contentDescription = "Save time config")
            }
        }

        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            TimeConfigItem(
                label = "Start date",
                value = localStart?.dateTime?.toString() ?: "None"
            ) { openWhichDialog = TimeDialogType.StartDate }
            Divider()
            TimeConfigItem(
                label = "End date",
                value = localEnd?.dateTime?.toString() ?: "None"
            ) { openWhichDialog = TimeDialogType.EndDate }
            Divider()
            TimeConfigItem(
                label = "Duration",
                value = localDuration?.totalMinutes?.let {
                    val h = it / 60
                    val m = it % 60
                    if (h>0) "${h}h ${m}m" else "${m}m"
                } ?: "None"
            ) { openWhichDialog = TimeDialogType.Duration }
            Divider()
            TimeConfigItem(
                label = "Reminder",
                value = localReminder?.mode?.name ?: "None"
            ) { openWhichDialog = TimeDialogType.Reminder }
            Divider()
            TimeConfigItem(
                label = "Repeat",
                value = localRepeat?.frequencyType?.name ?: "None"
            ) { openWhichDialog = TimeDialogType.Repeat }
        }

        // The "floating dialogs" that appear above
        when (openWhichDialog) {
            TimeDialogType.StartDate -> {
                FloatingStartEndDateDialog(
                    label = "Start date",
                    existing = localStart,
                    onDismiss = { openWhichDialog = null },
                    onReady = { newVal ->
                        localStart = newVal
                        openWhichDialog = null
                    }
                )
            }
            TimeDialogType.EndDate -> {
                FloatingStartEndDateDialog(
                    label = "End date",
                    existing = localEnd,
                    onDismiss = { openWhichDialog = null },
                    onReady = { newVal ->
                        localEnd = newVal
                        openWhichDialog = null
                    }
                )
            }
            TimeDialogType.Duration -> {
                FloatingDurationDialog(
                    existing = localDuration,
                    onDismiss = { openWhichDialog = null },
                    onReady = { newVal ->
                        localDuration = newVal
                        openWhichDialog = null
                    }
                )
            }
            TimeDialogType.Reminder -> {
                FloatingReminderDialog(
                    existing = localReminder,
                    onDismiss = { openWhichDialog = null },
                    onReady = { newVal ->
                        localReminder = newVal
                        openWhichDialog = null
                    }
                )
            }
            TimeDialogType.Repeat -> {
                FloatingRepeatDialog(
                    existing = localRepeat,
                    onDismiss = { openWhichDialog = null },
                    onReady = { newVal ->
                        localRepeat = newVal
                        openWhichDialog = null
                    }
                )
            }
            else -> {}
        }
    }
}

@Composable
fun TimeConfigItem(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * The 5 possible items in the time config. We open a "dialog" for each.
 */
enum class TimeDialogType {
    StartDate, EndDate, Duration, Reminder, Repeat
}

/**
 * A "floating" style small window for picking a date/time, morning/evening, etc.
 * Like your 3rd or 4th screenshot.
 */
@Composable
fun FloatingStartEndDateDialog(
    label: String,
    existing: TimePlanning?,
    onDismiss: () -> Unit,
    onReady: (TimePlanning?) -> Unit
) {
    // We'll emulate a small "dialog" by using a Surface that's smaller.
    // You can also use AlertDialog if you prefer.
    var date by remember { mutableStateOf(existing?.dateTime?.toLocalDate() ?: LocalDate.now()) }
    var time by remember { mutableStateOf(existing?.dateTime?.toLocalTime() ?: LocalTime.now()) }
    var dayPeriod by remember { mutableStateOf(existing?.dayPeriod) }

    // We'll show a simple column with a calendar (1..30?), dayPeriod icons, hour & minute sliders
    DialogSurface {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(label, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterVertically))
                // Optionally a "None" button if you want to reset
                TextButton(onClick = { onReady(null) }) {
                    Text("None")
                }
            }

            Spacer(Modifier.height(8.dp))

            // DayPeriod row
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DayPeriod.values().forEach { dp ->
                    Column(
                        modifier = Modifier
                            .clickable { dayPeriod = dp }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar),
                            contentDescription = dp.name,
                            tint = if (dp == dayPeriod) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Text(dp.name, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text("Pick a date (1..30) placeholder:")
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                for (d in 1..30) {
                    val selected = (date.dayOfMonth == d)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (selected) Color(0xFFFFC107) else Color.LightGray)
                            .clickable {
                                date = LocalDate.of(date.year, date.monthValue, d)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$d")
                    }
                    Spacer(Modifier.width(4.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Hour & minute
            var hour by remember { mutableStateOf(time.hour) }
            var minute by remember { mutableStateOf(time.minute) }

            Text("Hour: $hour")
            Slider(
                value = hour.toFloat(),
                onValueChange = { hour = it.toInt() },
                valueRange = 0f..23f,
                steps = 23
            )
            Text("Minute: $minute")
            Slider(
                value = minute.toFloat(),
                onValueChange = { minute = it.toInt() },
                valueRange = 0f..59f,
                steps = 59
            )

            // Update time with the new hour/min
            time = LocalTime.of(hour, minute)

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val chosen = TimePlanning(
                        dateTime = LocalDateTime.of(date, time),
                        dayPeriod = dayPeriod
                    )
                    onReady(chosen)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Ready")
            }
        }
    }
}

/**
 * A small "dialog" for picking the duration in minutes or hours.
 * Like your second screenshot.
 */
@Composable
fun FloatingDurationDialog(
    existing: DurationPlan?,
    onDismiss: () -> Unit,
    onReady: (DurationPlan?) -> Unit
) {
    var useHours by remember { mutableStateOf(false) }
    var number by remember { mutableStateOf(0) }

    // If existing is set, interpret it
    LaunchedEffect(Unit) {
        existing?.totalMinutes?.let { mins ->
            if (mins % 60 == 0) {
                useHours = true
                number = mins / 60
            } else {
                useHours = false
                number = mins
            }
        }
    }

    DialogSurface {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                Text("Duration", fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterVertically))
                // "None"
                TextButton(onClick = { onReady(null) }) {
                    Text("None")
                }
            }
            Spacer(Modifier.height(16.dp))

            // A row with +/- for number, then a toggle for hours/min
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { if (number > 0) number-- }) { Text("-") }
                Text("$number", fontSize = 20.sp)
                TextButton(onClick = { number++ }) { Text("+") }

                Spacer(Modifier.width(16.dp))

                // Toggle for "Hours" or "Minutes"
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Text(
                        text = if (useHours) "Hours" else "Minutes",
                        modifier = Modifier.clickable { expanded = true }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Minutes") },
                            onClick = {
                                useHours = false
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hours") },
                            onClick = {
                                useHours = true
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val total = if (useHours) number * 60 else number
                    onReady(DurationPlan(total))
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Ready")
            }
        }
    }
}

/**
 * A small "dialog" for the reminder, with:
 *  - None
 *  - On time
 *  - 5 min early
 *  - 30 min early
 *  - 1 day early
 *  - 1 week early
 *  - Personalized => leads to a second sub-window
 */
@Composable
fun FloatingReminderDialog(
    existing: ReminderPlan?,
    onDismiss: () -> Unit,
    onReady: (ReminderPlan?) -> Unit
) {
    var showPersonalized by remember { mutableStateOf(false) }
    var localRem by remember { mutableStateOf(existing) }

    if (!showPersonalized) {
        DialogSurface {
            Column(Modifier.padding(16.dp)) {
                // Header row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                    Text("Reminder", fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    TextButton(onClick = { onReady(null) }) {
                        Text("None")
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Some standard offsets
                val presetOffsets = listOf(
                    "On time" to 0,
                    "5 min early" to 5,
                    "30 min early" to 30,
                    "1 day early" to (24*60),
                    "1 week early" to (7*24*60)
                )

                presetOffsets.forEach { (label, offset) ->
                    val selected = localRem?.offsetMinutes == offset
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                localRem = ReminderPlan(
                                    mode = ReminderMode.PRESET_OFFSET,
                                    offsetMinutes = offset
                                )
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label)
                        if (selected) {
                            Icon(Icons.Default.Check, contentDescription = "selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Divider()
                }

                // Personalized
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPersonalized = true }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Personalized")
                    if (localRem?.mode == ReminderMode.CUSTOM) {
                        Icon(Icons.Default.Check, contentDescription = "selected", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        onReady(localRem)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Ready")
                }
            }
        }
    } else {
        // Personalized sub-window
        FloatingPersonalizedReminderDialog(
            existing = localRem,
            onBack = { showPersonalized = false },
            onConfirm = {
                localRem = it
                showPersonalized = false
            }
        )
    }
}

/** The subwindow for the "personalized" reminder: multiple sliders, etc. */
@Composable
fun FloatingPersonalizedReminderDialog(
    existing: ReminderPlan?,
    onBack: () -> Unit,
    onConfirm: (ReminderPlan) -> Unit
) {
    var daysBefore by remember { mutableStateOf(0) }
    var hour by remember { mutableStateOf(0) }
    var minute by remember { mutableStateOf(0) }

    // If existing is custom with offset, parse it
    LaunchedEffect(Unit) {
        if (existing?.mode == ReminderMode.CUSTOM && existing.offsetMinutes != null) {
            val total = existing.offsetMinutes
            daysBefore = total / (24*60)
            val leftover = total % (24*60)
            hour = leftover / 60
            minute = leftover % 60
        }
    }

    DialogSurface {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                Text("Personalized Reminder", fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterVertically))
                Spacer(Modifier.width(48.dp))
            }
            Spacer(Modifier.height(8.dp))

            Text("Days before: $daysBefore")
            Slider(
                value = daysBefore.toFloat(),
                onValueChange = { daysBefore = it.toInt() },
                valueRange = 0f..7f,
                steps = 7
            )
            Text("Hour: $hour")
            Slider(
                value = hour.toFloat(),
                onValueChange = { hour = it.toInt() },
                valueRange = 0f..23f,
                steps = 23
            )
            Text("Minute: $minute")
            Slider(
                value = minute.toFloat(),
                onValueChange = { minute = it.toInt() },
                valueRange = 0f..59f,
                steps = 59
            )

            Button(
                onClick = {
                    val totalOffset = daysBefore*(24*60) + hour*60 + minute
                    val plan = ReminderPlan(
                        mode = ReminderMode.CUSTOM,
                        offsetMinutes = totalOffset
                    )
                    onConfirm(plan)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Ready")
            }
        }
    }
}

/**
 * The "floating" repeat dialog with none, daily, weekly, monthly, anually, weekdays, weekends, personalized
 */
@Composable
fun FloatingRepeatDialog(
    existing: RepeatPlan?,
    onDismiss: () -> Unit,
    onReady: (RepeatPlan?) -> Unit
) {
    var showPersonalized by remember { mutableStateOf(false) }
    var localRepeat by remember { mutableStateOf(existing ?: RepeatPlan(FrequencyType.NONE)) }

    if (!showPersonalized) {
        DialogSurface {
            Column(Modifier.padding(16.dp)) {
                // Header
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                    Text("Repeat", fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    TextButton(onClick = { onReady(null) }) {
                        Text("None")
                    }
                }
                Spacer(Modifier.height(8.dp))

                // A list of quick picks
                val picks = listOf(
                    FrequencyType.DAILY,
                    FrequencyType.WEEKLY,
                    FrequencyType.MONTHLY,
                    FrequencyType.YEARLY,
                    FrequencyType.WEEKDAYS,
                    FrequencyType.WEEKENDS
                )
                picks.forEach { ft ->
                    val selected = (localRepeat.frequencyType == ft)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                localRepeat = localRepeat.copy(frequencyType = ft)
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ft.name)
                        if (selected) {
                            Icon(Icons.Default.Check, contentDescription = "selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Divider()
                }

                // Personalized
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showPersonalized = true }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Personalized")
                    if (localRepeat.frequencyType == FrequencyType.CUSTOM) {
                        Icon(Icons.Default.Check, contentDescription = "selected", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onReady(localRepeat) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Ready")
                }
            }
        }
    } else {
        // Show the "Personalized" sub-window
        FloatingPersonalizedRepeatDialog(
            existing = localRepeat,
            onBack = { showPersonalized = false },
            onConfirm = { newVal ->
                localRepeat = newVal
                showPersonalized = false
            }
        )
    }
}

/** The sub-window for personalized repeat: an interval, plus a weekday selection. */
@Composable
fun FloatingPersonalizedRepeatDialog(
    existing: RepeatPlan?,
    onBack: () -> Unit,
    onConfirm: (RepeatPlan) -> Unit
) {
    var interval by remember { mutableStateOf(1) }
    var selectedWeekdays = remember { mutableStateListOf<Int>() }

    LaunchedEffect(Unit) {
        existing?.let {
            interval = it.interval ?: 1
            if (it.selectedWeekdays != null) {
                selectedWeekdays.clear()
                selectedWeekdays.addAll(it.selectedWeekdays)
            }
        }
    }

    val daysOfWeek = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")

    DialogSurface {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                Text("Personalized Repeat", fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterVertically))
                Spacer(Modifier.width(48.dp))
            }
            Spacer(Modifier.height(8.dp))

            Text("Interval: $interval")
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { if (interval>1) interval-- }) { Text("-") }
                Text("$interval", fontSize = 20.sp)
                TextButton(onClick = { interval++ }) { Text("+") }
            }

            Spacer(Modifier.height(8.dp))
            Text("Pick weekdays if relevant:")
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                daysOfWeek.forEachIndexed { idx, day ->
                    val dayNum = idx+1
                    val selected = selectedWeekdays.contains(dayNum)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) selectedWeekdays.remove(dayNum)
                            else selectedWeekdays.add(dayNum)
                        },
                        label = { Text(day) }
                    )
                    Spacer(Modifier.width(4.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val newRep = RepeatPlan(
                        frequencyType = FrequencyType.CUSTOM,
                        interval = interval,
                        selectedWeekdays = if (selectedWeekdays.isEmpty()) null else selectedWeekdays.toList()
                    )
                    onConfirm(newRep)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Ready")
            }
        }
    }
}

/**
 * Simple helper to mimic a smaller "floating" dialog box inside the sheet.
 * You could also use AlertDialog with `Dialog(onDismissRequest=..., ...)`.
 */
@Composable
fun DialogSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000)) // semi-transparent backdrop
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(min = 280.dp, max = 400.dp)
                .wrapContentHeight()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            content()
        }
    }
}

/**
 * A container for the final data before we call onAccept in the AddTaskSheet.
 * This might map directly to your domain or could be a partial structure.
 */
data class NewTaskData(
    val name: String = "",

    val priority: Priority = Priority.NONE,

    val startDateConf: TimePlanning? = null,
    val endDateConf: TimePlanning? = null,
    val durationConf: DurationPlan? = null,

    val reminderPlan: ReminderPlan? = null,
    val repeatPlan: RepeatPlan? = null,

    val subtasks: List<Subtask> = emptyList()
)
