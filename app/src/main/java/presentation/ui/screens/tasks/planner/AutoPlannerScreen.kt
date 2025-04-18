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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import com.elena.autoplanner.R
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
import com.elena.autoplanner.presentation.intents.TaskEditIntent
import com.elena.autoplanner.presentation.states.PlannerState
import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarView
import com.elena.autoplanner.presentation.ui.screens.tasks.HourMinutePickerDialog
import com.elena.autoplanner.presentation.ui.screens.tasks.modificationTaskSheet.ModificationTaskSheet
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
            titleContentColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = MaterialTheme.colorScheme.primary
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


    if (showStartTimePicker && state != null) {
        HourMinutePickerDialog( // Use the NumberPicker based dialog
            initialTime = state!!.workStartTime,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { selectedTime ->
                viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(selectedTime))
                showStartTimePicker = false
            }
        )
    }


    if (showEndTimePicker && state != null) {
        HourMinutePickerDialog( // Use the NumberPicker based dialog
            initialTime = state!!.workEndTime,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { selectedTime ->
                if (selectedTime > state!!.workStartTime) {
                    viewModel.sendIntent(PlannerIntent.UpdateWorkEndTime(selectedTime))
                    showEndTimePicker = false
                } else {

                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PlannerTopAppBar(onBackClick = {
                // Simplified back logic: If reviewing, go back to config, otherwise cancel.
                if (state?.currentStep == PlannerStep.REVIEW_PLAN) {
                    viewModel.sendIntent(PlannerIntent.GoToPreviousStep) // Assumes ViewModel handles going back to the config phase
                } else {
                    viewModel.sendIntent(PlannerIntent.CancelPlanner)
                }
            })
        },
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            state?.let { currentState ->
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    // Pass the updated navigation logic check
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
                .background(MaterialTheme.colorScheme.surface)
        ) {
            state?.let { currentState ->
                PlannerContent( // Pass state and handlers to PlannerContent
                    state = currentState,
                    onIntent = viewModel::sendIntent,
                    onStartTimeClick = { if (startTimeState != null) showStartTimePicker = true },
                    onEndTimeClick = { if (endTimeState != null) showEndTimePicker = true },
                    onReviewTaskClick = { task ->
                        // Keep review task click logic
                        if (currentState.tasksFlaggedForManualEdit.contains(task.id)) {
                            Log.d("AutoPlannerScreen", "Edit flagged task clicked: ${task.id}")
                            taskForEditSheet = task
                            taskIdForEditSheet = task.id
                            selectedTaskIdForDetail = null
                        } else {
                            Log.d("AutoPlannerScreen", "Normal task clicked: ${task.id}")
                            selectedTaskIdForDetail = task.id
                            taskIdForEditSheet = null
                        }
                    }
                )
            }
                ?: CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) // Loading indicator if state is null

            // Loading Overlay
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

        // --- Task Detail Sheet (Keep as is) ---
        selectedTaskIdForDetail?.let { taskId ->
            val detailViewModel: TaskDetailViewModel =
                koinViewModel(parameters = { parametersOf(taskId) })
            // ... (LaunchedEffect for detailViewModel) ...
            TaskDetailSheet(
                taskId = taskId,
                onDismiss = { selectedTaskIdForDetail = null },
                viewModel = detailViewModel
            )
        }

        // --- Modification Task Sheet (Keep as is) ---
        taskIdForEditSheet?.let { taskIdToEdit ->
            val editViewModel: TaskEditViewModel =
                koinViewModel(parameters = { parametersOf(taskIdToEdit) })
            // ... (LaunchedEffect for editViewModel) ...
            ModificationTaskSheet(
                taskEditViewModel = editViewModel,
                onClose = { editViewModel.sendIntent(TaskEditIntent.Cancel) }
            )
        }
    }
}

@Composable
fun PlannerConfigurationStep(
    state: PlannerState,
    onIntent: (PlannerIntent) -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
) {
    // Card for Work Hours
    QuestionnaireSectionCard(title = "Availability Hours", icon = Icons.Outlined.DateRange) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            ModernTimeSelectorButton(
                time = state.workStartTime,
                label = "Start Time",
                onClick = onStartTimeClick,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            ModernTimeSelectorButton(
                time = state.workEndTime,
                label = "End Time",
                onClick = onEndTimeClick,
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Card for Scheduling Scope
    QuestionnaireSectionCard(title = "Scheduling Scope", icon = Icons.Outlined.DateRange) {
        SingleChoiceChipGroup(
            options = ScheduleScope.entries,
            selectedOption = state.scheduleScope,
            onOptionSelected = { onIntent(PlannerIntent.SelectScheduleScope(it)) },
            labelSelector = { it.toDisplayString() },
        )
    }

    // Card for Prioritization Strategy
    QuestionnaireSectionCard(title = "Prioritization Strategy", icon = Icons.Outlined.List) {
        SingleChoiceChipGroup(
            options = PrioritizationStrategy.entries,
            selectedOption = state.selectedPriority,
            onOptionSelected = { priority ->
                onIntent(PlannerIntent.SelectPriority(priority))
            },
            labelSelector = { it.toDisplayString() },
        )
    }

    // Card for Day Organization Style
    QuestionnaireSectionCard(title = "Day Organization Style", icon = Icons.Outlined.List) {
        SingleChoiceChipGroup(
            options = DayOrganization.entries,
            selectedOption = state.selectedDayOrganization,
            onOptionSelected = { organization ->
                onIntent(PlannerIntent.SelectDayOrganization(organization))
            },
            labelSelector = { it.toDisplayString() }
            // Optional: Add icons like DensityMedium, DensitySmall, etc.
        )
    }

    // Card for Task Splitting
    BooleanSettingCard(
        title = "Task Splitting",
        icon = Icons.Outlined.Menu,
        description = "Allow splitting long tasks with durations?",
        checked = state.allowSplitting, // Make sure allowSplitting is nullable or provide default
        onCheckedChange = { allow -> onIntent(PlannerIntent.SelectAllowSplitting(allow)) }
    )

    // Card for Overdue Tasks (Conditional)
    // Use AnimatedVisibility for a smoother appearance/disappearance
    AnimatedVisibility(visible = state.numOverdueTasks > 0) {
        QuestionnaireSectionCard(
            title = "Overdue Tasks (${state.numOverdueTasks})",
            icon = Icons.Outlined.Warning
        ) {
            Text(
                text = "How should we handle tasks past their deadline?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            SingleChoiceChipGroup(
                options = OverdueTaskHandling.entries,
                selectedOption = state.selectedOverdueHandling,
                onOptionSelected = { handling ->
                    onIntent(PlannerIntent.SelectOverdueHandling(handling))
                },
                labelSelector = { it.toDisplayString() }
            )
        }
    }

    // Optional: Show a confirmation if no overdue tasks, can be simpler now
    AnimatedVisibility(visible = state.numOverdueTasks <= 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp), // Add some padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp) // Slightly smaller icon
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "No overdue tasks found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReviewSectionHeader(title: String, isError: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp, start = 4.dp)
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
    var currentCalendarView by remember { mutableStateOf(CalendarView.DAY) }

    val availableViews = remember(state.scheduleScope) {
        when (state.scheduleScope) {
            ScheduleScope.TODAY, ScheduleScope.TOMORROW -> listOf(CalendarView.DAY)
            else -> listOf(CalendarView.DAY, CalendarView.WEEK)
        }
    }

    LaunchedEffect(availableViews) {
        if (availableViews.size > 1 && state.scheduleScope == ScheduleScope.THIS_WEEK) {
            currentCalendarView = CalendarView.WEEK
        } else {
            currentCalendarView = CalendarView.DAY
        }
    }


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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                                onIntent(PlannerIntent.UnflagTaskForManualEdit(it.id))
                                            }
                                        }
                                    },
                                    onTaskClick = onReviewTaskClick,
                                    tasksFlaggedForManualEdit = state.tasksFlaggedForManualEdit,
                                    modifier = Modifier
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
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (availableViews.size > 1) {
                    ReviewViewToggle(
                        availableViews = availableViews,
                        selectedView = currentCalendarView,
                        onViewSelected = { currentCalendarView = it }
                    )
                }
            }


            Box(
                modifier = Modifier
                    .heightIn(min = 200.dp, max = 550.dp)
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (state.generatedPlan.isNotEmpty() || state.conflictsToResolve.isNotEmpty()) {
                    GeneratedPlanReviewView(
                        viewType = currentCalendarView,
                        plan = state.generatedPlan,
                        conflicts = state.conflictsToResolve,
                        resolutions = state.conflictResolutions + state.taskResolutions,
                        startDate = planStartDate,
                        onTaskClick = onReviewTaskClick,
                        tasksFlaggedForManualEdit = state.tasksFlaggedForManualEdit
                    )
                } else if (!state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No tasks scheduled. Try adjusting the options or check conflicts.",
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


@Composable
fun PlannerNavigationButtons(
    state: PlannerState,
    onIntent: (PlannerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReviewStep = state.currentStep == PlannerStep.REVIEW_PLAN

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween // Keep space between
    ) {
        // Back / Cancel Button
        OutlinedButton(
            onClick = {
                if (isReviewStep) {
                    // If reviewing, go back to the configuration step
                    onIntent(PlannerIntent.GoToPreviousStep) // ViewModel should handle this transition
                } else {
                    // If configuring, cancel the whole process
                    onIntent(PlannerIntent.CancelPlanner)
                }
            },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = if (isReviewStep) "Back" else "Cancel", // Dynamic text
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Generate Plan / Add Plan Button
        Button(
            onClick = {
                if (isReviewStep) {
                    onIntent(PlannerIntent.AddPlanToCalendar)
                } else {
                    // Directly generate plan from the configuration step
                    onIntent(PlannerIntent.GeneratePlan)
                }
            },
            enabled = when {
                isReviewStep -> !state.requiresResolution && !state.isLoading // Enable condition for review step
                else -> state.canGeneratePlan && !state.isLoading // Enable condition for config step
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = if (isReviewStep) "Add Plan" else "Generate Plan", // Dynamic text
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun ReviewViewToggle(
    availableViews: List<CalendarView>,
    selectedView: CalendarView,
    onViewSelected: (CalendarView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50)),
        horizontalArrangement = Arrangement.Center
    ) {
        availableViews.forEachIndexed { index, view ->
            val isSelected = view == selectedView

            // Determine shape based on position
            val shape = when {
                availableViews.size == 1 -> RoundedCornerShape(50) // Fully rounded if only one
                index == 0 -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                index == availableViews.lastIndex -> RoundedCornerShape(
                    topEnd = 50.dp,
                    bottomEnd = 50.dp
                )

                else -> RoundedCornerShape(0.dp)
            }

            // Use IconToggleButton for better semantics and state handling
            IconToggleButton(
                checked = isSelected,
                onCheckedChange = { if (it) onViewSelected(view) }, // Select only when checked
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight() // Fill height within the Row
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = shape // Apply shape to background
                    )
                    .clip(shape), // Clip the content area as well
                colors = androidx.compose.material3.IconButtonDefaults.iconToggleButtonColors(
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    painter = painterResource(id = view.getIconRes()), // Use the existing icon function
                    contentDescription = view.name,
                    modifier = Modifier.size(20.dp) // Adjust icon size if needed
                )
            }
        }
    }
}

// Ensure getIconRes is accessible or defined here
fun CalendarView.getIconRes(): Int = when (this) {
    CalendarView.DAY -> R.drawable.ic_day_view
    CalendarView.WEEK -> R.drawable.ic_week_view
    CalendarView.MONTH -> R.drawable.ic_month_view
}


@Composable
fun GeneratedPlanReviewView(
    viewType: CalendarView,
    plan: Map<LocalDate, List<ScheduledTaskItem>>,
    conflicts: List<ConflictItem>, // Added
    resolutions: Map<Int, ResolutionOption>,
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
                    conflicts = conflicts, // Pass down
                    resolutions = resolutions, // Pass down
                    onTaskClick = onTaskClick,
                    tasksFlaggedForManualEdit = tasksFlaggedForManualEdit,
                    modifier = Modifier.fillMaxHeight()
                )
            }

            CalendarView.DAY -> {
                ReviewDailyViewContent(
                    date = startDate,
                    items = plan[startDate] ?: emptyList(),
                    conflicts = conflicts.filter { // Filter conflicts relevant to this day
                        it.conflictTime?.toLocalDate() == startDate || it.conflictingTasks.any { t -> t.startDateConf.dateTime?.toLocalDate() == startDate }
                    },
                    resolutions = resolutions, // Pass down
                    onTaskClick = onTaskClick,
                    tasksFlaggedForManualEdit = tasksFlaggedForManualEdit,
                    modifier = Modifier.fillMaxHeight()
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
    conflicts: List<ConflictItem>, // Added
    resolutions: Map<Int, ResolutionOption>, // Added
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

    val conflictedTasksToShow = remember(conflicts, resolutions, date) {
        conflicts.flatMap { conflict ->
            // Check if the conflict itself is resolved in a way that removes it visually
            val conflictResolved = resolutions[conflict.hashCode()]?.let {
                it == ResolutionOption.MOVE_TO_TOMORROW // Only move removes it visually here
            } ?: false

            if (conflictResolved) {
                emptyList()
            } else {
                conflict.conflictingTasks.filter { task ->
                    // Show if task resolution is null, LeaveAsIs, or Manual
                    val taskResolution = resolutions[task.id]
                    val showTask = taskResolution == null ||
                            taskResolution == ResolutionOption.LEAVE_IT_LIKE_THAT ||
                            taskResolution == ResolutionOption.MANUALLY_SCHEDULE

                    // And the task belongs to this date (either by conflict time or original start date)
                    showTask && (conflict.conflictTime?.toLocalDate() == date || task.startDateConf.dateTime?.toLocalDate() == date)
                }
            }
        }.distinctBy { it.id } // Avoid duplicates if a task is in multiple conflicts
    }
    val allItemsToRender = remember(items, conflictedTasksToShow) {
        val scheduledTaskIds = items.map { it.task.id }.toSet()
        // Combine, ensuring tasks from conflicts list are added if not already in scheduled items
        items + conflictedTasksToShow
            .filterNot { scheduledTaskIds.contains(it.id) }
            .map { task ->
                // Create a dummy ScheduledTaskItem for conflicted tasks not in the plan
                // Use conflict time or original start time for positioning
                val startTime =
                    conflicts.find { c -> c.conflictingTasks.any { t -> t.id == task.id } }?.conflictTime?.toLocalTime()
                        ?: task.startTime
                val endTime = startTime.plusMinutes(task.effectiveDurationMinutes.toLong())
                ScheduledTaskItem(
                    task,
                    startTime,
                    endTime,
                    task.startDateConf.dateTime?.toLocalDate() ?: date
                )
            }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
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
                .padding(start = 6.dp) // Padding from the grid line
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
                        modifier = Modifier.padding(top = 2.dp) // Use small top padding instead of offset
                    )
                }
            }
        }

        // Task Items Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = timeLabelWidth, end = 4.dp)
        ) {
            // Render combined list
            allItemsToRender.sortedBy { it.scheduledStartTime }.forEach { item ->
                val startMinutes =
                    item.scheduledStartTime.hour * 60 + item.scheduledStartTime.minute
                val endMinutes = item.scheduledEndTime.hour * 60 + item.scheduledEndTime.minute
                val durationMinutes = (endMinutes - startMinutes).coerceAtLeast(15)
                val topOffset = (startMinutes / 60f) * hourHeight
                val itemHeight = (durationMinutes / 60f) * hourHeight

                // Check if this task is part of an unresolved conflict
                val isConflicted = conflictedTasksToShow.any { it.id == item.task.id }

                ReviewTaskCard(
                    task = item.task,
                    startTime = item.scheduledStartTime,
                    endTime = item.scheduledEndTime,
                    isFlaggedForManualEdit = tasksFlaggedForManualEdit.contains(item.task.id),
                    isConflicted = isConflicted, // Pass conflict status
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 0.5.dp)
                        .height(itemHeight.coerceAtLeast(24.dp))
                        .offset(y = topOffset),
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
    conflicts: List<ConflictItem>, // Added
    resolutions: Map<Int, ResolutionOption>,
    onTaskClick: (Task) -> Unit,
    tasksFlaggedForManualEdit: Set<Int>, // Accept flagged IDs
    modifier: Modifier = Modifier,
) {
    val weekDays = remember(startDate) {
        // Ensure week starts on Monday and includes Sunday
        val monday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        (0..6).map { monday.plusDays(it.toLong()) }
    }
    val hourHeight: Dp = 60.dp
    val density = LocalDensity.current
    val hourHeightPx = remember(hourHeight, density) { with(density) { hourHeight.toPx() } }
    val timeLabelWidth: Dp = 48.dp
    val totalGridHeight = remember(hourHeight) { hourHeight * 24 }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val availableWidthForDays = (screenWidthDp - timeLabelWidth)
    val dayWidth: Dp = 47.dp
    val conflictedTasksToShowMap = remember(conflicts, resolutions, weekDays) {
        weekDays.associateWith { date ->
            conflicts.flatMap { conflict ->
                val conflictResolved = resolutions[conflict.hashCode()]?.let {
                    it == ResolutionOption.MOVE_TO_TOMORROW
                } ?: false
                if (conflictResolved) emptyList()
                else {
                    conflict.conflictingTasks.filter { task ->
                        val taskResolution = resolutions[task.id]
                        val showTask = taskResolution == null ||
                                taskResolution == ResolutionOption.LEAVE_IT_LIKE_THAT ||
                                taskResolution == ResolutionOption.MANUALLY_SCHEDULE
                        showTask && (conflict.conflictTime?.toLocalDate() == date || task.startDateConf.dateTime?.toLocalDate() == date)
                    }
                }
            }.distinctBy { it.id }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header Row for Day Names and Dates
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            // Add Spacer for the time label width
            Spacer(modifier = Modifier.width(timeLabelWidth))
            // Day Headers Row (takes remaining width)
            Row(modifier = Modifier.weight(1f)) {
                weekDays.forEach { day ->
                    Column(
                        modifier = Modifier
                            .width(dayWidth) // Assign calculated width
                            .padding(
                                vertical = 6.dp,
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally // Center content
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
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        // Scrollable Grid Area
        Box(
            modifier = Modifier
                .heightIn(max = 500.dp) // Limit height and make scrollable if content exceeds
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {
            // --- Combined Time Labels and Grid Content ---
            Row(
                modifier = Modifier
                    .height(totalGridHeight) // Set fixed height for 24 hours
                    .fillMaxWidth()
            ) {
                // Time Labels Column
                Column(
                    modifier = Modifier
                        .width(timeLabelWidth)
                        .fillMaxHeight()
                        .padding(start = 6.dp) // Padding from the grid line
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
                                modifier = Modifier.padding(top = 4.dp) // Use small top padding instead of offset
                            )
                        }
                    }
                }
                val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                val color1 = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                // Grid Content Area (Canvas + Tasks)
                Box(
                    modifier = Modifier
                        .weight(1f) // Takes remaining width
                        .fillMaxHeight()
                        .padding(end = 4.dp) // Padding to the right
                ) {
                    // Background Grid Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val dayWidthPx = with(density) { dayWidth.toPx() }
                        val gridColor = color
                        val gridColorHalf = color1

                        // Draw horizontal hour lines
                        for (hour in 0..23) {
                            val y = hour * hourHeightPx
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y), // Start from x=0 of this Box
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                            // Draw half-hour lines
                            val halfY = y + hourHeightPx / 2
                            drawLine(
                                color = gridColorHalf,
                                start = Offset(0f, halfY), // Start from x=0
                                end = Offset(size.width, halfY),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                            )
                        }

                        // Draw vertical lines separating days
                        for (i in 1 until weekDays.size) { // Draw lines BETWEEN days
                            val x = i * dayWidthPx
                            drawLine(
                                color = gridColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f
                            )
                        }
                    }

                    // Task Rendering Row (Overlay on Canvas)
                    Row(
                        modifier = Modifier.fillMaxSize() // Fill the Grid Content Area Box
                    ) {
                        weekDays.forEach { date ->
                            Box(
                                modifier = Modifier
                                    .width(dayWidth)
                                    .fillMaxHeight()
                            ) {
                                // ... (Combine scheduled and conflicted items - allItemsToRender logic) ...
                                val scheduledItems = plan[date] ?: emptyList()
                                val conflictedTasks = conflictedTasksToShowMap[date] ?: emptyList()
                                val scheduledTaskIds = scheduledItems.map { it.task.id }.toSet()
                                val allItemsToRender = scheduledItems + conflictedTasks
                                    .filterNot { scheduledTaskIds.contains(it.id) }
                                    .map { task -> /* ... create dummy item ... */
                                        val startTime =
                                            conflicts.find { c -> c.conflictingTasks.any { t -> t.id == task.id } }?.conflictTime?.toLocalTime()
                                                ?: task.startTime
                                        val endTime =
                                            startTime.plusMinutes(task.effectiveDurationMinutes.toLong())
                                        ScheduledTaskItem(
                                            task,
                                            startTime,
                                            endTime,
                                            task.startDateConf.dateTime?.toLocalDate() ?: date
                                        )
                                    }


                                allItemsToRender.sortedBy { it.scheduledStartTime }
                                    .forEach { item ->
                                        // ... (Calculate topOffset, itemHeight) ...
                                        val startMinutes =
                                            item.scheduledStartTime.hour * 60 + item.scheduledStartTime.minute
                                        val endMinutes =
                                            item.scheduledEndTime.hour * 60 + item.scheduledEndTime.minute
                                        val durationMinutes =
                                            (endMinutes - startMinutes).coerceAtLeast(15)
                                        val topOffset = (startMinutes / 60f) * hourHeight
                                        val itemHeight = (durationMinutes / 60f) * hourHeight
                                        val isConflicted =
                                            conflictedTasks.any { it.id == item.task.id }

                                        ReviewTaskCard(
                                            task = item.task,
                                            startTime = item.scheduledStartTime,
                                            endTime = item.scheduledEndTime,
                                            isFlaggedForManualEdit = tasksFlaggedForManualEdit.contains(
                                                item.task.id
                                            ),
                                            isConflicted = isConflicted,
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
                } // End Task Rendering Row
            } // End Grid Content Area Box
        } // End Combined Row (Labels + Grid)
    } // End Scrollable Box
}

@Composable
fun ConflictResolutionCard(
    conflict: ConflictItem,
    options: List<ResolutionOption>,
    selectedOption: ResolutionOption?,
    onOptionSelected: (ResolutionOption) -> Unit,
    onTaskClick: (Task) -> Unit, // Callback to handle task clicks
    tasksFlaggedForManualEdit: Set<Int>, // To style flagged tasks
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("MMM d, HH:mm") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp) // Increased spacing
        ) {
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
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    conflict.conflictTime?.let {
                        Text(
                            "Around: ${it.format(timeFormatter)}", // Changed "At" to "Around"
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) { // Reduced start padding
                Text(
                    "Involved Tasks:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                conflict.conflictingTasks.forEach { task ->
                    val displayStartTime = conflict.conflictTime?.toLocalTime() ?: task.startTime
                    val displayEndTime =
                        displayStartTime.plusMinutes(task.effectiveDurationMinutes.toLong())
                    ReviewTaskCard(
                        task = task,
                        startTime = displayStartTime, // Use conflict time or task start
                        endTime = displayEndTime,     // Calculate end based on duration
                        isFlaggedForManualEdit = tasksFlaggedForManualEdit.contains(task.id),
                        modifier = Modifier.fillMaxWidth(), // Make cards fill width within the column
                        onClick = { onTaskClick(task) },
                        isConflicted = true
                    )
                }
            }

            // Resolution Dropdown
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
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
                    ?: task.startDateConf.dateTime?.format(dateFormatter) ?: "Unknown Date"
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
    isConflicted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val priorityColor = when (task.priority) {
        Priority.HIGH -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
        Priority.LOW -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        Priority.NONE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    // Base card color
    var cardColor = when {
        isFlaggedForManualEdit -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        else -> MaterialTheme.colorScheme.surface
    }
    // Base border color
    var borderColor = when {
        isFlaggedForManualEdit -> MaterialTheme.colorScheme.tertiary
        else -> priorityColor.copy(alpha = 0.5f)
    }
    // Base text color
    var textColor = when {
        isFlaggedForManualEdit -> MaterialTheme.colorScheme.onTertiaryContainer
        task.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Override for conflict
    if (isConflicted) {
        cardColor =
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) // More subtle conflict background
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f) // Stronger conflict border
        textColor =
            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f) // Ensure text is readable
    }

    val borderStroke = BorderStroke(
        width = if (isFlaggedForManualEdit || isConflicted) 1.5.dp else 1.dp,
        color = borderColor
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(cardColor)
            .border(border = borderStroke, shape = RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            // Priority/Conflict Indicator
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        color = if (isConflicted) borderColor else priorityColor,
                        shape = RoundedCornerShape(2.dp)
                    ) // Use border color for conflict
            )
            Spacer(Modifier.width(5.dp))
            // Task Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isFlaggedForManualEdit) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Edit, "Edit Required", Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    } else if (isConflicted) { // Show warning for conflict if not flagged for edit
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Warning, "Conflict", Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Text(
                    "${startTime.format(timeFormatter)} - ${endTime.format(timeFormatter)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
            if (task.isCompleted && !isFlaggedForManualEdit && !isConflicted) { // Hide check if conflicted or flagged
                Icon(
                    Icons.Default.Check, "Completed", Modifier
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

// Add a helper function to get a title for each step
fun PlannerStep.getTitle(): String = when (this) {
    PlannerStep.TIME_INPUT -> "Set Time & Scope"
    PlannerStep.PRIORITY_INPUT -> "Define Priorities"
    PlannerStep.ADDITIONAL_OPTIONS -> "Configure Options"
    PlannerStep.REVIEW_PLAN -> "Review & Confirm"
}


@Composable
fun QuestionnaireSectionCard(
    title: String,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard( // Use ElevatedCard for a slight lift
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp), // Slightly more rounded
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface) // Use surface, chips will provide contrast
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface // Use onSurface for better contrast
                )
            }
            // Content provided by the caller
            content()
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
) // Add ExperimentalLayoutApi for FlowRow
@Composable
fun <T> SingleChoiceChipGroup(
    modifier: Modifier = Modifier,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    labelSelector: (T) -> String,
    iconSelector: ((T) -> ImageVector?)? = null, // Optional icon selector

) {
    FlowRow( // Use FlowRow to allow chips to wrap on smaller screens
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp) // Spacing between rows if it wraps
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val icon = iconSelector?.invoke(option)
            FilterChip(
                selected = isSelected,
                onClick = { onOptionSelected(option) },
                label = { Text(labelSelector(option)) },
                leadingIcon = if (icon != null) { // Conditionally display icon
                    {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    selectedBorderWidth = 1.dp,
                    enabled = true,
                    selected = isSelected

                )
            )
        }
    }
}

@Composable
fun BooleanSettingCard(
    title: String,
    description: String,
    checked: Boolean?,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    QuestionnaireSectionCard(title = title, icon = icon) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { checked?.let { onCheckedChange(!it) } }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = 0.6f
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            )
            Switch(
                checked = checked == true,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
        }
    }
}

// Updated Time Selector Button
@Composable
fun ModernTimeSelectorButton(
    time: LocalTime,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp), // More rounded
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface // Use onSurface
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.DateRange, // Use outlined icon
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary // Tint icon primary
            )
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.Start) { // Align text left
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall, // Smaller label
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = time.format(formatter),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium // Slightly bolder time
                )
            }
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
            .padding(bottom = 80.dp), // Keep bottom padding for nav buttons
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Title remains
        Text(
            text = if (state.currentStep == PlannerStep.REVIEW_PLAN) "Review Your Plan" else "Configure Your Auto-Plan",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(
                top = 16.dp,
                bottom = 16.dp,
                start = 16.dp,
                end = 16.dp
            ), // Increased bottom padding
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        // Use a Column with spacing for the content area
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp) // Consistent spacing between cards/sections
        ) {
            // Show configuration or review based on the current step
            when (state.currentStep) {
                PlannerStep.TIME_INPUT, PlannerStep.PRIORITY_INPUT, PlannerStep.ADDITIONAL_OPTIONS -> {
                    // Show the combined configuration step
                    PlannerConfigurationStep(
                        state = state,
                        onIntent = onIntent,
                        onStartTimeClick = onStartTimeClick,
                        onEndTimeClick = onEndTimeClick
                    )
                }

                PlannerStep.REVIEW_PLAN -> {
                    // Show the review step (Step4ReviewPlan remains mostly the same)
                    Step4ReviewPlan(
                        state = state,
                        onIntent = onIntent,
                        onReviewTaskClick = onReviewTaskClick
                    )
                }
                // Handle other potential future steps if necessary
            }
        }

        // Error message display remains
        if (state.error != null) {
            Text(
                "Error: ${state.error}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}


