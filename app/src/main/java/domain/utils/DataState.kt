package com.elena.autoplanner.domain.utils

sealed class DataState<out T> {
    data class Success<out T>(val data: T) : DataState<T>()
    data class Error(val exception: Exception) : DataState<Nothing>()
    object Loading : DataState<Nothing>()

    object Empty : DataState<Nothing>()
}