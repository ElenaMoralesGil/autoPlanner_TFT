package com.elena.autoplanner.presentation.ui.screens.tasks.planner

import java.time.DayOfWeek
import android.util.Log
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.presentation.effects.PlannerEffect
import com.elena.autoplanner.presentation.intents.PlannerIntent
import com.elena.autoplanner.presentation.states.PlannerState
import com.elena.autoplanner.presentation.viewmodel.PlannerViewModel
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.zIndex
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.DayOrganization
import com.elena.autoplanner.domain.models.OverdueTaskHandling
import com.elena.autoplanner.domain.models.PlannerStep
import com.elena.autoplanner.domain.models.PrioritizationStrategy
import com.elena.autoplanner.domain.models.ResolutionOption
import com.elena.autoplanner.domain.models.ScheduleScope
import com.elena.autoplanner.domain.models.ScheduledTaskItem
import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarView
import com.elena.autoplanner.presentation.ui.screens.tasks.TaskDetailSheet
import com.elena.autoplanner.presentation.viewmodel.TaskDetailViewModel
import org.koin.core.parameter.parametersOf
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

    // Show Start Time Picker
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

    // Show End Time Picker
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
            // Validation: End time must be after start time
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
                            .padding(horizontal = 16.dp, vertical = 12.dp) // Adjusted padding
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
                        .zIndex(10f) // Ensure overlay is on top
                ) {
                    CircularProgressIndicator(modifier = Modifier.wrapContentSize(Alignment.Center))
                }
            }
        }
        selectedTaskIdForDetail?.let { taskId ->
            // Use Koin to get the ViewModel scoped to this task ID
            val detailViewModel: TaskDetailViewModel =
                koinViewModel(parameters = { parametersOf(taskId) })

            // Handle effects from the detail ViewModel (like closing the sheet)
            LaunchedEffect(detailViewModel, taskId) { // Re-launch if taskId changes
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
            .fillMaxSize() // Takes space above bottom bar
            .verticalScroll(rememberScrollState())
            .padding(16.dp), // Padding for content area
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp) // Increased spacing between cards
    ) {
        Text(
            text = "Let's create your optimized schedule!",
            style = MaterialTheme.typography.titleLarge, // Larger title
            modifier = Modifier.padding(bottom = 12.dp), // Adjust padding
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center // Center align title
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
    ) { // Adjust padding
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
                    onOptionSelected = { priority -> onIntent(PlannerIntent.SelectPriority(priority)) }, // **** Use SelectPriority ****
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
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium, // Use TitleMedium for sections
        fontWeight = FontWeight.Bold,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 4.dp) // Consistent padding
    )
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

        // --- Resolution Section (Only shown if there are unresolved items) ---
        if (hasUnresolvedItems) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                        alpha = 0.4f
                    )
                ), // Slightly more opaque
                elevation = CardDefaults.cardElevation(0.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                ) // Slightly stronger border
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 16.dp
                    ), // Adjusted padding
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Space between headers and cards
                ) {
                    // Expired Tasks Section
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
                                    // ResolutionOption.LEAVE_IT_LIKE_THAT // Maybe less relevant for expired?
                                ),
                                selectedOption = state.taskResolutions[task.id], // Show current selection if any
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

                    // Conflicts Section
                    if (unresolvedConflicts.isNotEmpty()) {
                        // Add spacer only if both expired and conflicts are present
                        if (unresolvedExpired.isNotEmpty()) Spacer(modifier = Modifier.height(12.dp))

                        ReviewSectionHeader(
                            title = "Action Required: Scheduling Conflicts (${unresolvedConflicts.size})",
                            isError = true
                        )
                        unresolvedConflicts.forEach { conflict -> // Use the filtered list
                            ConflictResolutionCard(
                                conflict = conflict,
                                options = listOf(
                                    ResolutionOption.MOVE_TO_TOMORROW, // Offer simple resolution
                                    ResolutionOption.MANUALLY_SCHEDULE,
                                    ResolutionOption.LEAVE_IT_LIKE_THAT
                                ),
                                selectedOption = state.conflictResolutions[conflict.hashCode()], // Show current selection
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

            // Message shown only when resolutions are actually needed
            Text(
                "Please resolve the items above to add the plan",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 4.dp) // Reduced top padding
                    .align(Alignment.CenterHorizontally)
            )

        } // End of conditional resolution section

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

                    if (state.isLoading) {
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
            // Corrected enabled logic
            enabled = when (state.currentStep) {
                PlannerStep.TIME_INPUT -> state.canMoveToStep2
                PlannerStep.PRIORITY_INPUT -> state.canMoveToStep3
                PlannerStep.ADDITIONAL_OPTIONS -> state.canGeneratePlan
                PlannerStep.REVIEW_PLAN -> !state.requiresResolution && !state.isLoading // Enable only if no resolutions needed and not loading
            } && !state.isLoading, // Always disable if loading
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
    // Only show Day and Week for review, Month is too complex/less useful here
    val viewsToShow = listOf(CalendarView.DAY, CalendarView.WEEK)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // Add padding for better spacing
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50)), // Clip the row for rounded corners effect
        horizontalArrangement = Arrangement.Center
    ) {
        viewsToShow.forEach { view ->
            val isSelected = view == selectedView
            // Determine shape based on position
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
                shape = shape, // Apply specific shape
                contentPadding = PaddingValues(vertical = 8.dp) // Adjust padding
            ) {
                Text(
                    text = view.name.replaceFirstChar { it.uppercase() }, // Capitalize
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
    startDate: LocalDate, // Used as reference for Daily/Weekly view start
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
            // Month view for review might be complex visually.
            // Consider showing a list grouped by day instead, or implementing a simplified month grid.
            ReviewWeeklyPlanView(
                startDate = startDate.withDayOfMonth(1)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), plan = plan
            ) // Show first week as fallback
            // Text("Monthly review view not implemented yet.", modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
fun ReviewDailyPlanView(date: LocalDate, items: List<ScheduledTaskItem>) {
    val hourHeight = 60.dp // Standard hour height
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val totalHeight = hourHeight * 24

    Box(
        modifier = Modifier
            .height(totalHeight)
            .fillMaxWidth()
    ) { // Ensure Box has height
        // Background Lines and Time Labels
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hourLabelWidth = with(density) { 40.dp.toPx() }
            // Draw hour lines
            for (hour in 0..23) {
                val y = hour * hourHeightPx
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(hourLabelWidth, y), // Start after label area
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                // Optional: Draw half-hour lines
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

        // Time Labels Column
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
                        modifier = Modifier.padding(top = 2.dp) // Adjust padding slightly
                    )
                }
            }
        }


        // Scheduled Task Items
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp)
        ) { // Offset content to avoid labels
            items.sortedBy { it.scheduledStartTime }.forEach { item ->
                val startMinutes =
                    item.scheduledStartTime.hour * 60 + item.scheduledStartTime.minute
                val endMinutes = item.scheduledEndTime.hour * 60 + item.scheduledEndTime.minute
                val durationMinutes =
                    (endMinutes - startMinutes).coerceAtLeast(15) // Min 15 min height

                val topOffset = (startMinutes / 60f) * hourHeight
                val itemHeight = (durationMinutes / 60f) * hourHeight

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 1.dp) // Small padding around card
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
                    ) { // Align content top
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

// --- ReviewWeeklyPlanView (NEW) ---
@Composable
fun ReviewWeeklyPlanView(startDate: LocalDate, plan: Map<LocalDate, List<ScheduledTaskItem>>) {
    val weekDays = remember(startDate) {
        (0..6).map { startDate.plusDays(it.toLong()) }
    }
    val dayWidth =
        LocalConfiguration.current.screenWidthDp.dp / 8 // Approx width per day column + time labels

    Column {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp, start = 40.dp)
        ) { // Offset start for time labels
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
        // Scrollable Content Row
        Box(
            modifier = Modifier
                .height(400.dp)
                .verticalScroll(rememberScrollState())
        ) { // Fixed height and scroll
            Row(modifier = Modifier.fillMaxWidth()) {
                // Combine daily views horizontally
                // Time labels column (rendered once)
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
                // Daily plan views
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
                    onClick = { onOptionSelected(option) }, // Direct selection on click
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant // Use theme color
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = labelSelector(option),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // Use theme color
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

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                alpha = 0.5f
            )
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Conflict",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    // Make conflict reason more prominent
                    "Conflict: ${conflict.reason}",
                    style = MaterialTheme.typography.bodyMedium, // Slightly larger
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(Modifier.height(4.dp))
            // Indent conflicting task list
            Column(modifier = Modifier.padding(start = 28.dp)) {
                conflict.conflictingTasks.forEach { task ->
                    Text(
                        "- ${task.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f), // Readable on error container
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Align dropdown to the end
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text(
                            selectedOption?.toDisplayString() ?: "Resolve",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.toDisplayString()) },
                                onClick = {
                                    onOptionSelected(option)
                                    expanded = false
                                },
                                trailingIcon = if (option == selectedOption) {
                                    {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
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

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                alpha = 0.5f
            )
        ), // Muted error background
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat design
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Task Info on Left
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning, // Use standard warning icon
                    contentDescription = "Expired Task",
                    tint = MaterialTheme.colorScheme.error, // Error color for icon
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer, // Ensure text is readable
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    task.endDateConf?.dateTime?.let { // Use endDateConf for expiration
                        Text(
                            "Expired: ${it.format(DateTimeFormatter.ofPattern("MMM d"))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error // Error color for date
                        )
                    }
                }
            }

            // Dropdown Button on Right
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error
                    ) // Error color border
                ) {
                    Text(
                        selectedOption?.toDisplayString() ?: "Action",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error // Error color text
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error // Error color icon
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.toDisplayString()) },
                            onClick = {
                                onOptionSelected(option)
                                expanded = false
                            },
                            trailingIcon = if (option == selectedOption) {
                                {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
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
    startDate: LocalDate, // Should be the start of the week (e.g., Monday)
    plan: Map<LocalDate, List<ScheduledTaskItem>>,
    onTaskClick: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Calculate the 7 days of the week starting from the provided startDate
    val weekDays = remember(startDate) {
        (0..6).map { startDate.plusDays(it.toLong()) }
    }

    val hourHeight: Dp = 60.dp
    val density = LocalDensity.current
    val hourHeightPx = remember(hourHeight, density) { with(density) { hourHeight.toPx() } }
    val timeLabelWidth: Dp = 48.dp
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    // Calculate width available for the 7 day columns
    val availableWidthForDays = (screenWidthDp - timeLabelWidth)
    // Calculate width per day, ensuring it's positive
    val dayWidth = remember(availableWidthForDays, weekDays) {
        if (weekDays.isNotEmpty()) (availableWidthForDays / weekDays.size).coerceAtLeast(40.dp) // Min width per day
        else availableWidthForDays
    }
    // Total height required for 24 hours
    val totalGridHeight = remember(hourHeight) { hourHeight * 24 }

    Column(modifier = modifier.fillMaxWidth()) {

        // --- Day Headers Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) // Subtle background for headers
                .padding(start = timeLabelWidth) // Align headers with the grid content
        ) {
            weekDays.forEach { day ->
                Column(
                    modifier = Modifier
                        .width(dayWidth) // Use calculated day width
                        .padding(vertical = 6.dp), // Padding for header text
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Display abbreviated day name (e.g., "Mon")
                    Text(
                        day.format(DateTimeFormatter.ofPattern("E")),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (day == LocalDate.now()) FontWeight.Bold else FontWeight.Normal
                    )
                    // Display day number (e.g., "15")
                    Text(
                        day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (day == LocalDate.now()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (day == LocalDate.now()) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        // Separator below headers
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        // --- Scrollable Grid Content ---
        Box(
            modifier = Modifier
                .heightIn(max = 500.dp) // Constrain max height to make it scrollable if needed
                .verticalScroll(rememberScrollState()) // Enable vertical scrolling
                .fillMaxWidth()
        ) {
            // Inner Box defining the full 24-hour height for drawing
            Box(modifier = Modifier
                .height(totalGridHeight)
                .fillMaxWidth()) {
                val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                val color2 = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                // Layer 1: Background Grid Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val labelWidthPx = with(density) { timeLabelWidth.toPx() }
                    val dayWidthPx = with(density) { dayWidth.toPx() }
                    // Draw horizontal hour lines across the day columns
                    for (hour in 0..23) {
                        val y = hour * hourHeightPx
                        drawLine(
                            color = color,
                            start = Offset(labelWidthPx, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        // Optional: Dashed half-hour lines
                        val halfY = y + hourHeightPx / 2
                        drawLine(
                            color = color2,
                            start = Offset(labelWidthPx, halfY),
                            end = Offset(size.width, halfY),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )
                    }

                    // Draw vertical day divider lines
                    for (i in 0..weekDays.size) { // Draw n+1 lines including the start and end
                        val x = labelWidthPx + i * dayWidthPx
                        drawLine(
                            color = color,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height), // Draw full height
                            strokeWidth = 1f
                        )
                    }
                }

                // Layer 2: Time Labels Column (Fixed on the left)
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

                // Layer 3: Task Items distributed horizontally in day columns
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = timeLabelWidth) // Offset the task area
                ) {
                    weekDays.forEach { date ->
                        // Create a Box for each day's column to contain tasks
                        Box(
                            modifier = Modifier
                                .width(dayWidth) // Use calculated day width
                                .fillMaxHeight()
                        ) {
                            // Get tasks for the current date, sort by start time
                            plan[date]?.sortedBy { it.scheduledStartTime }?.forEach { item ->
                                // Calculate vertical position and height
                                val startMinutes =
                                    item.scheduledStartTime.hour * 60 + item.scheduledStartTime.minute
                                val endMinutes =
                                    item.scheduledEndTime.hour * 60 + item.scheduledEndTime.minute
                                val durationMinutes =
                                    (endMinutes - startMinutes).coerceAtLeast(15) // Min visual height
                                val topOffset = (startMinutes / 60f) * hourHeight
                                val itemHeight = (durationMinutes / 60f) * hourHeight

                                // Render the task card
                                ReviewTaskCard(
                                    task = item.task,
                                    startTime = item.scheduledStartTime,
                                    endTime = item.scheduledEndTime,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 1.dp) // Minimal horizontal padding within the day column
                                        .height(itemHeight.coerceAtLeast(24.dp)) // Min height for clickability
                                        .offset(y = topOffset), // Position vertically
                                    onClick = { onTaskClick(item.task) } // Attach click listener
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
    date: LocalDate, // The specific date this view represents
    items: List<ScheduledTaskItem>,
    onTaskClick: (Task) -> Unit,
    modifier: Modifier = Modifier, // Allow passing modifiers
) {
    val hourHeight: Dp = 60.dp // Height for one hour slot
    val density = LocalDensity.current
    val hourHeightPx = remember(hourHeight, density) { with(density) { hourHeight.toPx() } }
    val totalHeight =
        remember(hourHeight) { hourHeight * 24 } // Calculate total height for 24 hours
    val timeLabelWidth: Dp = 48.dp // Consistent width for time labels
    val scrollState = rememberScrollState()
    // Use a Box for layering the grid, labels, and tasks
    Box(
        modifier = modifier
            .height(totalHeight) // Set the total height for the 24-hour day
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
                // Draw horizontal hour lines
                for (hour in 0..23) {
                    val y = hour * hourHeightPx
                    drawLine(
                        color = color, // Subtle grid line
                        start = Offset(labelWidthPx, y), // Start after label area
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    // Optional: Draw dashed half-hour lines
                    val halfY = y + hourHeightPx / 2
                    drawLine(
                        color = color2, // Even more subtle
                        start = Offset(labelWidthPx, halfY),
                        end = Offset(size.width, halfY),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(
                                4f,
                                4f
                            )
                        ) // Dashed effect
                    )
                }
                // Draw the vertical line separating labels from content
                drawLine(
                    color = color,
                    start = Offset(labelWidthPx, 0f),
                    end = Offset(labelWidthPx, size.height),
                    strokeWidth = 1f
                )
            }

            // Layer 2: Time Labels Column
            Column(
                modifier = Modifier
                    .fillMaxHeight() // Take full height of the parent Box
                    .width(timeLabelWidth)
                    .padding(end = 4.dp) // Padding between label and grid line
            ) {
                for (hour in 0..23) {
                    // Format time labels (e.g., 1AM, 12PM)
                    val timeString = when (hour) {
                        0 -> "12AM"
                        12 -> "12PM"
                        in 1..11 -> "${hour}AM"
                        else -> "${hour - 12}PM"
                    }
                    Box(
                        modifier = Modifier
                            .height(hourHeight) // Each label box takes the height of an hour
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter // Align text to the top-center near the line
                    ) {
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, // Use a secondary color
                            modifier = Modifier.padding(top = 4.dp) // Padding from the top line
                        )
                    }
                }
            }

            // Layer 3: Scheduled Task Items Area (Offset to start after labels)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = timeLabelWidth) // Offset content area
            ) {
                // Sort items by start time to handle overlaps (though ideally planner prevents overlaps)
                items.sortedBy { it.scheduledStartTime }.forEach { item ->
                    // Calculate position and size based on scheduled times
                    val startMinutes =
                        item.scheduledStartTime.hour * 60 + item.scheduledStartTime.minute
                    val endMinutes = item.scheduledEndTime.hour * 60 + item.scheduledEndTime.minute
                    // Ensure a minimum visual duration (e.g., 15 minutes) for very short tasks
                    val durationMinutes = (endMinutes - startMinutes).coerceAtLeast(15)

                    // Calculate Dp values based on pixels per hour
                    val topOffset = (startMinutes / 60f) * hourHeight
                    val itemHeight = (durationMinutes / 60f) * hourHeight

                    // Use the ReviewTaskCard for display
                    ReviewTaskCard(
                        task = item.task,
                        startTime = item.scheduledStartTime,
                        endTime = item.scheduledEndTime,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 2.dp,
                                vertical = 0.5.dp
                            ) // Minimal padding between cards
                            .height(itemHeight.coerceAtLeast(24.dp)) // Ensure minimum clickable height
                            .offset(y = topOffset), // Position card vertically
                        onClick = { onTaskClick(item.task) } // Pass the click callback
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
    onTaskClick: (Task) -> Unit, // Add callback
) {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)) {
        when (viewType) {
            CalendarView.WEEK -> {
                val weekStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                ReviewWeeklyViewContent(
                    startDate = weekStart,
                    plan = plan,
                    onTaskClick = onTaskClick
                ) // Pass down
            }

            CalendarView.DAY -> {
                ReviewDailyViewContent(
                    date = startDate,
                    items = plan[startDate] ?: emptyList(),
                    onTaskClick = onTaskClick
                ) // Pass down
            }

            CalendarView.MONTH -> {
                val weekStart = startDate.withDayOfMonth(1)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                ReviewWeeklyViewContent(
                    startDate = weekStart,
                    plan = plan,
                    onTaskClick = onTaskClick
                ) // Pass down
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
    ResolutionOption.MOVE_TO_NEAREST_FREE -> "Move Auto" // Shorter label
    ResolutionOption.MOVE_TO_TOMORROW -> "Move Tomorrow"
    ResolutionOption.MANUALLY_SCHEDULE -> "Schedule Manually"
    ResolutionOption.LEAVE_IT_LIKE_THAT -> "Leave As Is"
    ResolutionOption.RESOLVED -> "Resolved" // Internal state, maybe shouldn't be shown as an option
}