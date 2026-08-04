package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.UriContentReader

class ReadUriAsBytesUseCase(private val uriContentReader: UriContentReader) {
    suspend operator fun invoke(uri: String): ByteArray = uriContentReader.readBytes(uri)
}
