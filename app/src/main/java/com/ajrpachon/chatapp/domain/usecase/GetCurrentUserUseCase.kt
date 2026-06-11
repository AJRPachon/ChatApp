package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class GetCurrentUserUseCase(private val userRepository: UserRepository) {
    operator fun invoke(): Flow<UserBO?> = userRepository.getCurrentUser()
}
