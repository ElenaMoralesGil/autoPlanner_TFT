package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.presentation.utils.Intent

sealed class TaskDetailIntent : Intent {
    data class LoadTask(val taskId: Int) : TaskDetailIntent()
    data class ToggleCompletion(val completed: Boolean) : TaskDetailIntent()
    object DeleteTask : TaskDetailIntent()
    data class AddSubtask(val name: String) : TaskDetailIntent()
    data class ToggleSubtask(val subtaskId: Int, val completed: Boolean) : TaskDetailIntent()
    data class DeleteSubtask(val subtaskId: Int) : TaskDetailIntent()
    object EditTask : TaskDetailIntent()
}