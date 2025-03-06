package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task


data class InvalidTaskException(override val message: String) : Exception()


class ValidateTaskUseCase {
    operator fun invoke(task: Task): Result<Task> {
        return when {
            task.name.isBlank() ->
                Result.failure(InvalidTaskException("Task name cannot be empty"))

            task.endDateConf != null && task.startDateConf?.dateTime?.isAfter(task.endDateConf.dateTime) == true ->
                Result.failure(InvalidTaskException("Start date must be before end date"))

            task.startDateConf == null && task.endDateConf != null ->
                Result.failure(InvalidTaskException("End date requires start date"))

            task.durationConf?.totalMinutes != null && task.durationConf.totalMinutes < 0 ->
                Result.failure(InvalidTaskException("Duration cannot be negative"))

            else -> Result.success(task)
        }
    }
}