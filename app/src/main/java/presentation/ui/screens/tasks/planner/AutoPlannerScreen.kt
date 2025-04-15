package com.elena.autoplanner.presentation.ui.screens.tasks.planner

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox // Added Checkbox import
import androidx.compose.material3.CheckboxDefaults // Added CheckboxDefaults import
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times // Keep this import
import androidx.compose.ui.zIndex
import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.DayOrganization
import com.elena.autoplanner.domain.models.OverdueTaskHandling
import com.elena.autoplanner.domain.models.PlannerStep
import com.elena.autoplanner.domain.models.PrioritizationStrategy
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.ResolutionOption
import com.elena.autoplanner.domain.models.ScheduleScope
import com.elena.autoplanner.domain.models.ScheduledTaskItem
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.effects.PlannerEffect
import com.elena.autoplanner.presentation.effects.TaskEditEffect
import com.elena.autoplanner.presentation.intents.PlannerIntent
import com.elena.autoplanner.presentation.states.PlannerState
import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarView
import com.elena.autoplanner.presentation.ui.screens.tasks.ModificationTaskSheet.ModificationTaskSheet
import com.elena.autoplanner.presentation.ui.screens.tasks.TaskDetailSheet
import com.elena.autoplanner.presentation.viewmodel.PlannerViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskDetailViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskEditViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerTopAppBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = { Text("Auto Planner") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    state: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    validateTime: (LocalTime) -> Boolean = { true },
    errorMessage: String = "Invalid time selection",
) {
    var showError by remember { mutableStateOf(false) }
    BasicAlertDialog(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Select Time",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                TimePicker(state = state)
                if (showError) {
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val selectedTime = LocalTime.of(state.hour, state.minute)
                        if (validateTime(selectedTime)) {
                            onConfirm(); showError = false
                        } else {
                            showError = true
                        }
                    }) { Text("OK") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPlannerScreen(
    viewModel: PlannerViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var selectedTaskIdForDetail by remember { mutableStateOf<Int?>(null) } // Corrected declaration
    var taskIdForEditSheet by remember { mutableStateOf<Int?>(null) }
    var taskForEditSheet by remember { mutableStateOf<Task?>(null) }

    val startTimeState = state?.workStartTime?.let {
        rememberTimePickerState(
            initialHour = it.hour,
            initialMinute = it.minute,
            is24Hour = true
        )
    }
    val endTimeState = state?.workEndTime?.let {
        rememberTimePickerState(
            initialHour = it.hour,
            initialMinute = it.minute,
            is24Hour = true
        )
    }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PlannerEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is PlannerEffect.NavigateBack -> onNavigateBack()
            }
        }
    }


    if (showStartTimePicker && startTimeState != null) {
        TimePickerDialog(
            state = startTimeState,
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                val selectedTime = LocalTime.of(startTimeState.hour, startTimeState.minute)
                viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(selectedTime))
                showStartTimePicker = false
            }
        )
    }


    if (showEndTimePicker && endTimeState != null && startTimeState != null) {
        TimePickerDialog(
            state = endTimeState,
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                viewModel.sendIntent(
                    PlannerIntent.UpdateWorkEndTime(
                        LocalTime.of(
                            endTimeState.hour,
                            endTimeState.minute
                        )
                    )
                )
                showEndTimePicker = false
            },

            validateTime = { selectedEndTime ->
                selectedEndTime > LocalTime.of(startTimeState.hour, startTimeState.minute)
            },
            errorMessage = "End time must be after start time"
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PlannerTopAppBar(onBackClick = {
                if (state?.currentStep == PlannerStep.TIME_INPUT) viewModel.sendIntent(PlannerIntent.CancelPlanner)
                else viewModel.sendIntent(PlannerIntent.GoToPreviousStep)
            })
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            state?.let { currentState ->
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    PlannerNavigationButtons(
                        state = currentState,
                        onIntent = viewModel::sendIntent,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                    end = paddingValues.calculateEndPadding(LayoutDirection.Ltr)
                    // Removed bottom padding to prevent double padding with bottomBar
                )
                .fillMaxSize()
        ) {
            state?.let { currentState ->
                PlannerContent(
                    state = currentState,
                    onIntent = viewModel::sendIntent,
                    onStartTimeClick = { if (startTimeState != null) showStartTimePicker = true },
                    onEndTimeClick = { if (endTimeState != null) showEndTimePicker = true },
                    onReviewTaskClick = { task ->
                        // Decide whether to show detail or edit based on manual flag
                        if (currentState.tasksFlaggedForManualEdit.contains(task.id)) { // Added check if tasksFlaggedForManualEdit exists
                            Log.d("AutoPlannerScreen", "Edit flagged task clicked: ${task.id}")
                            taskForEditSheet = task // Store task for context
                            taskIdForEditSheet = task.id // Trigger edit sheet
                            selectedTaskIdForDetail =
                                null // Ensure detail sheet is closed // Corrected variable name
                        } else {
                            Log.d("AutoPlannerScreen", "Normal task clicked: ${task.id}")
                            selectedTaskIdForDetail =
                                task.id // Trigger detail sheet // Corrected variable name
                            taskIdForEditSheet = null // Ensure edit sheet is closed
                        }
                    }
                )
            } ?: CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            if (state?.isLoading == true) {
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(10f)
                ) {
                    CircularProgressIndicator(modifier = Modifier.wrapContentSize(Alignment.Center))
                }
            }
        }

        // --- Task Detail Sheet ---
        selectedTaskIdForDetail?.let { taskId -> // Corrected variable name
            val detailViewModel: TaskDetailViewModel =
                koinViewModel(parameters = { parametersOf(taskId) })
            LaunchedEffect(detailViewModel, taskId) {
                detailViewModel.effect.collect { effect ->
                    when (effect) {
                        is com.elena.autoplanner.presentation.effects.TaskDetailEffect.NavigateBack -> selectedTaskIdForDetail =
                            null // Corrected variable name
                        // --- Handle Navigation to Edit from Detail Sheet ---
                        is com.elena.autoplanner.presentation.effects.TaskDetailEffect.NavigateToEdit -> {
                            selectedTaskIdForDetail =
                                null // Close detail // Corrected variable name
                            // Need the actual task object to pass to Modification sheet if needed,
                            // might need to fetch it again or pass from the review click handler
                            taskForEditSheet = state?.generatedPlan?.values?.flatten()
                                ?.find { it.task.id == effect.taskId }?.task
                                ?: state?.expiredTasksToResolve?.find { it.id == effect.taskId }
                                        ?: state?.conflictsToResolve?.flatMap { it.conflictingTasks }
                                    ?.find { it.id == effect.taskId }
                            taskIdForEditSheet = effect.taskId // Open edit sheet
                        }

                        is com.elena.autoplanner.presentation.effects.TaskDetailEffect.ShowSnackbar -> snackbarHostState.showSnackbar(
                            effect.message
                        )
                    }
                }
            }
            TaskDetailSheet(
                taskId = taskId,
                onDismiss = { selectedTaskIdForDetail = null }, // Corrected variable name
                viewModel = detailViewModel
            )
        }

        // --- Modification Task Sheet (for Manual Edit/Regular Edit) ---
        taskIdForEditSheet?.let { taskIdToEdit ->
            val editViewModel: TaskEditViewModel =
                koinViewModel(parameters = { parametersOf(taskIdToEdit) })

            // Handle effects (closing the sheet, showing snackbars)
            LaunchedEffect(editViewModel, taskIdToEdit) {
                editViewModel.effect.collect { effect ->
                    when (effect) {
                        is TaskEditEffect.NavigateBack -> {
                            taskIdForEditSheet = null // Close the sheet
                            taskForEditSheet = null
                            // Send intent to acknowledge manual edit (if triggered from review)
                            if (state?.tasksFlaggedForManualEdit?.contains(taskIdToEdit) == true) { // Added check if tasksFlaggedForManualEdit exists
                                viewModel.sendIntent(PlannerIntent.AcknowledgeManualEdits) // Or potentially UpdateManuallyEditedTask if data was passed back
                            }
                            // Optional: Refresh the planner preview if needed, though not strictly necessary as save hasn't happened
                        }

                        is TaskEditEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }
            // Load the task data into the edit VM when the sheet opens
            LaunchedEffect(taskIdToEdit) {
                editViewModel.sendIntent(
                    com.elena.autoplanner.presentation.intents.TaskEditIntent.LoadTask(
                        taskIdToEdit
                    )
                )
            }

            ModificationTaskSheet(
                taskEditViewModel = editViewModel,
                onClose = { editViewModel.sendIntent(com.elena.autoplanner.presentation.intents.TaskEditIntent.Cancel) } // Standard cancel closes sheet
            )
        }

        // Duplicate detail sheet logic? Removed this duplicate block.
        // selectedTaskIdForDetail?.let { taskId -> ... }
    }
}

