package com.ajrpachon.chatapp.domain.repository

import android.net.Uri
import com.ajrpachon.chatapp.domain.model.ContactBO

interface ContactRepository {
    suspend fun getContacts(): List<ContactBO>
    suspend fun getContactByUri(uri: Uri): ContactBO?
}
