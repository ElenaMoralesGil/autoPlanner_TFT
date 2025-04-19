package com.elena.autoplanner.domain.repository

import com.elena.autoplanner.domain.models.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUser(): Flow<User?>
    suspend fun login(email: String, password: String): AuthResult<User>
    suspend fun register(email: String, password: String): AuthResult<User>
    suspend fun logout()
    suspend fun deleteAccount(): AuthResult<Unit>
}