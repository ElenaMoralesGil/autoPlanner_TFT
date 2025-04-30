package com.elena.autoplanner.domain.results

sealed class TaskResult<out T> {
    data class Success<T>(val data: T) : TaskResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : TaskResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): TaskResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }

    inline fun <R> fold(
        onSuccess: (T) -> R,
        onError: (String) -> R,
    ): R {
        return when (this) {
            is Success -> onSuccess(data)
            is Error -> onError(message)
        }
    }

    fun isSuccess() = this is Success
    fun isError() = this is Error

}