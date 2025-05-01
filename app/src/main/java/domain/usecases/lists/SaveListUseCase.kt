package com.elena.autoplanner.domain.usecases.lists

import com.elena.autoplanner.domain.models.TaskList
import com.elena.autoplanner.domain.repositories.ListRepository
import com.elena.autoplanner.domain.results.TaskResult

class SaveListUseCase(private val listRepository: ListRepository) {
    suspend operator fun invoke(list: TaskList): TaskResult<Long> {
        if (list.name.isBlank()) {
            return TaskResult.Error("List name cannot be empty.")
        }
        // Add color validation if needed
        return listRepository.saveList(list)
    }
}