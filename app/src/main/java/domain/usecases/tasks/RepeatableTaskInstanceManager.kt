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
