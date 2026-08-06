package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.ContactBO
import com.ajrpachon.chatapp.domain.repository.ContactRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetDeviceContactsUseCaseTest {

    private val contactRepository = mockk<ContactRepository>()
    private val useCase = GetDeviceContactsUseCase(contactRepository)

    @Test
    fun `delegates to repository and returns its contacts`() = runTest {
        val contacts = listOf(mockk<ContactBO>(relaxed = true))
        coEvery { contactRepository.getContacts() } returns contacts

        val result = useCase()

        assertEquals(contacts, result)
    }

    @Test
    fun `returns empty list when repository has no contacts`() = runTest {
        coEvery { contactRepository.getContacts() } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }
}
