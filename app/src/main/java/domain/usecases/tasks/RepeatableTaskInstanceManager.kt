package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.data.dao.RepeatableTaskInstanceDao
import com.elena.autoplanner.domain.models.RepeatableTaskInstance
import java.time.LocalDateTime

class RepeatableTaskInstanceManager(private val instanceDao: RepeatableTaskInstanceDao) {
    suspend fun getInstancesForTask(parentTaskId: Int): List<RepeatableTaskInstance> {
        return instanceDao.getInstancesForTask(parentTaskId)
    }

    suspend fun deleteInstanceByIdentifier(instanceIdentifier: String) {
        instanceDao.deleteInstanceByIdentifier(instanceIdentifier)
    }

    suspend fun reloadInstances(parentTaskId: Int): List<RepeatableTaskInstance> {
        return getInstancesForTask(parentTaskId)
    }

    suspend fun getInstancesByIdentifier(instanceIdentifier: String): List<RepeatableTaskInstance> {
        return instanceDao.getInstancesForIdentifier(instanceIdentifier)
    }

    /**
     * Actualiza las instancias futuras de una tarea repetida cuando se modifica la configuración de repetición.
     * @param parentTaskId ID de la tarea principal repetida
     * @param newRepeatConfig Nueva configuración de repetición
     * @param fromDate Fecha desde la cual actualizar las instancias (normalmente ahora)
     */
    suspend fun updateFutureInstances(
        parentTaskId: Int,
        newRepeatConfig: Any,
        fromDate: LocalDateTime,
    ) {
        // Obtiene las instancias futuras
        val fromDateString = fromDate.toString() // Formato ISO
        val futureInstances: List<RepeatableTaskInstance> =
            instanceDao.getFutureInstancesForTask(parentTaskId, fromDateString)
        // Elimina las instancias futuras
        for (instance in futureInstances) {
            instanceDao.deleteInstanceByIdentifier(instance.instanceIdentifier)
        }
        // Genera nuevas instancias según la nueva configuración
        // (Supón que existe un método para esto, si no, implementa la lógica aquí)
        // Por ejemplo: instanceDao.generateInstances(parentTaskId, newRepeatConfig, fromDateString)
    }

    // Llama a reloadInstances automáticamente al abrir la pantalla o tras eliminar una instancia
    // Ejemplo de uso en el ViewModel:
    // fun refreshInstances() {
    //     viewModelScope.launch {
    //         repeatableInstances = repeatableTaskInstanceManager.reloadInstances(taskId)
    //         setState { copy() }
    //     }
    // }
    // Puedes llamar a refreshInstances() desde la UI en un LaunchedEffect.
}
