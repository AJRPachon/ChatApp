package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.BackupInfoBO

interface BackupRepository {
    /** Uploads an encrypted copy of the messages; [passphrase] is needed to restore it. */
    suspend fun backup(passphrase: String): BackupInfoBO
    /** Throws [com.ajrpachon.chatapp.domain.model.WrongBackupPassphraseException] if [passphrase] does not open the backup. */
    suspend fun restore(passphrase: String)
    suspend fun getLatestBackupInfo(): BackupInfoBO?
}
