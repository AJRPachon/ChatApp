package com.ajrpachon.chatapp.domain.model

data class BackupInfoBO(
    val lastBackupDate: String,
    val backupSizeMb: String,
    val fileId: String,
)
