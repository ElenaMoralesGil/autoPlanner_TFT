package com.elena.autoplanner.presentation.ui.screens.tasks.planner

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.elena.autoplanner.domain.models.PlannerStep
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.effects.PlannerEffect
import com.elena.autoplanner.presentation.intents.PlannerIntent
import com.elena.autoplanner.presentation.intents.TaskEditIntent
import com.elena.autoplanner.presentation.ui.screens.tasks.HourMinutePickerDialog
import com.elena.autoplanner.presentation.ui.screens.tasks.TaskDetailSheet
import com.elena.autoplanner.presentation.ui.screens.tasks.modificationTaskSheet.ModificationTaskSheet
import com.elena.autoplanner.presentation.viewmodel.PlannerViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskDetailViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskEditViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalTime

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
        HourMinutePickerDialog(
            initialTime = state!!.workStartTime,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { selectedTime ->
                viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(selectedTime))
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker && state != null) {
        HourMinutePickerDialog(
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

                if (state?.currentStep == PlannerStep.REVIEW_PLAN) {
                    viewModel.sendIntent(PlannerIntent.GoToPreviousStep)
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
                PlannerContent(
                    state = currentState,
                    onIntent = viewModel::sendIntent,
                    onStartTimeClick = { if (startTimeState != null) showStartTimePicker = true },
                    onEndTimeClick = { if (endTimeState != null) showEndTimePicker = true },
                    onReviewTaskClick = { task ->

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
                ?: CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

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

            TaskDetailSheet(
                taskId = taskId,
                onDismiss = { selectedTaskIdForDetail = null },
                viewModel = detailViewModel
            )
        }

        taskIdForEditSheet?.let { taskIdToEdit ->
            val editViewModel: TaskEditViewModel =
                koinViewModel(parameters = { parametersOf(taskIdToEdit) })

            ModificationTaskSheet(
                taskEditViewModel = editViewModel,
                onClose = { editViewModel.sendIntent(TaskEditIntent.Cancel) }
            )
        }
    }
}