@Composable
fun PlannerContent(
    state: PlannerState,
    onIntent: (PlannerIntent) -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onReviewTaskClick: (Task) -> Unit, // Added callback parameter
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Allows content to scroll if it overflows
            .padding(bottom = 80.dp), // Add padding at the bottom to avoid overlap with navigation buttons
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp) // Spacing between steps/cards
    ) {
        Text(
            text = "Let's create your optimized schedule!",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp), // Adjusted padding
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        when (state.currentStep) {
            PlannerStep.TIME_INPUT -> Step1TimeInput(
                state,
                onIntent,
                onStartTimeClick,
                onEndTimeClick
            )
            PlannerStep.PRIORITY_INPUT -> Step2PriorityInput(state, onIntent)
            PlannerStep.ADDITIONAL_OPTIONS -> Step3AdditionalOptions(state, onIntent)
            PlannerStep.REVIEW_PLAN -> Step4ReviewPlan(
                state,
                onIntent,
                onReviewTaskClick
            ) // Pass callback
        }

        if (state.error != null) {
            Text(
                "Error: ${state.error}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        // Removed Spacer as bottom padding is handled by Column modifier
    }
}

@Composable
fun Step1TimeInput(
    state: PlannerState,
    onIntent: (PlannerIntent) -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) { // Add padding
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Work/Availability Hours",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimeSelectorButton(state.workStartTime, onClick = onStartTimeClick)
                    Text(
                        " to ",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TimeSelectorButton(state.workEndTime, onClick = onEndTimeClick)
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Scheduling Scope",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RadioGroupColumn(
                    options = ScheduleScope.entries.toList(),
                    selectedOption = state.scheduleScope,
                    onOptionSelected = { onIntent(PlannerIntent.SelectScheduleScope(it)) },
                    labelSelector = { it.toDisplayString() })
            }
        }
    }
}

