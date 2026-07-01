package com.ajrpachon.chatapp.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BackupViewModel(
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupState())
    val state = _state.asStateFlow()

    init {
        loadLastBackupInfo()
    }

    fun onIntent(intent: BackupIntent) {
        when (intent) {
            BackupIntent.StartBackup -> startBackup()
            BackupIntent.StartRestore -> startRestore()
            BackupIntent.DismissError -> _state.update { it.copy(error = null) }
            BackupIntent.DismissSuccess -> _state.update { it.copy(successMessage = null) }
        }
    }

    private fun loadLastBackupInfo() {
        viewModelScope.launch {
            runCatching {
                val info = backupManager.getLatestBackupInfo()
                if (info != null) {
                    _state.update {
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
        if (_state.value.isBackingUp || _state.value.isRestoring) return
        viewModelScope.launch {
            _state.update { it.copy(isBackingUp = true, error = null) }
            runCatching {
                val info = backupManager.backup()
                _state.update {
                    it.copy(
                        isBackingUp = false,
                        lastBackupDate = info.lastBackupDate,
                        backupSizeMb = info.backupSizeMb,
                        successMessage = "Copia realizada correctamente",
                    )
                }
            }.onFailure { e ->
                AppLogger.e("BackupViewModel", "Backup failed", e)
                _state.update {
                    it.copy(
                        isBackingUp = false,
                        error = e.message ?: "Error al hacer la copia de seguridad",
                    )
                }
            }
        }
    }

    private fun startRestore() {
        if (_state.value.isBackingUp || _state.value.isRestoring) return
        viewModelScope.launch {
            _state.update { it.copy(isRestoring = true, error = null) }
            runCatching {
                backupManager.restore()
                _state.update {
                    it.copy(
                        isRestoring = false,
                        successMessage = "Mensajes restaurados correctamente",
                    )
                }
            }.onFailure { e ->
                AppLogger.e("BackupViewModel", "Restore failed", e)
                _state.update {
                    it.copy(
                        isRestoring = false,
                        error = e.message ?: "Error al restaurar la copia de seguridad",
                    )
                }
            }
        }
    }
}
