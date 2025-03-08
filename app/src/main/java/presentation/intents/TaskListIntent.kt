package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame
import com.elena.autoplanner.presentation.viewmodel.Intent

sealed class TaskListIntent : Intent {
    object LoadTasks : TaskListIntent()
    data class UpdateStatusFilter(val status: TaskStatus) : TaskListIntent()
    data class UpdateTimeFrameFilter(val timeFrame: TimeFrame) : TaskListIntent()
    data class ToggleTaskCompletion(val taskId: Int, val completed: Boolean) : TaskListIntent()
    data class SelectTask(val taskId: Int) : TaskListIntent()
    data class UpdateTask(val taskId: Task) : TaskListIntent()
    data class DeleteTask(val taskId: Int) : TaskListIntent()
}