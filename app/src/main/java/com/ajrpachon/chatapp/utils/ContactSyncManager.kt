package com.ajrpachon.chatapp.utils

import android.content.ContentResolver
import android.provider.ContactsContract

/**
 * Reads the device address book and returns a deduplicated list of email addresses
 * found across all contacts. Requires READ_CONTACTS permission to have been granted
 * before calling [readContactEmails].
 */
class ContactSyncManager(private val contentResolver: ContentResolver) {

    /**
     * Queries [ContactsContract.CommonDataKinds.Email.CONTENT_URI] and returns all
     * unique, lowercase email addresses stored in the device's address book.
     *
     * Must be called from a background thread (e.g. [kotlinx.coroutines.Dispatchers.IO]).
     */
    fun readContactEmails(): List<String> {
        val emails = mutableSetOf<String>()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
            null,
            null,
            null,
        ) ?: return emptyList()

        cursor.use {
            val emailIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (it.moveToNext()) {
                val email = it.getString(emailIdx)?.trim()?.lowercase() ?: continue
                if (email.contains('@')) emails.add(email)
            }
        }
        return emails.toList()
    }
}
