package com.elena.autoplanner.presentation.intents

import domain.models.Priority
import domain.models.Subtask
import domain.models.TimePlanning
import domain.models.DurationPlan
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan

sealed class TaskIntent : BaseIntent() {

    object LoadTasks : TaskIntent()

    /**
     * Creates a new Task using the new domain model approach.
     * We now have 'TimePlanning' for start & end,
     * a 'DurationPlan' for total minutes,
     * a 'ReminderPlan' for reminders,
     * and a 'RepeatPlan' for repeats.
     */
    data class AddTask(
        val name: String,
        val priority: Priority = Priority.NONE,

        val startDateConf: TimePlanning? = null,
        val endDateConf: TimePlanning? = null,
        val durationConf: DurationPlan? = null,

        val reminderPlan: ReminderPlan? = null,
        val repeatPlan: RepeatPlan? = null,

        val subtasks: List<Subtask> = emptyList()
    ) : TaskIntent()
}
