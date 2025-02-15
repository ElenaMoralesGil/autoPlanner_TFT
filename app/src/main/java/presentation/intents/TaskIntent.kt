package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame
import com.elena.autoplanner.presentation.utils.NewTaskData

sealed class TaskIntent : BaseIntent() {
    object LoadTasks : TaskIntent()
    data class CreateTask(val newTaskData: NewTaskData) : TaskIntent()
    data class UpdateTask(val task: Task) : TaskIntent()
    data class DeleteTask(val task: Int) : TaskIntent()
    data class ToggleTaskCompletion(val task: Task, val checked: Boolean) : TaskIntent()
    data class UpdateStatusFilter(val status: TaskStatus) : TaskIntent()
    data class UpdateTimeFrameFilter(val timeFrame: TimeFrame) : TaskIntent()
    data class AddSubtask(val task: Int, val subtaskName: String) : TaskIntent()
    data class ToggleSubtask(val task: Int, val subtask: Int, val checked: Boolean) : TaskIntent()
    data class DeleteSubtask(val task: Int, val subtask: Int) : TaskIntent()
    object ClearError : TaskIntent()
}
