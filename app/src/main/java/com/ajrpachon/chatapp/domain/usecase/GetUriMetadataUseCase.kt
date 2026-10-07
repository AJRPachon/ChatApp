package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.UriMetadataBO
import com.ajrpachon.chatapp.domain.repository.UriContentReader

class GetUriMetadataUseCase(private val uriContentReader: UriContentReader) {
    operator fun invoke(uri: String): UriMetadataBO = uriContentReader.getMetadata(uri)
}
