package presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.usecases.GetTasksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import presentation.states.TaskState

class TaskViewModel(
    private val getTasksUseCase: GetTasksUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TaskState())
    val state: StateFlow<TaskState> = _state

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                getTasksUseCase().collect { tasks ->
                    _state.value = TaskState(
                        notCompletedTasks = tasks.filter { !it.isCompleted && !it.isExpired },
                        completedTasks = tasks.filter { it.isCompleted },
                        expiredTasks = tasks.filter { it.isExpired },
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error loading tasks", e)
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
