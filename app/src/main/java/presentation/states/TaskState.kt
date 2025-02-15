package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.domain.models.Task

data class TaskState(
    val tasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val filters: Filters = Filters(),
    val uiState: UiState = UiState.Idle
) {
    data class Filters(
        val status: TaskStatus = TaskStatus.ALL,
        val timeFrame: TimeFrame = TimeFrame.ALL
    )

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String? = null) : UiState()
        data class Error(val message: String? = null) : UiState()
    }
}


enum class TaskSection {
    NOT_DONE,
    COMPLETED,
    EXPIRED
}

enum class TaskStatus(val displayName: String) {
    ALL("All"),
    COMPLETED("Completed"),
    UNCOMPLETED("Uncompleted")
}
enum class TimeFrame(val displayName: String) {
    TODAY("Today"),
    ALL("All Time"),
    WEEK("This Week"),
    MONTH("This Month"),
    EXPIRED("Expired")
}
