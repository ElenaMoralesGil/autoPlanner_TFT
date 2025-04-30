package com.elena.autoplanner.domain.usecases.auth

import com.elena.autoplanner.domain.results.AuthResult
import com.elena.autoplanner.domain.repositories.UserRepository

class DeleteAccountUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(): AuthResult<Unit> = userRepository.deleteAccount()
}
