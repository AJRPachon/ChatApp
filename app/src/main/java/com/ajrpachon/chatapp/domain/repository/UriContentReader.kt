package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.UriMetadataBO

interface UriContentReader {
    fun getMetadata(uri: String): UriMetadataBO
    suspend fun readBytes(uri: String): ByteArray
}
