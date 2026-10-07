package com.ajrpachon.chatapp.ui.backup

import com.ajrpachon.chatapp.ui.common.UiText

sealed interface BackupEffect

/** What the passphrase being asked for is going to be used for. */
enum class PassphraseAction { BACKUP, RESTORE }

/** Minimum length of the passphrase that protects a new backup. */
const val MIN_BACKUP_PASSPHRASE_LENGTH = 8

sealed interface BackupIntent {
    data object RequestBackup : BackupIntent
    data object RequestRestore : BackupIntent
    data object DismissPassphrase : BackupIntent
    data class StartBackup(val passphrase: String) : BackupIntent
    data class StartRestore(val passphrase: String) : BackupIntent
    data object DismissError : BackupIntent
    data object DismissSuccess : BackupIntent
}

data class BackupState(
    val lastBackupDate: String? = null,
    val backupSizeMb: String? = null,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val passphraseAction: PassphraseAction? = null,
    val error: UiText? = null,
    val successMessage: UiText? = null,
)
