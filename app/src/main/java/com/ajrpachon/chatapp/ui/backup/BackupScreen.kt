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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajrpachon.chatapp.R
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
            title = { Text(stringResource(R.string.backup_error_title)) },
            text = { Text(state.error.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { vm.onIntent(BackupIntent.DismissError) }) {
                    Text(stringResource(R.string.backup_accept))
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatAppTopBar(title = stringResource(R.string.backup_top_bar_title), onBack = onBack)
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
                        stringResource(R.string.backup_last_backup_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    HorizontalDivider()
                    if (state.lastBackupDate != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                stringResource(R.string.backup_date_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                state.lastBackupDate.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (state.backupSizeMb != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    stringResource(R.string.backup_size_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    stringResource(R.string.backup_size_mb, state.backupSizeMb.toString()),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    } else {
                        Text(
                            stringResource(R.string.backup_no_backups),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.backup_disclaimer),
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
                        stringResource(R.string.backup_creating_backup),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                ChatAppPrimaryButton(
                    text = stringResource(R.string.backup_make_backup_button),
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
                        stringResource(R.string.backup_restoring_messages),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                ChatAppSecondaryButton(
                    text = stringResource(R.string.backup_restore_button),
                    onClick = { vm.onIntent(BackupIntent.StartRestore) },
                    leadingIcon = Icons.Default.CloudDownload,
                    enabled = !state.isBackingUp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
