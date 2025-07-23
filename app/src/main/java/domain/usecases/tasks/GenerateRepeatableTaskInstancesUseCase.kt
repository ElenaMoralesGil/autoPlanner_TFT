package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.data.dao.RepeatableTaskInstanceDao
import com.elena.autoplanner.domain.models.RepeatableTaskInstance
import java.time.LocalDateTime
import java.util.UUID

class GenerateRepeatableTaskInstancesUseCase(private val instanceDao: RepeatableTaskInstanceDao) {
    suspend fun generateWeeklyInstances(
        parentTaskId: Int,
        startDate: LocalDateTime,
        repeatCount: Int,
    ) {
        for (i in 0 until repeatCount) {
            val instanceDate = startDate.plusWeeks(i.toLong())
            val instanceIdentifier = UUID.randomUUID().toString()
            val instance = RepeatableTaskInstance(
                parentTaskId = parentTaskId,
                instanceIdentifier = instanceIdentifier,
                scheduledDateTime = instanceDate,
                isCompleted = false
            )
            instanceDao.insertInstance(instance)
        }
    }
}

