package com.elena.autoplanner.domain.usecases.lists

import com.elena.autoplanner.domain.models.TaskListInfo
import com.elena.autoplanner.domain.repositories.ListRepository
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.flow.Flow

class GetListsInfoUseCase(private val listRepository: ListRepository) {
    operator fun invoke(): Flow<TaskResult<List<TaskListInfo>>> = listRepository.getListsInfo()
}