package com.elena.autoplanner.domain.usecases.planner

import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.ConflictType
import com.elena.autoplanner.domain.models.InfoItem
import com.elena.autoplanner.domain.models.PlanningTask
import com.elena.autoplanner.domain.models.ScheduledTaskItem
import com.elena.autoplanner.domain.models.Task
import java.time.LocalDate

class PlanningContext(initialTasks: List<Task>) {
    val planningTaskMap: MutableMap<Int, PlanningTask> =
        initialTasks.associate { it.id to PlanningTask(it) }.toMutableMap()
    val conflicts: MutableList<ConflictItem> = mutableListOf()
    val expiredForResolution: MutableList<Task> = mutableListOf()
    val postponedTasks: MutableList<Task> = mutableListOf()
    val infoItems: MutableList<InfoItem> = mutableListOf()
    val scheduledItemsMap: MutableMap<LocalDate, MutableList<ScheduledTaskItem>> = mutableMapOf()
    val placedTaskIds: MutableSet<Int> =
        mutableSetOf()

    // ✅ CORRECCIÓN CRÍTICA: Permitir que tareas postponed se procesen para planificación
    fun getTasksToPlan(): Collection<PlanningTask> = planningTaskMap.values.filterNot {
        placedTaskIds.contains(it.id) || it.flags.needsManualResolution
        // Removido: || it.flags.isPostponed
        // Las tareas postponed DEBEN procesarse para ser planificadas automáticamente
    }

    fun addConflict(conflict: ConflictItem, taskIdToMarkHandled: Int?) {
        if (conflicts.none { it.hashCode() == conflict.hashCode() }) {
            conflicts.add(conflict)
        }

        conflict.conflictingTasks.forEach { task ->
            placedTaskIds.add(task.id)

            if (conflict.conflictType == ConflictType.FIXED_VS_FIXED || conflict.conflictType == ConflictType.RECURRENCE_ERROR) {
                planningTaskMap[task.id]?.flags?.isHardConflict = true
            }
        }

        taskIdToMarkHandled?.let { placedTaskIds.add(it) }
    }

    fun addScheduledItem(item: ScheduledTaskItem, taskId: Int) {
        scheduledItemsMap.computeIfAbsent(item.date) { mutableListOf() }.add(item)
        placedTaskIds.add(taskId)
    }

    fun addInfoItem(info: InfoItem) {
        if (infoItems.none { it.task?.id == info.task?.id && it.message == info.message }) {
            infoItems.add(info)
        }
    }

    // ✅ CORRECCIÓN CRÍTICA: No marcar tareas postponed como "placed" automáticamente
    fun addPostponedTask(task: Task) {
        if (postponedTasks.none { it.id == task.id }) {
            postponedTasks.add(task)
        }

    }

    fun addExpiredForManualResolution(task: Task) {
        if (expiredForResolution.none { it.id == task.id }) {
            expiredForResolution.add(task)
        }

        planningTaskMap[task.id]?.flags?.needsManualResolution = true
        placedTaskIds.add(task.id)
    }
}