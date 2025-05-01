package com.elena.autoplanner.domain.usecases.lists

import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.domain.repositories.ListRepository
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.flow.Flow

class GetSectionsUseCase(private val listRepository: ListRepository) {
    operator fun invoke(listId: Long): Flow<TaskResult<List<TaskSection>>> =
        listRepository.getSections(listId)
}