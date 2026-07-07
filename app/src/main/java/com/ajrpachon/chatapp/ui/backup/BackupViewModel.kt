package com.ajrpachon.chatapp.ui.backup

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.BackupManager
import kotlinx.coroutines.launch

class BackupViewModel(
    private val backupManager: BackupManager,
) : BaseViewModel<BackupState, BackupEffect>(BackupState()) {

    init {
        loadLastBackupInfo()
    }

    fun onIntent(intent: BackupIntent) {
        when (intent) {
            BackupIntent.StartBackup -> startBackup()
            BackupIntent.StartRestore -> startRestore()
            BackupIntent.DismissError -> updateState { it.copy(error = null) }
            BackupIntent.DismissSuccess -> updateState { it.copy(successMessage = null) }
        }
    }

    private fun loadLastBackupInfo() {
        viewModelScope.launch {
            runCatching {
                val info = backupManager.getLatestBackupInfo()
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

    private fun startBackup() {
        if (state.value.isBackingUp || state.value.isRestoring) return
        viewModelScope.launch {
            updateState { it.copy(isBackingUp = true, error = null) }
            runCatching {
                val info = backupManager.backup()
                updateState {
                    it.copy(
                        isBackingUp = false,
                        lastBackupDate = info.lastBackupDate,
                        backupSizeMb = info.backupSizeMb,
                        successMessage = "Copia realizada correctamente",
                    )
                }
            }.onFailure { e ->
                AppLogger.e("BackupViewModel", "Backup failed", e)
                updateState {
                    it.copy(
                        isBackingUp = false,
                        error = e.message ?: "Error al hacer la copia de seguridad",
                    )
                }
            }
        }
    }

    private fun startRestore() {
        if (state.value.isBackingUp || state.value.isRestoring) return
        viewModelScope.launch {
            updateState { it.copy(isRestoring = true, error = null) }
            runCatching {
                backupManager.restore()
                updateState {
                    it.copy(
                        isRestoring = false,
                        successMessage = "Mensajes restaurados correctamente",
                    )
                }
            }.onFailure { e ->
                AppLogger.e("BackupViewModel", "Restore failed", e)
                updateState {
                    it.copy(
                        isRestoring = false,
                        error = e.message ?: "Error al restaurar la copia de seguridad",
                    )
                }
            }
        }
    }
}
