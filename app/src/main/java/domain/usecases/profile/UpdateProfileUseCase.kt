package com.elena.autoplanner.domain.usecases.profile

import com.elena.autoplanner.domain.repositories.UserRepository
import com.elena.autoplanner.domain.results.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateProfileUseCase(private val userRepository: UserRepository) {

    suspend operator fun invoke(
        newDisplayName: String?,
        newEmail: String?,
        newPassword: String?,     
    ): AuthResult<Unit> = withContext(Dispatchers.IO) {

        val results = mutableListOf<AuthResult<Unit>>()

        if (newDisplayName != null) {
            results.add(userRepository.updateDisplayName(newDisplayName))
        }

        if (newEmail != null) {
            results.add(userRepository.updateEmail(newEmail))
        }

        if (newPassword != null) {
            results.add(userRepository.updatePassword(newPassword))
        }

        val firstError = results.filterIsInstance<AuthResult.Error>().firstOrNull()

        if (firstError != null) {

            if (firstError.message.contains("Re-authentication required")) {
                AuthResult.Error("Re-authentication required.")
            } else {
                firstError 
            }
        } else if (results.any { it is AuthResult.Success }) {

            AuthResult.Success(Unit)
        } else {

            AuthResult.Error("No profile changes were applied.")
        }
    }
}