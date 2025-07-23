package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.presentation.viewmodel.Intent

sealed class TaskDetailIntent : Intent {
    data class LoadTask(val taskId: Int, val instanceIdentifier: String? = null) :
        TaskDetailIntent()
    data class ToggleCompletion(val completed: Boolean) : TaskDetailIntent()
    object DeleteTask : TaskDetailIntent()
    data class AddSubtask(val name: String) : TaskDetailIntent()
    data class ToggleSubtask(val subtaskId: Int, val completed: Boolean) : TaskDetailIntent()
    data class DeleteSubtask(val subtaskId: Int) : TaskDetailIntent()
    object EditTask : TaskDetailIntent()
    data class DeleteRepeatableTask(
        val instanceIdentifier: String,
        val deleteType: RepeatableDeleteType,
    ) : TaskDetailIntent()
    data class EditRepeatableTask(
        val newTask: com.elena.autoplanner.domain.models.Task,
        val newRepeatConfig: Any?,
        val fromDate: java.time.LocalDateTime? = null,
    ) : TaskDetailIntent()

    data class UpdateRepeatableTaskInstances(
        val newRepeatConfig: Any,
        val fromDate: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    ) : TaskDetailIntent()
}

// Tipo de borrado para tareas repetidas
enum class RepeatableDeleteType {
    INSTANCE,
    FUTURE,
    ALL
}