package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.UriMetadata

interface UriContentReader {
    fun getMetadata(uri: String): UriMetadata
    suspend fun readBytes(uri: String): ByteArray
}
