package com.elena.autoplanner.domain.usecases.lists

import com.elena.autoplanner.domain.repositories.ListRepository
import com.elena.autoplanner.domain.results.TaskResult

class DeleteSectionUseCase(private val listRepository: ListRepository) {
    suspend operator fun invoke(sectionId: Long): TaskResult<Unit> =
        listRepository.deleteSection(sectionId)
}