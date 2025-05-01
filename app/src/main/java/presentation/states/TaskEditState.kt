package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.TaskList
import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.domain.models.TimePlanning

data class TaskEditState(
    val isLoading: Boolean = false,
    val isNewTask: Boolean = true,
    val taskId: Int = 0,
    val name: String = "",
    val priority: Priority = Priority.NONE,
    val startDateConf: TimePlanning? = null,
    val endDateConf: TimePlanning? = null,
    val durationConf: DurationPlan? = null,
    val reminderPlan: ReminderPlan? = null,
    val repeatPlan: RepeatPlan? = null,
    val subtasks: List<Subtask> = emptyList(),
    val error: String? = null,
    val listId: Long? = null,
    val sectionId: Long? = null,
    val isLoadingSelection: Boolean = false, // For loading lists/sections
    val availableLists: List<TaskList> = emptyList(),
    val availableSections: List<TaskSection> = emptyList(),
)
