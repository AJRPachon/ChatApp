package com.ajrpachon.chatapp.ui.backup

sealed interface BackupIntent {
    data object StartBackup : BackupIntent
    data object StartRestore : BackupIntent
    data object DismissError : BackupIntent
    data object DismissSuccess : BackupIntent
}

data class BackupState(
    val lastBackupDate: String? = null,
    val backupSizeMb: String? = null,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)
