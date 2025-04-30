package com.elena.autoplanner.domain.utils

import com.elena.autoplanner.domain.results.TaskResult

object ResultConverter {

    fun <T> Result<T>.toTaskResult(): TaskResult<T> {
        return fold(
            onSuccess = { TaskResult.Success(it) },
            onFailure = { TaskResult.Error(it.message ?: "Unknown error", it) }
        )
    }

    fun <T> TaskResult<T>.toResult(): Result<T> {
        return when (this) {
            is TaskResult.Success -> Result.success(data)
            is TaskResult.Error -> Result.failure(exception ?: RuntimeException(message))
        }
    }
}