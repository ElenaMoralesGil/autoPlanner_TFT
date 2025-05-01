package com.elena.autoplanner.domain.usecases.lists

import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.domain.repositories.ListRepository
import com.elena.autoplanner.domain.results.TaskResult

class GetAllSectionsUseCase(private val listRepository: ListRepository) {
    suspend operator fun invoke(listId: Long): TaskResult<List<TaskSection>> =
        listRepository.getAllSections(listId)
}