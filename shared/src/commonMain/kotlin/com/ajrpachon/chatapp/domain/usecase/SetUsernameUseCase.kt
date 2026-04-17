package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.UserRepository

class SetUsernameUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(userId: String, username: String): Result<UserBO> {
        if (username.length < 3) return Result.failure(IllegalArgumentException("Username too short"))
        if (!username.matches(Regex("^[a-z0-9_]{3,20}$")))
            return Result.failure(IllegalArgumentException("Username: 3-20 chars, lowercase, digits or underscores"))
        if (!userRepository.isUsernameAvailable(username))
            return Result.failure(IllegalStateException("Username already taken"))
        return userRepository.setUsername(userId, username)
    }
}
