package com.ajrpachon.chatapp.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import com.ajrpachon.chatapp.domain.model.ContactBO
import com.ajrpachon.chatapp.domain.repository.ContactRepository

class ContactRepositoryImpl(
    private val contentResolver: ContentResolver,
) : ContactRepository {

    override suspend fun getContacts(): List<ContactBO> {
        val contacts = mutableListOf<ContactBO>()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
        ) ?: return contacts

        cursor.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameIdx) ?: continue
                val number = it.getString(numIdx) ?: continue
                contacts.add(ContactBO(name, number.trim()))
            }
        }
        return contacts.distinctBy { it.phoneNumber }
    }

    override suspend fun getContactByUri(uri: String): ContactBO? {
        val (name, contactId) = readNameAndId(Uri.parse(uri)) ?: return null
        val phone = if (contactId != null) readPhoneNumber(contactId) else ""
        return if (name.isNotBlank() || phone.isNotBlank()) ContactBO(name, phone) else null
    }

    private fun readNameAndId(uri: Uri): Pair<String, String?>? {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val contactId = if (idIdx >= 0) cursor.getString(idIdx) else null
            return name to contactId
        }
        return null
    }

    private fun readPhoneNumber(contactId: String): String {
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null,
        )?.use { pc ->
            if (pc.moveToFirst()) return pc.getString(0) ?: ""
        }
        return ""
    }
}
