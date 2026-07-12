package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observes the currently authenticated user.
 *
 * Business rule: de-duplicates consecutive identical emissions so that UI layers
 * are not re-rendered on every repository poll when the user object has not
 * actually changed. A null value signals that no session is active and callers
 * should redirect to the authentication flow.
 */
class GetCurrentUserUseCase(private val userRepository: UserRepository) {
    operator fun invoke(): Flow<UserBO?> =
        userRepository.getCurrentUser().distinctUntilChanged()
}
