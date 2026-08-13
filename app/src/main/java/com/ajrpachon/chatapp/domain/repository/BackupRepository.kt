package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.BackupInfo

interface BackupRepository {
    suspend fun backup(): BackupInfo
    suspend fun restore()
    suspend fun getLatestBackupInfo(): BackupInfo?
}
