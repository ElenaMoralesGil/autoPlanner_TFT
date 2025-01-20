package presentation.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.presentation.intents.TaskIntent
import com.elena.autoplanner.presentation.utils.BaseViewModel
import domain.usecases.AddTaskUseCase
import domain.usecases.GetTasksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import presentation.states.TaskState
import domain.models.Task

class TaskViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val addTaskUseCase: AddTaskUseCase
) : BaseViewModel<TaskIntent>() {

    // StateFlow para la parte específica de Tareas
    private val _state = MutableStateFlow(TaskState())
    val state: StateFlow<TaskState> = _state



    override fun onTriggerEvent(intent: TaskIntent) {
        when (intent) {
            is TaskIntent.LoadTasks -> {
                loadAllTasks()
            }
            is TaskIntent.AddTask -> {
                addNewTask(intent)
            }
        }
    }

    private fun loadAllTasks() {
        viewModelScope.launch {
            try {
                // Primero se pone isLoading en true
                _state.value = _state.value.copy(isLoading = true)

                Log.d("TaskViewModel", "=== loadAllTasks(): About to collect from getTasksUseCase() ===")

                getTasksUseCase().collect { allTasks ->
                    Log.d("TaskViewModel", "=== loadAllTasks(): Received tasks -> ${allTasks.size} items ===")

                    val notCompleted = allTasks.filter { !it.isCompleted && !it.isExpired }
                    val completed = allTasks.filter { it.isCompleted }
                    val expired = allTasks.filter { it.isExpired }

                    _state.value = _state.value.copy(
                        notCompletedTasks = notCompleted,
                        completedTasks = completed,
                        expiredTasks = expired,
                        isLoading = false,
                        error = null,
                        taskCreatedSuccessfully = false
                    )
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error loading tasks", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun addNewTask(intent: TaskIntent.AddTask) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true)

                val newTask = Task(
                    name = intent.name,
                    priority = intent.priority,
                    startDateConf = intent.startDateConf,
                    endDateConf = intent.endDateConf,
                    durationConf = intent.durationConf,
                    reminderPlan = intent.reminderPlan,
                    repeatPlan = intent.repeatPlan,
                    subtasks = intent.subtasks
                )

                addTaskUseCase(newTask)

                loadAllTasks()

                // Señal de tarea creada
                _state.value = _state.value.copy(
                    taskCreatedSuccessfully = true
                )
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error adding task", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}
