package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.UserRepository

/**
 * Searches users by username fragment.
 *
 * Business rule: a blank [query] (whitespace-only or empty) is treated as a
 * no-op and returns an empty list without hitting the repository, avoiding
 * unnecessary network round-trips for empty search inputs.
 */
class SearchUsersUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(query: String): List<UserBO> {
        if (query.isBlank()) return emptyList()
        return userRepository.searchByUsername(query)
    }
}
