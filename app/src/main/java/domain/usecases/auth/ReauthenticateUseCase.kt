package com.elena.autoplanner.domain.usecases.auth

import com.elena.autoplanner.domain.repositories.UserRepository
import com.elena.autoplanner.domain.results.AuthResult

class ReauthenticateUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(password: String): AuthResult<Unit> {
        // Currently just delegates, but could hold more logic later
        return userRepository.reauthenticate(password)
    }
}