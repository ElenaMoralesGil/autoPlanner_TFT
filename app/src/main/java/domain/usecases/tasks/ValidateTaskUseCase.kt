package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.exceptions.TaskValidationException
import com.elena.autoplanner.domain.models.Task


class ValidateTaskUseCase {
    operator fun invoke(task: Task): Result<Task> {
        return when {
            task.name.isBlank() ->
                Result.failure(TaskValidationException("Task name cannot be empty"))

            task.endDateConf != null && task.startDateConf?.dateTime?.isAfter(task.endDateConf.dateTime) == true ->
                Result.failure(TaskValidationException("Start date must be before end date"))

            task.startDateConf == null && task.endDateConf != null ->
                Result.failure(TaskValidationException("End date requires start date"))

            task.durationConf?.totalMinutes != null && task.durationConf.totalMinutes < 0 ->
                Result.failure(TaskValidationException("Duration cannot be negative"))

            else -> Result.success(task)
        }
    }
}