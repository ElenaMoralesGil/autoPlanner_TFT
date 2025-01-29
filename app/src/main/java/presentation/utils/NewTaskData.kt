package com.elena.autoplanner.presentation.utils

import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning


data class NewTaskData(
    val name: String,
    val priority: Priority = Priority.NONE,
    val startDateConf: TimePlanning? = null,
    val endDateConf: TimePlanning? = null,
    val durationConf: DurationPlan? = null,
    val reminderPlan: ReminderPlan? = null,
    val repeatPlan: RepeatPlan? = null,
    val subtasks: List<Subtask> = emptyList()
)

fun NewTaskData.toTask(): Task {
    return Task(
        name = this.name,
        priority = this.priority,
        startDateConf = this.startDateConf,
        endDateConf = this.endDateConf,
        durationConf = this.durationConf,
        reminderPlan = this.reminderPlan,
        repeatPlan = this.repeatPlan,
        subtasks = this.subtasks
    )
}
