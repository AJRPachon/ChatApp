package com.ajrpachon.chatapp.ui.backup

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.repository.BackupRepository
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.ui.common.UiText
import com.ajrpachon.chatapp.ui.common.toUiText
import com.ajrpachon.chatapp.utils.AppLogger
import kotlinx.coroutines.launch

class BackupViewModel(
    private val backupRepository: BackupRepository,
) : BaseViewModel<BackupState, BackupEffect>(BackupState()) {

    init {
        loadLastBackupInfo()
    }

    fun onIntent(intent: BackupIntent) {
        when (intent) {
            BackupIntent.RequestBackup -> askPassphrase(PassphraseAction.BACKUP)
            BackupIntent.RequestRestore -> askPassphrase(PassphraseAction.RESTORE)
            BackupIntent.DismissPassphrase -> updateState { it.copy(passphraseAction = null) }
            is BackupIntent.StartBackup -> startBackup(intent.passphrase)
            is BackupIntent.StartRestore -> startRestore(intent.passphrase)
            BackupIntent.DismissError -> updateState { it.copy(error = null) }
            BackupIntent.DismissSuccess -> updateState { it.copy(successMessage = null) }
        }
    }

    private fun askPassphrase(action: PassphraseAction) {
        if (state.value.isBackingUp || state.value.isRestoring) return
        updateState { it.copy(passphraseAction = action) }
    }

    private fun loadLastBackupInfo() {
        viewModelScope.launch {
            runCatching {
                val info = backupRepository.getLatestBackupInfo()
                if (info != null) {
                    updateState {
                        it.copy(
                            lastBackupDate = info.lastBackupDate,
                            backupSizeMb = info.backupSizeMb,
                        )
                    }
                }
            }.onFailure { e ->
                AppLogger.w("BackupViewModel", "Could not load backup info: ${e.message}")
            }
        }
    }

    private fun startBackup(passphrase: String) {
        if (state.value.isBackingUp || state.value.isRestoring) return
        if (passphrase.length < MIN_BACKUP_PASSPHRASE_LENGTH) return
        viewModelScope.launch {
            updateState { it.copy(isBackingUp = true, passphraseAction = null, error = null) }
            runCatching {
                val info = backupRepository.backup(passphrase)
                updateState {
                    it.copy(
                        isBackingUp = false,
                        lastBackupDate = info.lastBackupDate,
                        backupSizeMb = info.backupSizeMb,
                        successMessage = UiText.StringResource(R.string.backup_success),
                    )
                }
            }.onFailure { e ->
                AppLogger.e("BackupViewModel", "Backup failed", e)
                updateState {
                    it.copy(
                        isBackingUp = false,
                        error = e.toUiText(R.string.backup_error_backup),
                    )
                }
            }
        }
    }

    private fun startRestore(passphrase: String) {
        if (state.value.isBackingUp || state.value.isRestoring) return
        if (passphrase.isEmpty()) return
        viewModelScope.launch {
            updateState { it.copy(isRestoring = true, passphraseAction = null, error = null) }
            runCatching {
                backupRepository.restore(passphrase)
                updateState {
                    it.copy(
                        isRestoring = false,
                        successMessage = UiText.StringResource(R.string.backup_restore_success),
                    )
                }
            }.onFailure { e ->
                AppLogger.e("BackupViewModel", "Restore failed", e)
                updateState {
                    it.copy(
                        isRestoring = false,
                        error = e.toUiText(R.string.backup_error_restore),
                    )
                }
            }
        }
    }
}
