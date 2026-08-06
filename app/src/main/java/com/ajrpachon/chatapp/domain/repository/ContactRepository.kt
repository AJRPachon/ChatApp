package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.ContactBO

interface ContactRepository {
    suspend fun getContacts(): List<ContactBO>
    suspend fun getContactByUri(uri: String): ContactBO?
}
