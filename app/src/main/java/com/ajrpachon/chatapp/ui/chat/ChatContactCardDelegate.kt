package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.domain.model.UserRelationship
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.SendInvitationResult
import com.ajrpachon.chatapp.domain.usecase.SendInvitationUseCase
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "ChatContactCardDelegate"

/**
 * Handles the shared-contact-card lookups keyed by phone (see [ContactPhoneLookup]'s doc):
 * resolving whether the phone number belongs to an existing user and the current user's
 * relationship to them, and the card's primary action (invite / accept / navigate to the
 * existing chat, depending on that relationship). Sixth slice of the decomposition in
 * docs/chat-viewmodel-decomposition.md — see [ChatAiDelegate] for the pattern this follows.
 */
class ChatContactCardDelegate(
    private val currentUserId: () -> String?,
    private val userRepository: UserRepository,
    private val sendInvitationUseCase: SendInvitationUseCase,
    private val scope: CoroutineScope,
    private val context: ChatDelegateContext,
) {
    private val getState get() = context.getState
    private val updateState get() = context.updateState
    private val sendEffect get() = context.sendEffect

    fun checkContactRelationship(phone: String) {
        if (phone.isBlank() || getState().contactCard.lookups.containsKey(phone)) return
        val currentId = currentUserId() ?: return
        updateState {
            it.copy(contactCard = it.contactCard.copy(lookups = it.contactCard.lookups + (phone to ContactPhoneLookup(isLoading = true))))
        }
        scope.launch {
            val lookup = catchResult {
                val user = userRepository.findUserByPhone(phone)
                val relationship = user?.let { sendInvitationUseCase.checkRelationship(currentId, it.id) }
                ContactPhoneLookup(resolvedUser = user, relationship = relationship)
            }.getOrDefault(ContactPhoneLookup())
            updateState { it.copy(contactCard = it.contactCard.copy(lookups = it.contactCard.lookups + (phone to lookup))) }
        }
    }

    fun contactCardPrimaryAction(phone: String) {
        val lookup = getState().contactCard.lookups[phone]
        val resolvedUser = lookup?.resolvedUser
        if (resolvedUser == null) {
            val text = "¡Únete a ChatApp y hablamos! 💬"
            scope.launch { sendEffect(ChatEffect.InviteContact(phone, text)) }
            return
        }
        scope.launch {
            when (val result = sendInvitationUseCase(resolvedUser)) {
                is SendInvitationResult.Sent -> {
                    updateState {
                        it.copy(contactCard = it.contactCard.copy(lookups = it.contactCard.lookups + (phone to lookup.copy(relationship = UserRelationship.PENDING_SENT))))
                    }
                    sendEffect(ChatEffect.ShowSnackbar("¡Invitación enviada!"))
                }
                is SendInvitationResult.AlreadySent -> {
                    updateState {
                        it.copy(contactCard = it.contactCard.copy(lookups = it.contactCard.lookups + (phone to lookup.copy(relationship = UserRelationship.PENDING_SENT))))
                    }
                    sendEffect(ChatEffect.ShowSnackbar("Invitación enviada · Pendiente de respuesta"))
                }
                is SendInvitationResult.PendingReceived -> {
                    updateState {
                        it.copy(contactCard = it.contactCard.copy(lookups = it.contactCard.lookups + (phone to lookup.copy(relationship = UserRelationship.PENDING_RECEIVED))))
                    }
                    sendEffect(ChatEffect.ShowSnackbar("Ya tienes una invitación pendiente de esta persona"))
                }
                is SendInvitationResult.NavigateToChat -> {
                    updateState {
                        it.copy(contactCard = it.contactCard.copy(lookups = it.contactCard.lookups + (phone to lookup.copy(relationship = UserRelationship.CONNECTED))))
                    }
                    sendEffect(ChatEffect.NavigateToConversation(result.conversationId, result.name))
                }
                is SendInvitationResult.Blocked -> {
                    updateState {
                        it.copy(contactCard = it.contactCard.copy(lookups = it.contactCard.lookups + (phone to lookup.copy(relationship = UserRelationship.BLOCKED))))
                    }
                    sendEffect(ChatEffect.ShowSnackbar("No puedes enviar una invitación a este contacto"))
                }
                is SendInvitationResult.Failure -> {
                    AppLogger.e(TAG, "contactCardPrimaryAction failed: ${result.message}")
                    sendEffect(ChatEffect.ShowSnackbar(result.message))
                }
            }
        }
    }
}