@Composable
fun TimeSelectorButton(currentTime: LocalTime, onClick: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = currentTime.format(formatter),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}


@Composable
fun Step2PriorityInput(state: PlannerState, onIntent: (PlannerIntent) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) { // Add padding
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Prioritization Strategy",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RadioGroupColumn(
                    options = PrioritizationStrategy.entries.toList(),
                    selectedOption = state.selectedPriority, // Use selectedPriorities (Set)
                    onOptionSelected = { priority -> // Lambda receives priority and checked state
                        onIntent(PlannerIntent.SelectPriority(priority)) // Use TogglePriority intent
                    },
                    labelSelector = { it.toDisplayString() }
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Day Organization Style",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RadioGroupColumn(
                    options = DayOrganization.entries.toList(),
                    selectedOption = state.selectedDayOrganization,
                    onOptionSelected = { organization ->
                        onIntent(
                            PlannerIntent.SelectDayOrganization(organization)
                        )
                    },
                    labelSelector = { it.toDisplayString() })
            }
        }
    }
}

@Composable
fun Step3AdditionalOptions(state: PlannerState, onIntent: (PlannerIntent) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) { // Add padding
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Task Splitting",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Allow splitting tasks into subtasks if they have durations?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RadioGroupColumn(
                    options = listOf(true, false),
                    selectedOption = state.allowSplitting,
                    onOptionSelected = { show -> onIntent(PlannerIntent.SelectAllowSplitting(show)) }, // Intent name is correct
                    labelSelector = { if (it) "Yes, allow splitting" else "No, keep tasks whole" })
            }
        }
        if (state.numOverdueTasks > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Overdue Tasks",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "You have ${state.numOverdueTasks} overdue task(s). How to handle?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    RadioGroupColumn(
                        options = OverdueTaskHandling.entries.toList(),
                        selectedOption = state.selectedOverdueHandling,
                        onOptionSelected = { handling ->
                            onIntent(
                                PlannerIntent.SelectOverdueHandling(handling)
                            )
                        },
                        labelSelector = { it.toDisplayString() })
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "No overdue tasks found!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewSectionHeader(title: String, isError: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp, start = 4.dp) // Adjusted padding
    ) {
        Icon(
            imageVector = if (isError) Icons.Default.Warning else Icons.Default.Info,
            contentDescription = if (isError) "Action Required" else "Information",
            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun Step4ReviewPlan(
    state: PlannerState,
    onIntent: (PlannerIntent) -> Unit,
    onReviewTaskClick: (Task) -> Unit,
) {
    var currentCalendarView by remember { mutableStateOf(CalendarView.WEEK) }

    val planStartDate = remember(state.generatedPlan, state.scheduleScope) {
        state.generatedPlan.keys.minOrNull() ?: state.scheduleScope?.let { scope ->
            val today = LocalDate.now()
            when (scope) {
                ScheduleScope.TODAY -> today
                ScheduleScope.TOMORROW -> today.plusDays(1)
                ScheduleScope.THIS_WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            }
        } ?: LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp) // Consistent spacing
    ) {

        AnimatedVisibility(visible = state.requiresResolution) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                            alpha = 0.4f
                        )
                    ),
                    elevation = CardDefaults.cardElevation(0.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val unresolvedExpired =
                            remember(state.expiredTasksToResolve, state.taskResolutions) {
                                state.expiredTasksToResolve.filter { state.taskResolutions[it.id] == null }
                            }
                        if (unresolvedExpired.isNotEmpty()) {
                            ReviewSectionHeader(
                                title = "Action Required: Expired Tasks (${unresolvedExpired.size})",
                                isError = true
                            )
                            unresolvedExpired.forEach { task ->
                                ResolutionCard(
                                    task = task,
                                    options = listOf(
                                        ResolutionOption.MOVE_TO_TOMORROW,
                                        ResolutionOption.MANUALLY_SCHEDULE
                                    ),
                                    selectedOption = state.taskResolutions[task.id],
                                    onOptionSelected = { option ->
                                        onIntent(PlannerIntent.ResolveExpiredTask(task, option))
                                        if (option == ResolutionOption.MANUALLY_SCHEDULE) {
                                            onIntent(PlannerIntent.FlagTaskForManualEdit(task.id))
                                        } else {
                                            // Remove flag if another option is chosen
                                            onIntent(PlannerIntent.UnflagTaskForManualEdit(task.id))
                                        }
                                    }
                                )
                            }
                        }

                        val unresolvedConflicts =
                            remember(state.conflictsToResolve, state.conflictResolutions) {
                                state.conflictsToResolve.filter { state.conflictResolutions[it.hashCode()] == null }
                            }
                        if (unresolvedConflicts.isNotEmpty()) {
                            ReviewSectionHeader(
                                title = "Action Required: Scheduling Conflicts (${unresolvedConflicts.size})",
                                isError = true
                            )
                            unresolvedConflicts.forEach { conflict ->
                                ConflictResolutionCard(
                                    conflict = conflict,
                                    options = listOf(
                                        ResolutionOption.MOVE_TO_TOMORROW,
                                        ResolutionOption.MANUALLY_SCHEDULE,
                                        ResolutionOption.LEAVE_IT_LIKE_THAT
                                    ),
                                    selectedOption = state.conflictResolutions[conflict.hashCode()],
                                    onOptionSelected = { option ->
                                        onIntent(PlannerIntent.ResolveConflict(conflict, option))
                                        val taskToFlag =
                                            conflict.conflictingTasks.minByOrNull { it.priority.ordinal }
                                        taskToFlag?.let {
                                            if (option == ResolutionOption.MANUALLY_SCHEDULE || option == ResolutionOption.LEAVE_IT_LIKE_THAT) {
                                                onIntent(PlannerIntent.FlagTaskForManualEdit(it.id))
                                            } else {
                                                // Remove flag if another option is chosen
                                                onIntent(PlannerIntent.UnflagTaskForManualEdit(it.id))
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                if (state.requiresResolution) {
                    Text(
                        "Please resolve the items above to add the plan",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Generated Plan Preview",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ViewToggleButtons(
                    selectedView = currentCalendarView,
                    onViewSelected = { currentCalendarView = it })

                Box(
                    modifier = Modifier
                        .heightIn(min = 250.dp, max = 550.dp)
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (state.generatedPlan.isNotEmpty()) {
                        GeneratedPlanReviewView(
                            viewType = currentCalendarView,
                            plan = state.generatedPlan,
                            startDate = planStartDate,
                            onTaskClick = onReviewTaskClick,
                            tasksFlaggedForManualEdit = state.tasksFlaggedForManualEdit // Pass flags
                        )
                    } else if (!state.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No tasks scheduled. Try adjusting the options.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    if (state.isLoading && state.currentStep == PlannerStep.REVIEW_PLAN) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(40.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun <T> CheckboxGroupColumn(
    options: List<T>,
    selectedOptions: Set<T>,
    onOptionSelected: (T, Boolean) -> Unit,
    labelSelector: (T) -> String,
) {
    Column {
        options.forEach { option ->
            val isSelected = selectedOptions.contains(option)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOptionSelected(option, !isSelected) }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { checked -> onOptionSelected(option, checked) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = labelSelector(option),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun <T> RadioGroupColumn(
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    labelSelector: (T) -> String,
) {
    Column {
        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = option == selectedOption,
                    onClick = { onOptionSelected(option) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = labelSelector(option),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PlannerNavigationButtons(
    state: PlannerState,
    onIntent: (PlannerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedButton(
            onClick = {
                if (state.currentStep == PlannerStep.TIME_INPUT) {
                    onIntent(PlannerIntent.CancelPlanner)
                } else {
                    onIntent(PlannerIntent.GoToPreviousStep)
                }
            },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text(
                if (state.currentStep == PlannerStep.TIME_INPUT) "Cancel" else "Back",
                color = MaterialTheme.colorScheme.primary
            )
        }

        Button(
            onClick = {
                when (state.currentStep) {
                    PlannerStep.TIME_INPUT, PlannerStep.PRIORITY_INPUT -> onIntent(PlannerIntent.GoToNextStep)
                    PlannerStep.ADDITIONAL_OPTIONS -> onIntent(PlannerIntent.GeneratePlan)
                    PlannerStep.REVIEW_PLAN -> onIntent(PlannerIntent.AddPlanToCalendar)
                }
            },

            enabled = when (state.currentStep) {
                PlannerStep.TIME_INPUT -> state.canMoveToStep2
                PlannerStep.PRIORITY_INPUT -> state.canMoveToStep3
                PlannerStep.ADDITIONAL_OPTIONS -> state.canGeneratePlan
                PlannerStep.REVIEW_PLAN -> !state.requiresResolution && !state.isLoading
            } && !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = when (state.currentStep) {
                    PlannerStep.TIME_INPUT, PlannerStep.PRIORITY_INPUT -> "Continue"
                    PlannerStep.ADDITIONAL_OPTIONS -> "Generate Plan"
                    PlannerStep.REVIEW_PLAN -> "Add Plan"
                },
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun ViewToggleButtons(selectedView: CalendarView, onViewSelected: (CalendarView) -> Unit) {

    val viewsToShow = listOf(CalendarView.DAY, CalendarView.WEEK) // Only show Day and Week

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // .padding(horizontal = 16.dp) // Keep horizontal padding for centering
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50)),
        horizontalArrangement = Arrangement.Center // Center the buttons horizontally
    ) {
        viewsToShow.forEachIndexed { index, view ->
            val isSelected = view == selectedView

            // Determine shape based on position
            val shape = when (index) {
                0 -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp) // First button
                viewsToShow.lastIndex -> RoundedCornerShape(
                    topEnd = 50.dp,
                    bottomEnd = 50.dp
                ) // Last button
                else -> RoundedCornerShape(0.dp) // Middle buttons (if any)
            }

            TextButton(
                onClick = { onViewSelected(view) },
                modifier = Modifier
                    .weight(1f) // Distribute width equally
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                shape = shape,
                contentPadding = PaddingValues(vertical = 8.dp) // Consistent vertical padding
            ) {
                Text(
                    text = view.name.replaceFirstChar { it.uppercase() },
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@Composable
fun GeneratedPlanReviewView(
    viewType: CalendarView,
    plan: Map<LocalDate, List<ScheduledTaskItem>>,
    startDate: LocalDate,
    onTaskClick: (Task) -> Unit,
    tasksFlaggedForManualEdit: Set<Int>, // Receive the flags
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        when (viewType) {
            CalendarView.WEEK -> {
                val weekStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                ReviewWeeklyViewContent(
                    startDate = weekStart,
                    plan = plan,
                    onTaskClick = onTaskClick,
                    tasksFlaggedForManualEdit = tasksFlaggedForManualEdit // Pass down
                )
            }

            CalendarView.DAY -> {
                ReviewDailyViewContent(
                    date = startDate,
                    items = plan[startDate] ?: emptyList(),
                    onTaskClick = onTaskClick,
                    tasksFlaggedForManualEdit = tasksFlaggedForManualEdit // Pass down
                )
            }

            CalendarView.MONTH -> {
                // Display a placeholder or simple list for month view in review
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Monthly view preview not available.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("Switch to Day or Week view.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ReviewDailyViewContent(
    date: LocalDate,
    items: List<ScheduledTaskItem>,
    onTaskClick: (Task) -> Unit,
    tasksFlaggedForManualEdit: Set<Int>, // Receive the flags
    modifier: Modifier = Modifier,
) {
    val hourHeight = 60.dp
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val totalHeight = hourHeight * 24
    val timeLabelWidth: Dp = 48.dp // Increased width for time labels
    val scrollState = rememberScrollState()
    val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val color1 = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    Box(
        modifier = modifier
            .heightIn(max = totalHeight) // Constrain height but allow scrolling if needed
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        // Background Grid Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val labelWidthPx = with(density) { timeLabelWidth.toPx() }
            val gridColor = color
            val gridColorHalf = color1

            // Draw horizontal hour lines
            for (hour in 0..23) {
                val y = hour * hourHeightPx
                drawLine(
                    color = gridColor,
                    start = Offset(labelWidthPx, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                // Draw half-hour lines
                val halfY = y + hourHeightPx / 2
                drawLine(
                    color = gridColorHalf,
                    start = Offset(labelWidthPx, halfY),
                    end = Offset(size.width, halfY),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
            }
            // Draw vertical line separating labels and grid
            drawLine(
                color = gridColor,
                start = Offset(labelWidthPx, 0f),
                end = Offset(labelWidthPx, size.height),
                strokeWidth = 1f
            )
        }

        // Time Labels Column
        Column(
            modifier = Modifier
                .fillMaxHeight() // Takes full grid height
                .width(timeLabelWidth)
                .padding(end = 4.dp) // Padding from the grid line
        ) {
            for (hour in 0..23) {
                val timeString = when (hour) {
                    0 -> "12AM"; 12 -> "12PM"; in 1..11 -> "${hour}AM"; else -> "${hour - 12}PM"
                }
                Box(
                    modifier = Modifier.height(hourHeight), // Height of one hour
                    contentAlignment = Alignment.TopCenter // Align text to the top
                ) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp) // Padding from the top line
                    )
                }
            }
        }

        // Task Items Container
        Box(
            modifier = Modifier
                .fillMaxSize() // Fill the parent Box
                .padding(start = timeLabelWidth) // Offset start past the labels
        ) {
            items.sortedBy { it.scheduledStartTime }.forEach { item ->
                val startMinutes =
                    item.scheduledStartTime.hour * 60 + item.scheduledStartTime.minute
                val endMinutes = item.scheduledEndTime.hour * 60 + item.scheduledEndTime.minute
                // Ensure minimum duration for calculation, e.g., 15 minutes
                val durationMinutes = (endMinutes - startMinutes).coerceAtLeast(15)

                val topOffset = (startMinutes / 60f) * hourHeight
                val itemHeight = (durationMinutes / 60f) * hourHeight

                ReviewTaskCard(
                    task = item.task,
                    startTime = item.scheduledStartTime,
                    endTime = item.scheduledEndTime,
                    isFlaggedForManualEdit = tasksFlaggedForManualEdit.contains(item.task.id), // Pass flag
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 0.5.dp) // Minimal padding
                        .height(itemHeight.coerceAtLeast(24.dp)) // Minimum height
                        .offset(y = topOffset), // Position based on start time
                    onClick = { onTaskClick(item.task) }
                )
            }
        }
    }
}


@Composable
fun ReviewWeeklyViewContent(
    startDate: LocalDate,
    plan: Map<LocalDate, List<ScheduledTaskItem>>,
    onTaskClick: (Task) -> Unit,
    tasksFlaggedForManualEdit: Set<Int>, // Accept flagged IDs
    modifier: Modifier = Modifier,
) {
    val weekDays = remember(startDate) {
        (0..6).map { startDate.plusDays(it.toLong()) }
    }
    val hourHeight: Dp = 60.dp // Height for one hour slot
    val density = LocalDensity.current
    val hourHeightPx = remember(hourHeight, density) { with(density) { hourHeight.toPx() } }
    val timeLabelWidth: Dp = 48.dp // Width for the time labels on the left
    val totalGridHeight = remember(hourHeight) { hourHeight * 24 } // Total height for 24 hours
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp // Get screen width

    // Calculate width available for the day columns after accounting for time labels
    val availableWidthForDays = (screenWidthDp - timeLabelWidth)
    val dayWidth = remember(availableWidthForDays, weekDays) {
        // Ensure weekDays is not empty before dividing
        if (weekDays.isNotEmpty()) (availableWidthForDays / weekDays.size).coerceAtLeast(40.dp)
        else availableWidthForDays // Fallback if weekDays is empty
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header Row for Day Names and Dates
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(start = timeLabelWidth) // Offset to align with grid content
        ) {
            weekDays.forEach { day ->
                Column(
                    modifier = Modifier
                        .width(dayWidth) // Assign calculated width
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isToday = day == LocalDate.now()
                    Text(
                        day.format(DateTimeFormatter.ofPattern("E")), // Day abbreviation (Mon, Tue)
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        day.dayOfMonth.toString(), // Day number
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        // Scrollable Grid Area
        Box(
            modifier = Modifier
                .heightIn(max = 500.dp) // Limit height and make scrollable if content exceeds
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {

            val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            val color1 = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            Box(
                modifier = Modifier
                    .height(totalGridHeight) // Set fixed height for 24 hours
                    .fillMaxWidth()
            ) {
                // Background Grid Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val labelWidthPx = with(density) { timeLabelWidth.toPx() }
                    val dayWidthPx = with(density) { dayWidth.toPx() }
                    val gridColor = color
                    val gridColorHalf = color1

                    // Draw horizontal hour lines
                    for (hour in 0..23) {
                        val y = hour * hourHeightPx
                        drawLine(
                            color = gridColor,
                            start = Offset(labelWidthPx, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        // Draw half-hour lines
                        val halfY = y + hourHeightPx / 2
                        drawLine(
                            color = gridColorHalf,
                            start = Offset(labelWidthPx, halfY),
                            end = Offset(size.width, halfY),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )
                    }

                    // Draw vertical lines separating days
                    for (i in 0..weekDays.size) { // Draw lines including the last one
                        val x = labelWidthPx + i * dayWidthPx
                        drawLine(
                            color = gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                    }
                }

                // Time Labels Column (on the left)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(timeLabelWidth)
                        .padding(end = 4.dp) // Padding from the grid line
                ) {
                    for (hour in 0..23) {
                        val timeString = when (hour) {
                            0 -> "12AM"; 12 -> "12PM"; in 1..11 -> "${hour}AM"; else -> "${hour - 12}PM"
                        }
                        Box(
                            modifier = Modifier.height(hourHeight), // Height of one hour
                            contentAlignment = Alignment.TopCenter // Align text to the top
                        ) {
                            Text(
                                text = timeString,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp) // Padding from the top line
                            )
                        }
                    }
                }

                // Task Rendering Area (Row containing columns for each day)
                Row(
                    modifier = Modifier
                        .fillMaxSize() // Fill the parent Box
                        .padding(start = timeLabelWidth) // Offset start past the labels
                ) {
                    weekDays.forEach { date ->
                        // Column for tasks for a specific day
                        Box(
                            modifier = Modifier
                                .width(dayWidth) // Assign calculated width
                                .fillMaxHeight()
                        ) {
                            // Iterate through tasks scheduled for this date
                            plan[date]?.sortedBy { it.scheduledStartTime }?.forEach { item ->
                                // Calculate vertical position and height based on time
                                val startMinutes =
                                    item.scheduledStartTime.hour * 60 + item.scheduledStartTime.minute
                                val endMinutes =
                                    item.scheduledEndTime.hour * 60 + item.scheduledEndTime.minute
                                // Ensure minimum duration for calculation, e.g., 15 minutes
                                val durationMinutes = (endMinutes - startMinutes).coerceAtLeast(15)

                                val topOffset = (startMinutes / 60f) * hourHeight
                                val itemHeight = (durationMinutes / 60f) * hourHeight

                                // Render the task card
                                ReviewTaskCard(
                                    task = item.task,
                                    startTime = item.scheduledStartTime,
                                    endTime = item.scheduledEndTime,
                                    isFlaggedForManualEdit = tasksFlaggedForManualEdit.contains(item.task.id), // Pass flag
                                    modifier = Modifier
                                        .fillMaxWidth() // Fill the width of the day column
                                        .padding(horizontal = 1.dp) // Minimal horizontal padding
                                        .height(itemHeight.coerceAtLeast(24.dp)) // Minimum height
                                        .offset(y = topOffset), // Position based on start time
                                    onClick = { onTaskClick(item.task) } // Click handler
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConflictResolutionCard(
    conflict: ConflictItem,
    options: List<ResolutionOption>,
    selectedOption: ResolutionOption?,
    onOptionSelected: (ResolutionOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("MMM d, HH:mm") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface, // Use regular surface color
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        ) // Keep error border
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), // Adjusted padding
            verticalArrangement = Arrangement.spacedBy(6.dp) // Consistent spacing
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    "Conflict",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Conflict: ${conflict.reason}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    conflict.conflictTime?.let { // Check if conflictTime exists
                        Text(
                            "At: ${it.format(timeFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(start = 28.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "Involved Tasks:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                conflict.conflictingTasks.forEach { task ->
                    Text(
                        "- ${task.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            selectedOption?.toDisplayString() ?: "Select Action",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.toDisplayString()) },
                                onClick = { onOptionSelected(option); expanded = false },
                                trailingIcon = if (option == selectedOption) {
                                    { Icon(Icons.Filled.Check, "Selected", Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ResolutionCard(
    task: Task,
    options: List<ResolutionOption>,
    selectedOption: ResolutionOption?,
    onOptionSelected: (ResolutionOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface, // Use regular surface
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        ) // Keep error border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                val expirationText = task.endDateConf?.dateTime?.format(dateFormatter)
                    ?: task.startDateConf?.dateTime?.format(dateFormatter) ?: "Unknown Date"
                Text(
                    "Expired: $expirationText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        selectedOption?.toDisplayString() ?: "Select Action",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        null,
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.toDisplayString()) },
                            onClick = { onOptionSelected(option); expanded = false },
                            trailingIcon = if (option == selectedOption) {
                                { Icon(Icons.Filled.Check, "Selected", Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewTaskCard(
    task: Task,
    startTime: LocalTime,
    endTime: LocalTime,
    isFlaggedForManualEdit: Boolean, // Receive flag
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    // Determine colors based on state
    val priorityColor = when (task.priority) {
        Priority.HIGH -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f) // Assuming tertiary is orange-ish
        Priority.LOW -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) // Using primary for low
        Priority.NONE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    val cardColor = when {
        isFlaggedForManualEdit -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f) // Highlight flagged
        task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        isFlaggedForManualEdit -> MaterialTheme.colorScheme.tertiary // Stronger border for flagged
        else -> priorityColor.copy(alpha = 0.5f)
    }
    val textColor = when {
        isFlaggedForManualEdit -> MaterialTheme.colorScheme.onTertiaryContainer
        task.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderStroke =
        BorderStroke(width = if (isFlaggedForManualEdit) 1.5.dp else 1.dp, color = borderColor)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(cardColor)
            .border(border = borderStroke, shape = RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 4.dp, top = 3.dp, bottom = 3.dp) // Adjusted padding
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            // Priority Indicator
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(color = priorityColor, shape = RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(5.dp))
            // Task Details
            Column(
                modifier = Modifier.weight(1f), // Takes available space
                verticalArrangement = Arrangement.spacedBy(1.dp) // Minimal space between lines
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.name,
                        style = MaterialTheme.typography.labelMedium, // Slightly smaller for compactness
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false) // Don't force fill if short
                    )
                    // Show Edit icon if flagged
                    if (isFlaggedForManualEdit) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Required",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                // Time Text
                Text(
                    "${startTime.format(timeFormatter)} - ${endTime.format(timeFormatter)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
            // Completion Check (only if not flagged for edit)
            if (task.isCompleted && !isFlaggedForManualEdit) {
                Icon(
                    Icons.Default.Check,
                    "Completed",
                    modifier = Modifier
                        .size(14.dp)
                        .padding(start = 2.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun PrioritizationStrategy.toDisplayString(): String = when (this) {
    PrioritizationStrategy.URGENT_FIRST -> "Urgent Tasks First"
    PrioritizationStrategy.HIGH_PRIORITY_FIRST -> "Highest Priority First"
    PrioritizationStrategy.SHORT_TASKS_FIRST -> "Shortest Tasks First"
    PrioritizationStrategy.EARLIER_DEADLINES_FIRST -> "Earliest Deadlines First"
}

fun DayOrganization.toDisplayString(): String = when (this) {
    DayOrganization.MAXIMIZE_PRODUCTIVITY -> "Maximize productivity (tight schedule)"
    DayOrganization.FOCUS_URGENT_BUFFER -> "Focus on urgent (add buffers)"
    DayOrganization.LOOSE_SCHEDULE_BREAKS -> "Relaxed schedule (add breaks)"
}

fun OverdueTaskHandling.toDisplayString(): String = when (this) {
    OverdueTaskHandling.ADD_TODAY_FREE_TIME -> "Schedule automatically"
    OverdueTaskHandling.MANAGE_WHEN_FREE -> "Schedule manually later"
    OverdueTaskHandling.POSTPONE_TO_TOMORROW -> "Postpone to tomorrow"
}

fun ScheduleScope.toDisplayString(): String =
    this.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

fun ResolutionOption.toDisplayString(): String = when (this) {
    ResolutionOption.MOVE_TO_NEAREST_FREE -> "Move Auto" // Kept short
    ResolutionOption.MOVE_TO_TOMORROW -> "Move Tomorrow"
    ResolutionOption.MANUALLY_SCHEDULE -> "Schedule Manually"
    ResolutionOption.LEAVE_IT_LIKE_THAT -> "Leave As Is"
    ResolutionOption.RESOLVED -> "Resolved" // Internal/Display state
}