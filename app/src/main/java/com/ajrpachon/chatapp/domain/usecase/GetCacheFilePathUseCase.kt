package com.ajrpachon.chatapp.domain.usecase

/**
 * Returns the absolute path for [filename] within the app's cache directory.
 *
 * Returns a plain [String] rather than `java.io.File` — `File` is a JVM-only type and this
 * project's `domain` layer is meant to stay free of platform-specific types (KMP-ready).
 * Construct a platform `File`/path object from the returned string where actual file I/O is
 * needed, not here.
 */
class GetCacheFilePathUseCase(private val cacheDirPath: String) {
    operator fun invoke(filename: String): String = "$cacheDirPath/$filename"
}
