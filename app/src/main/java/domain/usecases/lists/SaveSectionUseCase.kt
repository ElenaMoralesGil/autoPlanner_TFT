package com.elena.autoplanner.domain.usecases.lists

import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.domain.repositories.ListRepository
import com.elena.autoplanner.domain.results.TaskResult

class SaveSectionUseCase(private val listRepository: ListRepository) {
    suspend operator fun invoke(section: TaskSection): TaskResult<Long> {
        if (section.name.isBlank()) {
            return TaskResult.Error("Section name cannot be empty.")
        }
        return listRepository.saveSection(section)
    }
}