package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.UserRepository

class SearchUsersUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(query: String): List<UserBO> =
        userRepository.searchByUsername(query)
}
