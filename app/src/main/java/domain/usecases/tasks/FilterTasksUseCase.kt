package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame

class FilterTasksUseCase {
    operator fun invoke(
        tasks: List<Task>,
        status: TaskStatus,
        timeFrame: TimeFrame,
    ): List<Task> {
        return tasks.filter { task ->

            val statusMatches = when (status) {
                TaskStatus.ALL -> true
                TaskStatus.COMPLETED -> task.isCompleted
                TaskStatus.UNCOMPLETED -> !task.isCompleted
            }

            val timeFrameMatches = when (timeFrame) {
                TimeFrame.ALL -> true
                TimeFrame.TODAY -> task.isDueToday()
                TimeFrame.WEEK -> task.isDueThisWeek()
                TimeFrame.MONTH -> task.isDueThisMonth()
                TimeFrame.EXPIRED -> task.isExpired()
            }

            statusMatches && timeFrameMatches
        }
    }
}
