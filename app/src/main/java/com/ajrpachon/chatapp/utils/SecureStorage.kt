package com.ajrpachon.chatapp.utils

// No Android imports — ready to move to commonMain in a KMP project.
interface SecureStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}
