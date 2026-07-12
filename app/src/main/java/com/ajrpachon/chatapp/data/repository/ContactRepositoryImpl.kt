package com.ajrpachon.chatapp.data.repository

import android.content.ContentResolver
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
}
