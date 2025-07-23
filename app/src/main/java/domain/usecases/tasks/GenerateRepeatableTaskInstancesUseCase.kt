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
        val existingInstances = instanceDao.getInstancesForTask(parentTaskId)
        val existingDates = existingInstances.map { it.scheduledDateTime }

        for (i in 0 until repeatCount) {
            val instanceDate = startDate.plusWeeks(i.toLong())
            if (instanceDate !in existingDates) {
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
}
