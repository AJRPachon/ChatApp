package com.ajrpachon.chatapp.domain.model

/** The backup could not be decrypted: the passphrase is wrong, or the file is not a ChatApp backup. */
class WrongBackupPassphraseException(cause: Throwable? = null) :
    Exception("Wrong backup passphrase", cause)
