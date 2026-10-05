package com.ajrpachon.chatapp.data.repository

import android.app.Application
import android.content.ContentResolver
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import com.ajrpachon.chatapp.domain.model.ContactBO
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ContactRepositoryImplTest {

    private val resolver = mockk<ContentResolver>()
    private val repo = ContactRepositoryImpl(resolver)

    private val name = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
    private val number = ContactsContract.CommonDataKinds.Phone.NUMBER

    private fun phoneCursor(vararg rows: Array<Any?>) =
        MatrixCursor(arrayOf(name, number)).apply { rows.forEach { addRow(it) } }

    private fun givenPhoneRows(vararg rows: Array<Any?>) {
        every {
            resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, any(), null, null, any())
        } returns phoneCursor(*rows)
    }

    @Test
    fun `getContacts returns an empty list when the provider gives no cursor`() = runTest {
        every {
            resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, any(), null, null, any())
        } returns null

        assertEquals(emptyList<ContactBO>(), repo.getContacts())
    }

    @Test
    fun `getContacts trims numbers and skips rows missing a name or a number`() = runTest {
        givenPhoneRows(
            arrayOf("Ana", " 600111222 "),
            arrayOf(null, "600333444"),
            arrayOf("Bruno", null),
            arrayOf("Carla", "600555666"),
        )

        assertEquals(
            listOf(ContactBO("Ana", "600111222"), ContactBO("Carla", "600555666")),
            repo.getContacts(),
        )
    }

    @Test
    fun `getContacts keeps only the first contact for each phone number`() = runTest {
        givenPhoneRows(
            arrayOf("Ana", "600111222"),
            arrayOf("Ana (trabajo)", "600111222"),
        )

        assertEquals(listOf(ContactBO("Ana", "600111222")), repo.getContacts())
    }

    private fun givenContact(uri: Uri, displayName: String?, id: String?) {
        val columns = arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID)
        every { resolver.query(uri, null, null, null, null) } returns
            MatrixCursor(columns).apply { addRow(arrayOf(displayName, id)) }
    }

    private fun givenPhoneFor(contactId: String, phone: String?) {
        every {
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(number),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null,
            )
        } returns MatrixCursor(arrayOf(number)).apply { if (phone != null) addRow(arrayOf(phone)) }
    }

    @Test
    fun `getContactByUri combines the name with the first phone number`() = runTest {
        val uri = Uri.parse("content://contacts/42")
        givenContact(uri, "Ana", "42")
        givenPhoneFor("42", "600111222")

        assertEquals(ContactBO("Ana", "600111222"), repo.getContactByUri(uri.toString()))
    }

    @Test
    fun `getContactByUri returns a contact with only a name when it has no phone`() = runTest {
        val uri = Uri.parse("content://contacts/42")
        givenContact(uri, "Ana", "42")
        givenPhoneFor("42", null)

        assertEquals(ContactBO("Ana", ""), repo.getContactByUri(uri.toString()))
    }

    @Test
    fun `getContactByUri returns null when the contact has neither a name nor a phone`() = runTest {
        val uri = Uri.parse("content://contacts/42")
        givenContact(uri, "", "42")
        givenPhoneFor("42", null)

        assertNull(repo.getContactByUri(uri.toString()))
    }

    @Test
    fun `getContactByUri returns null when the provider has no such contact`() = runTest {
        val uri = Uri.parse("content://contacts/7")
        every { resolver.query(uri, null, null, null, null) } returns null

        assertNull(repo.getContactByUri(uri.toString()))
    }
}
