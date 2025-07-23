package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.data.dao.RepeatableTaskInstanceDao
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.models.RepeatableTaskInstance
import com.elena.autoplanner.domain.models.Task
import java.time.LocalDateTime

class RepeatableTaskInstanceManager(private val instanceDao: RepeatableTaskInstanceDao) {
    suspend fun getInstancesForTask(parentTaskId: Int): List<RepeatableTaskInstance> {
        return instanceDao.getInstancesForTask(parentTaskId)
    }

    suspend fun deleteInstanceByIdentifier(instanceIdentifier: String) {
        instanceDao.markInstancesDeletedByIdentifier(instanceIdentifier)
    }

    suspend fun reloadInstances(parentTaskId: Int): List<RepeatableTaskInstance> {
        return getInstancesForTask(parentTaskId)
    }

    suspend fun getInstancesByIdentifier(instanceIdentifier: String): List<RepeatableTaskInstance> {
        return instanceDao.getInstancesForIdentifier(instanceIdentifier)
    }

    suspend fun getInstancesForIdentifier(instanceIdentifier: String): List<RepeatableTaskInstance> {
        return instanceDao.getInstancesForIdentifier(instanceIdentifier)
    }

    /**
     * Actualiza las instancias futuras de una tarea repetida cuando se modifica la configuración de repetición.
     * Este método ahora solo elimina las instancias futuras, sin regenerarlas.
     * @param parentTaskId ID de la tarea principal repetida
     * @param fromDate Fecha desde la cual eliminar las instancias (normalmente ahora)
     */
    suspend fun updateFutureInstances(
        parentTaskId: Int,
        fromDate: LocalDateTime,
    ) {
        val fromDateString = fromDate.toString()
        val futureInstances: List<RepeatableTaskInstance> =
            instanceDao.getFutureInstancesForTask(parentTaskId, fromDateString)
        for (instance in futureInstances) {
            instanceDao.markInstancesDeletedByIdentifier(instance.instanceIdentifier)
        }
        // La regeneración de instancias se ha eliminado para cumplir con el requisito.
    }

    /**
     * Genera instancias de una tarea repetible dentro de un rango de fechas.
     * @param parentTaskId ID de la tarea principal repetible.
     * @param startDate Fecha de inicio del rango.
     * @param endDate Fecha de fin del rango.
     * @return Lista de instancias generadas.
     */
    suspend fun generateInstancesForRange(
        parentTask: Task,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<RepeatableTaskInstance> {

        val repeatPlan = parentTask.repeatPlan
            ?: return emptyList() // Confirmado que `repeatPlan` existe en la clase `Task`

        val generatedInstances = mutableListOf<RepeatableTaskInstance>()
        var currentDate = startDate

        while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
            if (repeatPlan.shouldGenerateInstanceOn(currentDate)) {
                val instance = RepeatableTaskInstance(
                    parentTaskId = parentTask.id,
                    scheduledDateTime = currentDate, // Corregido el parámetro
                    instanceIdentifier = "${parentTask.id}_${currentDate.toLocalDate()}"
                )
                generatedInstances.add(instance)
            }
            currentDate = currentDate.plusDays(1) // Incrementar la fecha
        }

        // Guardar las instancias generadas
        for (instance in generatedInstances) {
            instanceDao.insertInstance(instance) // Usar el método insertInstance
        }

        return generatedInstances
    }

    fun getInstanceDao(): RepeatableTaskInstanceDao = instanceDao
}
