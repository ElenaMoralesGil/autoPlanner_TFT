package presentation.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import org.koin.androidx.compose.koinViewModel
import presentation.navigation.Screen
import presentation.states.TaskState
import presentation.viewmodel.TaskViewModel
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.Reminder
import com.elena.autoplanner.domain.models.RepeatConfig
import com.elena.autoplanner.presentation.intents.TaskIntent
import domain.models.Priority
import domain.models.Subtask
import java.time.LocalDateTime

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
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.all_tasks), fontSize = 20.sp) },
                actions = {
                    IconButton(onClick = { /* menú futuro */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.add_task)
                )
            }
        }
    ) { innerPadding ->
        // Contenido principal (lista de tareas)
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
                state.error?.let { errorMsg ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = errorMsg,
                            color = Color.Red,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } ?: TaskListSection(state)
            }
        }

        // Ventana emergente al pulsar FAB
        if (showAddTaskSheet) {
            AddTaskSheet(
                onClose = { showAddTaskSheet = false },
                onAccept = { newTaskData ->
                    viewModel.triggerEvent(
                        TaskIntent.AddTask(
                            name = newTaskData.name,
                            priority = newTaskData.priority,
                            startDate = newTaskData.startDate,
                            endDate = newTaskData.endDate,
                            durationInMinutes = newTaskData.durationInMin,
                            reminders = newTaskData.reminders,
                            repeatConfig = newTaskData.repeatConfig,
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
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "No tasks", // O usar un stringResource
                modifier = Modifier.align(Alignment.Center)
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                TaskSection(
                    title = "Not Completed",
                    tasks = state.notCompletedTasks
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                TaskSection(
                    title = "Completed",
                    tasks = state.completedTasks
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                TaskSection(
                    title = "Expired",
                    tasks = state.expiredTasks
                )
            }
        }
    }
}

@Composable
fun TaskSection(title: String, tasks: List<domain.models.Task>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        tasks.forEach { task ->
            TaskItemComposable(task)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    onClose: () -> Unit,
    onAccept: (NewTaskData) -> Unit
) {
    var taskName by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.NONE) }
    var startDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var endDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var durationInMin by remember { mutableStateOf<Int?>(null) }
    var reminders by remember { mutableStateOf(emptyList<Reminder>()) }
    var repeatConfig by remember { mutableStateOf<RepeatConfig?>(null) }
    var subtasks by remember { mutableStateOf(emptyList<Subtask>()) }



    ModalBottomSheet(
        onDismissRequest = onClose,
        modifier = Modifier
            // 56.dp para no tapar la bottom bar
            .padding(bottom = 105.dp)
            .imePadding()
    ) {
        // Botones de cerrar/aceptar en la parte superior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
            IconButton(
                onClick = {
                    val newTask = NewTaskData(
                        name = taskName,
                        priority = priority,
                        startDate = startDate,
                        endDate = endDate,
                        durationInMin = durationInMin,
                        reminders = reminders,
                        repeatConfig = repeatConfig,
                        subtasks = subtasks
                    )
                    onAccept(newTask)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Accept"
                )
            }
        }

        // Contenido: TextField, íconos, etc.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = taskName,
                onValueChange = { taskName = it },
                placeholder = { Text("Add new task here") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = { /* repetición */ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = "Repeat"
                    )
                }
                IconButton(onClick = { /* prioridad */ }) {
                    Icon(
                        painter = painterResource(R.drawable.priority),
                        contentDescription = "Priority"
                    )
                }
                IconButton(onClick = { /* listas/recordatorios */ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lists),
                        contentDescription = "Reminder"
                    )
                }
                IconButton(onClick = { /* subtasks */ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_subtasks),
                        contentDescription = "Subtask"
                    )
                }
            }
        }
    }
}


@Composable
fun TaskItemComposable(task: domain.models.Task) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = {
                    // Aquí podría dispararse un Intent para marcar la tarea como completada.
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.name, fontSize = 16.sp)
                task.endDate?.let {
                    Text(
                        text = "Ends: $it",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
data class NewTaskData(
    val name: String,
    val priority: Priority = Priority.NONE,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val durationInMin: Int? = null,
    val reminders: List<Reminder> = emptyList(),
    val repeatConfig: RepeatConfig? = null,
    val subtasks: List<Subtask> = emptyList()
)
