package com.elena.autoplanner.domain.usecases.profile

import com.elena.autoplanner.domain.repositories.UserRepository
import com.elena.autoplanner.domain.results.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateProfileUseCase(private val userRepository: UserRepository) {

    // Returns a combined result. Individual errors might occur.
    suspend operator fun invoke(
        newDisplayName: String?, // Nullable if not changing
        newEmail: String?,       // Nullable if not changing
        newPassword: String?,     // Nullable if not changing
    ): AuthResult<Unit> = withContext(Dispatchers.IO) {

        val results = mutableListOf<AuthResult<Unit>>()

        // Update Display Name if provided
        if (newDisplayName != null) {
            results.add(userRepository.updateDisplayName(newDisplayName))
        }

        // Update Email if provided
        if (newEmail != null) {
            results.add(userRepository.updateEmail(newEmail))
        }

        // Update Password if provided
        if (newPassword != null) {
            results.add(userRepository.updatePassword(newPassword))
        }

        // Check for any errors
        val firstError = results.filterIsInstance<AuthResult.Error>().firstOrNull()

        if (firstError != null) {
            // Prioritize re-authentication errors
            if (firstError.message.contains("Re-authentication required")) {
                AuthResult.Error("Re-authentication required.")
            } else {
                firstError // Return the first specific error encountered
            }
        } else if (results.any { it is AuthResult.Success }) {
            // If at least one operation was successful (and no errors)
            AuthResult.Success(Unit)
        } else {
            // No changes attempted or an unexpected state
            AuthResult.Error("No profile changes were applied.")
        }
    }
}