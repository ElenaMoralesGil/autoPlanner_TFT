package com.elena.autoplanner.domain.repositories

import com.elena.autoplanner.domain.models.User
import com.elena.autoplanner.domain.results.AuthResult
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUser(): Flow<User?>
    suspend fun login(email: String, password: String): AuthResult<User>
    suspend fun register(email: String, password: String): AuthResult<User>
    suspend fun logout()
    suspend fun deleteAccount(): AuthResult<Unit>

    suspend fun reauthenticate(password: String): AuthResult<Unit>

    suspend fun updateEmail(newEmail: String): AuthResult<Unit>
    suspend fun updatePassword(newPassword: String): AuthResult<Unit>
    suspend fun updateDisplayName(newDisplayName: String): AuthResult<Unit>
}