package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.ContactBO
import com.ajrpachon.chatapp.domain.repository.ContactRepository

class GetDeviceContactsUseCase(
    private val contactRepository: ContactRepository,
) {
    suspend operator fun invoke(): List<ContactBO> = contactRepository.getContacts()
}
