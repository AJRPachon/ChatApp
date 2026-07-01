package com.ajrpachon.chatapp.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajrpachon.chatapp.ui.components.ChatAppPrimaryButton
import com.ajrpachon.chatapp.ui.components.ChatAppSecondaryButton
import com.ajrpachon.chatapp.ui.components.ChatAppTopBar
import org.koin.androidx.compose.koinViewModel

@Composable
fun BackupScreen(
    onBack: () -> Unit,
) {
    val vm: BackupViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.onIntent(BackupIntent.DismissSuccess)
        }
    }

    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { vm.onIntent(BackupIntent.DismissError) },
            title = { Text("Error") },
            text = { Text(state.error!!) },
            confirmButton = {
                TextButton(onClick = { vm.onIntent(BackupIntent.DismissError) }) {
                    Text("Aceptar")
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatAppTopBar(title = "Copia de seguridad", onBack = onBack)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Última copia de seguridad",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    HorizontalDivider()
                    if (state.lastBackupDate != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Fecha",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                state.lastBackupDate!!,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (state.backupSizeMb != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "Tamaño",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "${state.backupSizeMb} MB",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    } else {
                        Text(
                            "Sin copias de seguridad en Google Drive",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text(
                "Los mensajes se guardan como un archivo JSON en tu Google Drive personal. " +
                    "Solo tú puedes acceder a este archivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()

            if (state.isBackingUp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        "Creando copia de seguridad…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                ChatAppPrimaryButton(
                    text = "Hacer copia",
                    onClick = { vm.onIntent(BackupIntent.StartBackup) },
                    leadingIcon = Icons.Default.CloudUpload,
                    enabled = !state.isRestoring,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.isRestoring) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        "Restaurando mensajes…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                ChatAppSecondaryButton(
                    text = "Restaurar",
                    onClick = { vm.onIntent(BackupIntent.StartRestore) },
                    leadingIcon = Icons.Default.CloudDownload,
                    enabled = !state.isBackingUp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
