package com.elena.autoplanner.domain.usecases.lists

import com.elena.autoplanner.domain.models.TaskList
import com.elena.autoplanner.domain.repositories.ListRepository
import com.elena.autoplanner.domain.results.TaskResult

class GetAllListsUseCase(private val listRepository: ListRepository) {
    suspend operator fun invoke(): TaskResult<List<TaskList>> = listRepository.getAllLists()
}