package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.BackupInfoBO

interface BackupRepository {
    suspend fun backup(): BackupInfoBO
    suspend fun restore()
    suspend fun getLatestBackupInfo(): BackupInfoBO?
}
