package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import org.koin.androidx.compose.koinViewModel
import presentation.states.TaskState
import presentation.viewmodel.TaskViewModel
import domain.models.Task
import domain.models.TimePlanning
import domain.models.DurationPlan
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.presentation.intents.TaskIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    navController: NavHostController,
    viewModel: TaskViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddTaskSheet by remember { mutableStateOf(false) }

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

        // Show the "Add Task" bottom sheet if needed
        if (showAddTaskSheet) {
            AddTaskSheet(
                onClose = { showAddTaskSheet = false },
                onAccept = { newTaskData ->
                    // Convert the "newTaskData" to the AddTask intent
                    viewModel.triggerEvent(
                        TaskIntent.AddTask(
                            name = newTaskData.name,
                            priority = newTaskData.priority,
                            startDateConf = newTaskData.startDateConf,
                            endDateConf = newTaskData.endDateConf,
                            durationConf = newTaskData.durationConf,
                            reminderPlan = newTaskData.reminderPlan,
                            repeatPlan = newTaskData.repeatPlan,
                            subtasks = newTaskData.subtasks
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
            item {
                Spacer(Modifier.height(16.dp))
                TaskSection("Completed", state.completedTasks)
            }
            item {
                Spacer(Modifier.height(16.dp))
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = {
                    // Possibly dispatch an Intent to mark complete
                }
            )

            Spacer(Modifier.width(8.dp))

            Column(Modifier.weight(1f)) {
                Text(task.name, fontSize = 16.sp)
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
