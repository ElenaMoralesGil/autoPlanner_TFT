package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.tasks.GetTasksUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

abstract class BaseTaskViewModel<I : Intent, S, E : UiEffect> : BaseViewModel<I, S, E>() {

    protected fun loadTasks(
        getTasksUseCase: GetTasksUseCase,
        setLoadingState: (Boolean) -> Unit,
        processResult: (List<Task>) -> Unit,
        handleError: (Throwable) -> Unit,
    ) {
        viewModelScope.launch {
            setLoadingState(true)

            getTasksUseCase()
                .catch { error ->
                    setLoadingState(false)
                    handleError(error)
                }
                .collect { tasks ->
                    setLoadingState(false)
                    processResult(tasks)
                }
        }
    }

    protected fun <T> handleTaskResult(
        result: TaskResult<T>,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit,
    ) {
        when (result) {
            is TaskResult.Success -> onSuccess(result.data)
            is TaskResult.Error -> onError(result.message)
        }
    }

    protected suspend fun <T> executeTaskOperation(
        setLoadingState: (Boolean) -> Unit,
        operation: suspend () -> TaskResult<T>,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit,
    ) {
        setLoadingState(true)

        val result = operation()

        setLoadingState(false)
        handleTaskResult(result, onSuccess, onError)
    }
}