package com.elena.autoplanner.presentation.ui.screens.tasks.planner

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.times
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
import com.elena.autoplanner.presentation.intents.PlannerIntent
import com.elena.autoplanner.presentation.states.PlannerState
import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarView
import com.elena.autoplanner.presentation.ui.screens.tasks.TaskDetailSheet
import com.elena.autoplanner.presentation.viewmodel.PlannerViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskDetailViewModel
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
    var selectedTaskIdForDetail by remember { mutableStateOf<Int?>(null) }

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
                )
                .fillMaxSize()
        ) {
            state?.let { currentState ->
                PlannerContent(
                    state = currentState,
                    onIntent = viewModel::sendIntent,
                    onStartTimeClick = { if (startTimeState != null) showStartTimePicker = true },
                    onEndTimeClick = { if (endTimeState != null) showEndTimePicker = true },
                    onReviewTaskClick = { task -> selectedTaskIdForDetail = task.id }

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
        selectedTaskIdForDetail?.let { taskId ->

            val detailViewModel: TaskDetailViewModel =
                koinViewModel(parameters = { parametersOf(taskId) })


            LaunchedEffect(detailViewModel, taskId) {
                detailViewModel.effect.collect { effect ->
                    when (effect) {
                        is com.elena.autoplanner.presentation.effects.TaskDetailEffect.NavigateBack -> {
                            selectedTaskIdForDetail = null
                        }

                        else -> {}
                    }
                }
            }
            TaskDetailSheet(
                taskId = taskId,
                onDismiss = { selectedTaskIdForDetail = null },
                viewModel = detailViewModel
            )
        }
    }
}

@Composable
fun PlannerContent(
    state: PlannerState,
    onIntent: (PlannerIntent) -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onReviewTaskClick: (Task) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Let's create your optimized schedule!",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp),
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
            PlannerStep.REVIEW_PLAN -> Step4ReviewPlan(state, onIntent, onReviewTaskClick)
        }

        if (state.error != null) {
            Text(
                "Error: ${state.error}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun Step1TimeInput(
    state: PlannerState,
    onIntent: (PlannerIntent) -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    selectedOption = state.selectedPriority,
                    onOptionSelected = { priority -> onIntent(PlannerIntent.SelectPriority(priority)) },
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    selectedOption = state.showSubtasksWithDuration,
                    onOptionSelected = { show -> onIntent(PlannerIntent.SelectShowSubtasks(show)) },
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
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
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
    onReviewTaskClick: (Task) -> Unit, // Callback for when a task in the review view is clicked
) {
    var currentCalendarView by remember { mutableStateOf(CalendarView.WEEK) } // Default to week view

    // Determine the start date for the calendar view based on the plan or scope
    val planStartDate = remember(state.generatedPlan, state.scheduleScope) {
        state.generatedPlan.keys.minOrNull() ?: state.scheduleScope?.let { scope ->
            val today = LocalDate.now()
            when (scope) {
                ScheduleScope.TODAY -> today
                ScheduleScope.TOMORROW -> today.plusDays(1)
                ScheduleScope.THIS_WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            }
        } ?: LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) // Fallback
    }

    // Filter unresolved items *before* rendering the UI elements
    // Use remember to avoid recalculating on every recomposition unless dependencies change
    val unresolvedExpired = remember(state.expiredTasksToResolve, state.taskResolutions) {
        state.expiredTasksToResolve.filter { state.taskResolutions[it.id] == null }
    }
    val unresolvedConflicts = remember(state.conflictsToResolve, state.conflictResolutions) {
        state.conflictsToResolve.filter { state.conflictResolutions[it.hashCode()] == null }
    }
    val hasUnresolvedItems = unresolvedExpired.isNotEmpty() || unresolvedConflicts.isNotEmpty()

    // Main layout column for Step 4
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp) // Consistent spacing between sections
    ) {

        // --- Resolution Section (Animated Visibility) ---
        AnimatedVisibility(visible = hasUnresolvedItems) { // Animate the whole section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { // Use Column to group Card and Text
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
                        // Expired Tasks Section (Conditionally Rendered based on filtered list)
                        if (unresolvedExpired.isNotEmpty()) {
                            ReviewSectionHeader(
                                title = "Action Required: Expired Tasks (${unresolvedExpired.size})",
                                isError = true
                            )
                            // Render only unresolved items
                            unresolvedExpired.forEach { task ->
                                ResolutionCard(
                                    task = task,
                                    options = listOf(
                                        ResolutionOption.MOVE_TO_TOMORROW,
                                        ResolutionOption.MANUALLY_SCHEDULE
                                    ),
                                    selectedOption = null, // Selection is handled by state update, no need to show here
                                    onOptionSelected = { option ->
                                        onIntent(
                                            PlannerIntent.ResolveExpiredTask(
                                                task,
                                                option
                                            )
                                        )
                                    }
                                )
                            }
                        }

                        // Conflicts Section (Conditionally Rendered based on filtered list)
                        if (unresolvedConflicts.isNotEmpty()) {
                            if (unresolvedExpired.isNotEmpty()) Spacer(modifier = Modifier.height(0.dp)) // No extra space needed if both sections are showing within the same card
                            ReviewSectionHeader(
                                title = "Action Required: Scheduling Conflicts (${unresolvedConflicts.size})",
                                isError = true
                            )
                            // Render only unresolved items
                            unresolvedConflicts.forEach { conflict ->
                                ConflictResolutionCard(
                                    conflict = conflict,
                                    options = listOf(
                                        ResolutionOption.MOVE_TO_TOMORROW,
                                        ResolutionOption.MANUALLY_SCHEDULE,
                                        ResolutionOption.LEAVE_IT_LIKE_THAT
                                    ),
                                    selectedOption = null, // Selection handled by state update
                                    onOptionSelected = { option ->
                                        onIntent(
                                            PlannerIntent.ResolveConflict(
                                                conflict,
                                                option
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                // Text below the card, shown only if there are unresolved items
                Text(
                    "Please resolve the items above to add the plan",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(top = 4.dp) // Add some space above the text
                        .align(Alignment.CenterHorizontally)
                )
            }
        } // End of AnimatedVisibility for resolution section

        // --- Generated Plan Preview Section ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Generated Plan Preview",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // View Toggle Buttons
                ViewToggleButtons(
                    selectedView = currentCalendarView,
                    onViewSelected = { currentCalendarView = it }
                )
                Spacer(Modifier.height(12.dp))

                // Container for the actual calendar view preview
                Box(
                    modifier = Modifier
                        .heightIn(min = 250.dp, max = 550.dp) // Adjusted height constraints
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        ) // Add a subtle border
                        .clip(RoundedCornerShape(8.dp)) // Clip the content
                ) {
                    if (state.generatedPlan.isNotEmpty()) {
                        GeneratedPlanReviewView(
                            viewType = currentCalendarView,
                            plan = state.generatedPlan,
                            startDate = planStartDate,
                            onTaskClick = onReviewTaskClick // Pass the click handler down
                        )
                    } else if (!state.isLoading) {
                        // Message when the plan is empty (and not loading)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No tasks were scheduled based on the current criteria.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    // Loading indicator specifically for plan review generation (if needed)
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

    val viewsToShow = listOf(CalendarView.DAY, CalendarView.WEEK)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50)),
        horizontalArrangement = Arrangement.Center
    ) {
        viewsToShow.forEach { view ->
            val isSelected = view == selectedView

            val shape = when (view) {
                viewsToShow.first() -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                viewsToShow.last() -> RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp)
                else -> RoundedCornerShape(0.dp)
            }

            TextButton(
                onClick = { onViewSelected(view) },
                modifier = Modifier
                    .weight(1f)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                shape = shape,
                contentPadding = PaddingValues(vertical = 8.dp)
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
fun GeneratedPlanCalendarView(
    viewType: CalendarView,
    plan: Map<LocalDate, List<ScheduledTaskItem>>,
    startDate: LocalDate,
) {
    when (viewType) {
        CalendarView.WEEK -> {
            val weekStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekEnd = weekStart.plusDays(6)
            val weekPlan = plan.filterKeys { it in weekStart..weekEnd }
            ReviewWeeklyPlanView(startDate = weekStart, plan = weekPlan)
        }

        CalendarView.DAY -> {
            ReviewDailyPlanView(
                date = startDate,
                items = plan[startDate] ?: emptyList()
            )
        }

        CalendarView.MONTH -> {


            ReviewWeeklyPlanView(
                startDate = startDate.withDayOfMonth(1)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), plan = plan
            )

        }
    }
}

@Composable
fun ReviewDailyPlanView(date: LocalDate, items: List<ScheduledTaskItem>) {
    val hourHeight = 60.dp
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val totalHeight = hourHeight * 24

    Box(
        modifier = Modifier
            .height(totalHeight)
            .fillMaxWidth()
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val hourLabelWidth = with(density) { 40.dp.toPx() }

            for (hour in 0..23) {
                val y = hour * hourHeightPx
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(hourLabelWidth, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )

                val halfY = y + hourHeightPx / 2
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.15f),
                    start = Offset(hourLabelWidth, halfY),
                    end = Offset(size.width, halfY),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
            }
        }


        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp)
        ) {
            for (hour in 0..23) {
                Box(
                    modifier = Modifier
                        .height(hourHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = String.format("%02d:00", hour),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }



        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp)
        ) {
            items.sortedBy { it.scheduledStartTime }.forEach { item ->
                val startMinutes =
                    item.scheduledStartTime.hour * 60 + item.scheduledStartTime.minute
                val endMinutes = item.scheduledEndTime.hour * 60 + item.scheduledEndTime.minute
                val durationMinutes =
                    (endMinutes - startMinutes).coerceAtLeast(15)

                val topOffset = (startMinutes / 60f) * hourHeight
                val itemHeight = (durationMinutes / 60f) * hourHeight

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                        .height(itemHeight)
                        .offset(y = topOffset),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.task.isCompleted) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .fillMaxSize(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = item.task.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${item.scheduledStartTime.format(timeFormatter)} - ${
                                    item.scheduledEndTime.format(
                                        timeFormatter
                                    )
                                }",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        if (item.task.isCompleted) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Completed",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ReviewWeeklyPlanView(startDate: LocalDate, plan: Map<LocalDate, List<ScheduledTaskItem>>) {
    val weekDays = remember(startDate) {
        (0..6).map { startDate.plusDays(it.toLong()) }
    }
    val dayWidth =
        LocalConfiguration.current.screenWidthDp.dp / 8

    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp, start = 40.dp)
        ) {
            weekDays.forEach { day ->
                Column(
                    modifier = Modifier.width(dayWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        day.format(DateTimeFormatter.ofPattern("E")),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        HorizontalDivider()

        Box(
            modifier = Modifier
                .height(400.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {


            Column(
                    modifier = Modifier
                        .width(40.dp)
                        .fillMaxHeight()
                ) {
                    for (hour in 0..23) {
                        Box(
                            modifier = Modifier
                                .height(60.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = String.format("%02d:00", hour),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                weekDays.forEach { date ->
                    Box(
                        modifier = Modifier
                            .width(dayWidth)
                            .fillMaxHeight()
                    ) {
                        ReviewDailyPlanView(date = date, items = plan[date] ?: emptyList())
                    }
                }
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
fun ConflictResolutionCard(
    conflict: ConflictItem,
    options: List<ResolutionOption>,
    selectedOption: ResolutionOption?,
    onOptionSelected: (ResolutionOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val timeFormatter =
        remember { DateTimeFormatter.ofPattern("MMM d, HH:mm") } // Format for conflict time

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 10.dp
            )
        ) { // Increased vertical padding
            // Conflict Header
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
                        style = MaterialTheme.typography.bodyLarge, // Larger title
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Show Conflict Time if available
                    conflict.conflictTime?.let {
                        Text(
                            "At: ${it.format(timeFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error, // Use error color for time too
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp)) // Increased spacing

            // Conflicting Tasks List
            Column(
                modifier = Modifier.padding(start = 28.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) { // Add spacing
                Text(
                    "Involved Tasks:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                conflict.conflictingTasks.forEach { task ->
                    Text(
                        "- ${task.name}",
                        style = MaterialTheme.typography.bodyMedium, // Slightly larger task name
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(10.dp)) // Increased spacing

            // Resolution Button
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
    val dateFormatter =
        remember { DateTimeFormatter.ofPattern("MMM d, yyyy") } // More specific format

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp), // Increased vertical padding
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
                    ?: task.startDateConf?.dateTime?.format(dateFormatter)
                    ?: "Unknown Date"
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
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    ), // Adjusted padding
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val priorityColor = when (task.priority) {
        Priority.HIGH -> Color.Red.copy(alpha = 0.7f)
        Priority.MEDIUM -> Color(0xFFFFA500).copy(alpha = 0.7f)
        Priority.LOW -> Color(0xFF4CAF50).copy(alpha = 0.7f)
        Priority.NONE -> Color.Gray.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = priorityColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(color = priorityColor, shape = RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(5.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    task.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${startTime.format(timeFormatter)}-${endTime.format(timeFormatter)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (task.isCompleted) {
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

@Composable
fun ReviewWeeklyViewContent(
    startDate: LocalDate,
    plan: Map<LocalDate, List<ScheduledTaskItem>>,
    onTaskClick: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {

    val weekDays = remember(startDate) {
        (0..6).map { startDate.plusDays(it.toLong()) }
    }

    val hourHeight: Dp = 60.dp
    val density = LocalDensity.current
    val hourHeightPx = remember(hourHeight, density) { with(density) { hourHeight.toPx() } }
    val timeLabelWidth: Dp = 48.dp
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp

    val availableWidthForDays = (screenWidthDp - timeLabelWidth)

    val dayWidth = remember(availableWidthForDays, weekDays) {
        if (weekDays.isNotEmpty()) (availableWidthForDays / weekDays.size).coerceAtLeast(40.dp)
        else availableWidthForDays
    }

    val totalGridHeight = remember(hourHeight) { hourHeight * 24 }

    Column(modifier = modifier.fillMaxWidth()) {


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(start = timeLabelWidth)
        ) {
            weekDays.forEach { day ->
                Column(
                    modifier = Modifier
                        .width(dayWidth)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        day.format(DateTimeFormatter.ofPattern("E")),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (day == LocalDate.now()) FontWeight.Bold else FontWeight.Normal
                    )

                    Text(
                        day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (day == LocalDate.now()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (day == LocalDate.now()) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))


        Box(
            modifier = Modifier
                .heightIn(max = 500.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {

            Box(
                modifier = Modifier
                    .height(totalGridHeight)
                    .fillMaxWidth()
            ) {
                val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                val color2 = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val labelWidthPx = with(density) { timeLabelWidth.toPx() }
                    val dayWidthPx = with(density) { dayWidth.toPx() }

                    for (hour in 0..23) {
                        val y = hour * hourHeightPx
                        drawLine(
                            color = color,
                            start = Offset(labelWidthPx, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )

                        val halfY = y + hourHeightPx / 2
                        drawLine(
                            color = color2,
                            start = Offset(labelWidthPx, halfY),
                            end = Offset(size.width, halfY),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )
                    }


                    for (i in 0..weekDays.size) {
                        val x = labelWidthPx + i * dayWidthPx
                        drawLine(
                            color = color,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                    }
                }


                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(timeLabelWidth)
                        .padding(end = 4.dp)
                ) {
                    for (hour in 0..23) {
                        val timeString = when (hour) {
                            0 -> "12AM"; 12 -> "12PM"; in 1..11 -> "${hour}AM"; else -> "${hour - 12}PM"
                        }
                        Box(
                            modifier = Modifier
                                .height(hourHeight)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = timeString,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }


                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = timeLabelWidth)
                ) {
                    weekDays.forEach { date ->

                        Box(
                            modifier = Modifier
                                .width(dayWidth)
                                .fillMaxHeight()
                        ) {

                            plan[date]?.sortedBy { it.scheduledStartTime }?.forEach { item ->

                                val startMinutes =
                                    item.scheduledStartTime.hour * 60 + item.scheduledStartTime.minute
                                val endMinutes =
                                    item.scheduledEndTime.hour * 60 + item.scheduledEndTime.minute
                                val durationMinutes =
                                    (endMinutes - startMinutes).coerceAtLeast(15)
                                val topOffset = (startMinutes / 60f) * hourHeight
                                val itemHeight = (durationMinutes / 60f) * hourHeight


                                ReviewTaskCard(
                                    task = item.task,
                                    startTime = item.scheduledStartTime,
                                    endTime = item.scheduledEndTime,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 1.dp)
                                        .height(itemHeight.coerceAtLeast(24.dp))
                                        .offset(y = topOffset),
                                    onClick = { onTaskClick(item.task) }
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
fun ReviewDailyViewContent(
    date: LocalDate,
    items: List<ScheduledTaskItem>,
    onTaskClick: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hourHeight: Dp = 60.dp
    val density = LocalDensity.current
    val hourHeightPx = remember(hourHeight, density) { with(density) { hourHeight.toPx() } }
    val totalHeight =
        remember(hourHeight) { hourHeight * 24 }
    val timeLabelWidth: Dp = 48.dp
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .height(totalHeight)
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        val color2 = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        Box(
            modifier = Modifier
                .height(totalHeight)
                .fillMaxWidth()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val labelWidthPx = with(density) { timeLabelWidth.toPx() }

                for (hour in 0..23) {
                    val y = hour * hourHeightPx
                    drawLine(
                        color = color,
                        start = Offset(labelWidthPx, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )

                    val halfY = y + hourHeightPx / 2
                    drawLine(
                        color = color2,
                        start = Offset(labelWidthPx, halfY),
                        end = Offset(size.width, halfY),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(
                                4f,
                                4f
                            )
                        )
                    )
                }

                drawLine(
                    color = color,
                    start = Offset(labelWidthPx, 0f),
                    end = Offset(labelWidthPx, size.height),
                    strokeWidth = 1f
                )
            }


            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(timeLabelWidth)
                    .padding(end = 4.dp)
            ) {
                for (hour in 0..23) {

                    val timeString = when (hour) {
                        0 -> "12AM"
                        12 -> "12PM"
                        in 1..11 -> "${hour}AM"
                        else -> "${hour - 12}PM"
                    }
                    Box(
                        modifier = Modifier
                            .height(hourHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }


            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = timeLabelWidth)
            ) {

                items.sortedBy { it.scheduledStartTime }.forEach { item ->

                    val startMinutes =
                        item.scheduledStartTime.hour * 60 + item.scheduledStartTime.minute
                    val endMinutes = item.scheduledEndTime.hour * 60 + item.scheduledEndTime.minute

                    val durationMinutes = (endMinutes - startMinutes).coerceAtLeast(15)


                    val topOffset = (startMinutes / 60f) * hourHeight
                    val itemHeight = (durationMinutes / 60f) * hourHeight


                    ReviewTaskCard(
                        task = item.task,
                        startTime = item.scheduledStartTime,
                        endTime = item.scheduledEndTime,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 2.dp,
                                vertical = 0.5.dp
                            )
                            .height(itemHeight.coerceAtLeast(24.dp))
                            .offset(y = topOffset),
                        onClick = { onTaskClick(item.task) }
                    )
                }
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
                    onTaskClick = onTaskClick
                )
            }

            CalendarView.DAY -> {
                ReviewDailyViewContent(
                    date = startDate,
                    items = plan[startDate] ?: emptyList(),
                    onTaskClick = onTaskClick
                )
            }

            CalendarView.MONTH -> {
                val weekStart = startDate.withDayOfMonth(1)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                ReviewWeeklyViewContent(
                    startDate = weekStart,
                    plan = plan,
                    onTaskClick = onTaskClick
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
    ResolutionOption.MOVE_TO_NEAREST_FREE -> "Move Auto"
    ResolutionOption.MOVE_TO_TOMORROW -> "Move Tomorrow"
    ResolutionOption.MANUALLY_SCHEDULE -> "Schedule Manually"
    ResolutionOption.LEAVE_IT_LIKE_THAT -> "Leave As Is"
    ResolutionOption.RESOLVED -> "Resolved"
}