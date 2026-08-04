package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.UriMetadata
import com.ajrpachon.chatapp.domain.repository.UriContentReader

class GetUriMetadataUseCase(private val uriContentReader: UriContentReader) {
    operator fun invoke(uri: String): UriMetadata = uriContentReader.getMetadata(uri)
}
