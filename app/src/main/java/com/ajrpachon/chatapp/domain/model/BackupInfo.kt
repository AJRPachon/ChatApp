package com.ajrpachon.chatapp.domain.model

data class BackupInfo(
    val lastBackupDate: String,
    val backupSizeMb: String,
    val fileId: String,
)
