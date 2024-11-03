package presentation.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import presentation.states.TaskState
import presentation.viewmodel.TaskViewModel
import com.elena.autoplanner.R
import presentation.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TaskViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.all_tasks), fontSize = 20.sp) },
                actions = {
                    IconButton(onClick = { /* Future auto-planner action */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Add task action */ },
                containerColor = Color(0xFFFF9800)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_task))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            } else {
                state.error?.let {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = it,
                            color = Color.Red,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } ?: run {
                    TaskListSection(state)
                }
            }
        }
    }
}

@Composable
fun TaskListSection(state: TaskState) {
    if (state.notCompletedTasks.isEmpty() &&
        state.completedTasks.isEmpty() &&
        state.expiredTasks.isEmpty()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(id = R.string.no_tasks),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    } else {
        LazyColumn {
            item {
                TaskSection(
                    title = stringResource(id = R.string.not_completed),
                    tasks = state.notCompletedTasks
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                TaskSection(
                    title = stringResource(id = R.string.completed),
                    tasks = state.completedTasks
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                TaskSection(
                    title = stringResource(id = R.string.expired),
                    tasks = state.expiredTasks
                )
            }
        }
    }
}

@Composable
fun TaskSection(title: String, tasks: List<domain.models.Task>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = title, fontSize = 18.sp)
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(tasks) { task ->
                TaskItemComposable(task)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun TaskItemComposable(task: domain.models.Task) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { /* Handle task completion */ }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = task.name,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            task.deadline?.let {
                Text(
                    text = stringResource(id = R.string.due, it.toString()),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TasksScreenPreview() {
    val sampleTasks = listOf(
        domain.models.Task(
            id = 1,
            name = "Sample Task",
            deadline = java.time.LocalDateTime.now().plusDays(2),
            isCompleted = false,
            isExpired = false
        )
    )
    val sampleState = TaskState(
        notCompletedTasks = sampleTasks,
        completedTasks = emptyList(),
        expiredTasks = emptyList()
    )

    AppTheme {
        TaskListSection(sampleState)
    }
}
