package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.presentation.utils.Intent

sealed class TaskEditIntent : Intent {
    data class LoadTask(val taskId: Int) : TaskEditIntent()
    data class UpdateName(val name: String) : TaskEditIntent()
    data class UpdatePriority(val priority: Priority) : TaskEditIntent()
    data class UpdateStartDateConf(val timePlanning: TimePlanning?) : TaskEditIntent()
    data class UpdateEndDateConf(val timePlanning: TimePlanning?) : TaskEditIntent()
    data class UpdateDuration(val duration: DurationPlan?) : TaskEditIntent()
    data class UpdateReminder(val reminder: ReminderPlan?) : TaskEditIntent()
    data class UpdateRepeat(val repeat: RepeatPlan?) : TaskEditIntent()
    data class AddSubtask(val name: String) : TaskEditIntent()
    data class UpdateSubtask(val subtask: Subtask) : TaskEditIntent()
    data class RemoveSubtask(val subtaskId: Int) : TaskEditIntent()
    object SaveTask : TaskEditIntent()
    object Cancel : TaskEditIntent()
}