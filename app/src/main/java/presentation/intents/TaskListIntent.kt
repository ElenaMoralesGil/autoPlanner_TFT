package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TaskList
import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame
import com.elena.autoplanner.presentation.viewmodel.Intent

sealed class TaskListIntent : Intent {
    data class UpdateStatusFilter(val status: TaskStatus) : TaskListIntent()
    data class UpdateTimeFrameFilter(val timeFrame: TimeFrame) : TaskListIntent()
    data class ToggleTaskCompletion(val taskId: Int, val completed: Boolean) : TaskListIntent()
    data class SelectTask(val taskId: Int) : TaskListIntent()
    data class UpdateTask(val task: Task) : TaskListIntent()
    data class DeleteTask(val taskId: Int) : TaskListIntent()
    data class LoadTasks(val listId: Long) : TaskListIntent()
    data class ViewList(val listId: Long) : TaskListIntent()
    data object ViewAllTasks : TaskListIntent()
    data object RequestEditList : TaskListIntent()
    data class ViewSection(val listId: Long, val sectionId: Long) : TaskListIntent()
    data object RequestEditSections : TaskListIntent()
    data class SaveList(val list: TaskList) : TaskListIntent()
    data class SaveSection(val section: TaskSection) : TaskListIntent()
    data class ArgumentsChanged(val listId: Long?, val sectionId: Long?) : TaskListIntent()
}