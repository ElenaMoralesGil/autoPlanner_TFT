package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.exceptions.TaskValidationException
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskResult


class ValidateTaskUseCase {
    operator fun invoke(task: Task): TaskResult<Task> {
        return when {
            task.name.isBlank() ->
                TaskResult.Error("Task name cannot be empty")

            task.endDateConf != null && task.startDateConf?.dateTime?.isAfter(task.endDateConf.dateTime) == true ->
                TaskResult.Error("Start date must be before end date")

            task.startDateConf == null && task.endDateConf != null ->
                TaskResult.Error("End date requires start date")

            task.durationConf?.totalMinutes != null && task.durationConf.totalMinutes < 0 ->
                TaskResult.Error("Duration cannot be negative")

            else -> TaskResult.Success(task)
        }
    }
}