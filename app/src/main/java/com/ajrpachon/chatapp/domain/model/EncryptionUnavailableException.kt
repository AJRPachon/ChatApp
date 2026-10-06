package com.ajrpachon.chatapp.domain.model

/**
 * A 1:1 text message could not be end-to-end encrypted (the recipient has no public key yet, or the
 * key lookup or encryption failed). The message is not sent: it never falls back to plaintext.
 */
class EncryptionUnavailableException(cause: Throwable? = null) :
    Exception("Message could not be encrypted", cause)
