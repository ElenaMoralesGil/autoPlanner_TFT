package com.elena.autoplanner.domain.usecases.lists
import com.elena.autoplanner.domain.repositories.ListRepository
import com.elena.autoplanner.domain.results.TaskResult

class DeleteListUseCase(private val listRepository: ListRepository) {
    suspend operator fun invoke(listId: Long): TaskResult<Unit> =
        listRepository.deleteList(listId)
}