package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.utils.NewTaskData

sealed class TaskIntent : BaseIntent() {
    object LoadTasks : TaskIntent()
    data class CreateTask(val newTaskData: NewTaskData) : TaskIntent()
    data class UpdateTask(val task: Task) : TaskIntent()
    data class DeleteTask(val task: Task) : TaskIntent()
    data class ToggleTaskCompletion(val task: Task, val checked: Boolean) : TaskIntent()
    data class UpdateFilter(val filter: TaskFilter) : TaskIntent()
}

enum class TaskFilter(val displayName: String) {
    TODAY("Today"),
    WEEK("Week"),
    MONTH("Month"),
    ALL("All")
}