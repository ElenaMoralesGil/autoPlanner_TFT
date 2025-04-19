package com.elena.autoplanner.domain.usecases.auth

import com.elena.autoplanner.domain.repository.AuthResult
import com.elena.autoplanner.domain.repository.UserRepository

class DeleteAccountUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(): AuthResult<Unit> = userRepository.deleteAccount()
}
