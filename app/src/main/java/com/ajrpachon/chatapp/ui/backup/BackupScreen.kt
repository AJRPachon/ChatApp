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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.ui.components.ChatAppPrimaryButton
import com.ajrpachon.chatapp.ui.components.ChatAppSecondaryButton
import com.ajrpachon.chatapp.ui.components.ChatAppTextButton
import com.ajrpachon.chatapp.ui.components.ChatAppTopBar
import com.ajrpachon.chatapp.ui.theme.ChatAppTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun BackupScreen(
    onBack: () -> Unit,
) {
    val vm: BackupViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            vm.onIntent(BackupIntent.DismissSuccess)
        }
    }

    BackupContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = vm::onIntent,
        onBack = onBack,
    )
}

@Composable
internal fun BackupContent(
    state: BackupState,
    snackbarHostState: SnackbarHostState,
    onIntent: (BackupIntent) -> Unit,
    onBack: () -> Unit,
) {
    state.passphraseAction?.let { action ->
        PassphraseDialog(
            action = action,
            onConfirm = { passphrase ->
                onIntent(
                    if (action == PassphraseAction.BACKUP) BackupIntent.StartBackup(passphrase) else BackupIntent.StartRestore(passphrase),
                )
            },
            onDismiss = { onIntent(BackupIntent.DismissPassphrase) },
        )
    }

    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { onIntent(BackupIntent.DismissError) },
            title = { Text(stringResource(R.string.backup_error_title)) },
            text = { Text(state.error?.asString().orEmpty()) },
            confirmButton = {
                ChatAppTextButton(text = stringResource(R.string.backup_accept), onClick = { onIntent(BackupIntent.DismissError) })
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
                    onClick = { onIntent(BackupIntent.RequestBackup) },
                    leadingIcon = Icons.Default.CloudUpload,
                    enabled = !state.isRestoring,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_make_backup_button"),
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
                    onClick = { onIntent(BackupIntent.RequestRestore) },
                    leadingIcon = Icons.Default.CloudDownload,
                    enabled = !state.isBackingUp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PassphraseDialog(
    action: PassphraseAction,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var repeated by remember { mutableStateOf("") }
    val isBackup = action == PassphraseAction.BACKUP
    val valid = if (isBackup) {
        passphrase.length >= MIN_BACKUP_PASSPHRASE_LENGTH && passphrase == repeated
    } else {
        passphrase.isNotEmpty()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isBackup) R.string.backup_passphrase_title_backup else R.string.backup_passphrase_title_restore)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(if (isBackup) R.string.backup_passphrase_hint_backup else R.string.backup_passphrase_hint_restore, MIN_BACKUP_PASSPHRASE_LENGTH),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(stringResource(R.string.backup_passphrase_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().testTag("backup_passphrase_field"),
                )
                if (isBackup) {
                    OutlinedTextField(
                        value = repeated,
                        onValueChange = { repeated = it },
                        label = { Text(stringResource(R.string.backup_passphrase_repeat_label)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            ChatAppTextButton(
                text = stringResource(R.string.backup_accept),
                onClick = { onConfirm(passphrase) },
                enabled = valid,
            )
        },
        dismissButton = {
            ChatAppTextButton(text = stringResource(R.string.backup_cancel), onClick = onDismiss)
        },
    )
}

@Preview(name = "Never backed up", showBackground = true)
@Composable
internal fun BackupEmptyPreview() {
    ChatAppTheme {
        BackupContent(state = BackupState(), snackbarHostState = remember { SnackbarHostState() }, onIntent = {}, onBack = {})
    }
}

@Preview(name = "With a backup", showBackground = true)
@Composable
internal fun BackupWithDataPreview() {
    ChatAppTheme {
        BackupContent(
            state = BackupState(lastBackupDate = "03/10/2026 21:14", backupSizeMb = "12,4"),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Backing up", showBackground = true)
@Composable
internal fun BackupInProgressPreview() {
    ChatAppTheme {
        BackupContent(
            state = BackupState(lastBackupDate = "03/10/2026 21:14", backupSizeMb = "12,4", isBackingUp = true),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onBack = {},
        )
    }
}

