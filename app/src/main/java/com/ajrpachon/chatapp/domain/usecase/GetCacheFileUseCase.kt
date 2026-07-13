package com.ajrpachon.chatapp.domain.usecase

import java.io.File

class GetCacheFileUseCase(private val cacheDir: File) {
    operator fun invoke(filename: String): File = File(cacheDir, filename)
}
