package com.ajrpachon.chatapp.ui.profile

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.ThemePreference
import com.ajrpachon.chatapp.ui.components.ChatAppDestructiveButton
import com.ajrpachon.chatapp.ui.components.ChatAppPrimaryButton
import com.ajrpachon.chatapp.ui.components.ChatAppSecondaryButton
import com.ajrpachon.chatapp.ui.components.ChatAppTextField
import com.ajrpachon.chatapp.ui.components.ChatAppTopBar
import com.github.skydoves.navgraph.annotations.NavDestination
import com.github.skydoves.navgraph.annotations.NavEdge
import com.ajrpachon.chatapp.AuthRoute
import com.ajrpachon.chatapp.ProfileRoute
import org.koin.androidx.compose.koinViewModel

@NavEdge(to = AuthRoute::class, label = "Sign Out")
@NavDestination(route = ProfileRoute::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onBackup: () -> Unit = {},
    onSessionAudit: () -> Unit = {},
) {
    val vm: ProfileViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showSignOutAllDialog by remember { mutableStateOf(false) }
    var showQrSheet by remember { mutableStateOf(false) }
    val qrSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val enrollSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── QR Bottom Sheet ────────────────────────────────────────────────────
    if (showQrSheet && state.userId.isNotBlank()) {
        val qrBitmap: Bitmap? = state.qrBitmap

        ModalBottomSheet(
            onDismissRequest = { showQrSheet = false },
            sheetState = qrSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(R.string.profile_my_qr_code_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.profile_my_qr_code_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.profile_qr_code_content_description),
                        modifier = Modifier.size(220.dp),
                    )
                }
                Text(
                    text = state.userId,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    // ── 2FA Enroll Bottom Sheet ────────────────────────────────────────────
    if (state.twoFactor.showEnrollSheet) {
        val keyboard = LocalSoftwareKeyboardController.current
        var verifyCode by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { vm.onIntent(ProfileIntent.Dismiss2FASheet) },
            sheetState = enrollSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.profile_2fa_setup_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                val secret = state.twoFactor.secret
                if (secret != null) {
                    Text(
                        stringResource(R.string.profile_2fa_setup_instructions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = secret,
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                Text(
                    stringResource(R.string.profile_2fa_confirm_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                ChatAppTextField(
                    value = verifyCode,
                    onValueChange = { if (it.length <= 6) verifyCode = it },
                    label = stringResource(R.string.profile_totp_code_label),
                    isError = state.twoFactor.verifyError != null,
                    supportingText = state.twoFactor.verifyError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboard?.hide()
                        vm.onIntent(ProfileIntent.Verify2FACode(verifyCode))
                    }),
                )
                val enrollError = state.twoFactor.enrollError
                if (enrollError != null) {
                    Text(
                        enrollError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
                if (state.twoFactor.isLoading) {
                    CircularProgressIndicator()
                } else {
                    ChatAppPrimaryButton(
                        text = stringResource(R.string.profile_verify_and_activate),
                        onClick = {
                            keyboard?.hide()
                            vm.onIntent(ProfileIntent.Verify2FACode(verifyCode))
                        },
                        enabled = verifyCode.length == 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateToAuth -> onSignOut()
                ProfileEffect.ShowSignOutAllConfirm -> showSignOutAllDialog = true
            }
        }
    }

    if (showSignOutAllDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSignOutAllDialog = false },
            title = { androidx.compose.material3.Text(stringResource(R.string.profile_sign_out_all_dialog_title)) },
            text = { androidx.compose.material3.Text(stringResource(R.string.profile_sign_out_all_dialog_text)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showSignOutAllDialog = false
                    vm.signOutAll()
                }) { androidx.compose.material3.Text(stringResource(R.string.profile_sign_out_all_confirm)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showSignOutAllDialog = false }) {
                    androidx.compose.material3.Text(stringResource(R.string.profile_cancel))
                }
            },
        )
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    val avatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            if (bytes != null) vm.onAvatarSelected(bytes, mimeType)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatAppTopBar(title = stringResource(R.string.profile_top_bar_title), onBack = onBack)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Spacer(Modifier.size(24.dp))

            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable(enabled = !state.isUploadingAvatar) {
                            avatarLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.avatarUrl != null) {
                        AsyncImage(
                            model = state.avatarUrl,
                            contentDescription = stringResource(R.string.profile_avatar_content_description),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.isUploadingAvatar) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = stringResource(R.string.profile_change_photo_content_description),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.size(16.dp))

            val keyboard = LocalSoftwareKeyboardController.current
            ChatAppTextField(
                value = state.editingDisplayName,
                onValueChange = { vm.onIntent(ProfileIntent.EditDisplayName(it)) },
                label = stringResource(R.string.profile_display_name_label),
                enabled = !state.isSavingDisplayName,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboard?.hide()
                    vm.onIntent(ProfileIntent.SaveDisplayName)
                }),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.isSavingDisplayName) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            }
            Text(
                stringResource(R.string.profile_username_display, state.username),
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.primary,
            )
            if (state.email.isNotBlank()) {
                Text(
                    state.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Spacer(Modifier.size(24.dp))
            HorizontalDivider()
            Spacer(Modifier.size(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        stringResource(R.string.profile_show_online_status_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(R.string.profile_show_online_status_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Switch(
                    checked = state.showOnlineStatus,
                    onCheckedChange = { vm.onIntent(ProfileIntent.ToggleOnlineStatus(it)) },
                )
            }

            HorizontalDivider()
            Spacer(Modifier.size(8.dp))

            // ── Apariencia ──────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.profile_appearance_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                ThemeSelector(
                    selected = state.themePreference,
                    onSelect = { vm.onIntent(ProfileIntent.SetTheme(it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HorizontalDivider()

            ChatAppSecondaryButton(
                text = stringResource(R.string.profile_my_qr_code_button),
                onClick = { showQrSheet = true },
                leadingIcon = Icons.Default.QrCode,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            // ── App Lock ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(
                            stringResource(R.string.profile_app_lock_title),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            stringResource(R.string.profile_app_lock_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                Switch(
                    checked = state.isAppLockEnabled,
                    onCheckedChange = { vm.onIntent(ProfileIntent.ToggleAppLock) },
                )
            }

            HorizontalDivider()

            // ── 2FA ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.profile_2fa_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        if (state.twoFactor.isEnrolled) {
                            stringResource(R.string.profile_2fa_active)
                        } else {
                            stringResource(R.string.profile_2fa_inactive)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.twoFactor.isEnrolled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline,
                    )
                }
                if (state.twoFactor.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else if (state.twoFactor.isEnrolled) {
                    ChatAppDestructiveButton(
                        text = stringResource(R.string.profile_2fa_deactivate),
                        onClick = { vm.onIntent(ProfileIntent.Disable2FA) },
                    )
                } else {
                    ChatAppSecondaryButton(
                        text = stringResource(R.string.profile_2fa_activate),
                        onClick = { vm.onIntent(ProfileIntent.Enroll2FA) },
                        leadingIcon = Icons.Default.Shield,
                    )
                }
            }

            HorizontalDivider()

            // ── Copia de seguridad ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBackup() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.profile_backup_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(R.string.profile_backup_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            HorizontalDivider()

            // ── Sesiones activas ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSessionAudit() }
                    .padding(vertical = 12.dp)
                    .testTag("profile_session_audit_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.DevicesOther,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.profile_sessions_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(R.string.profile_sessions_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            HorizontalDivider()
            Spacer(Modifier.weight(1f))

            ChatAppDestructiveButton(
                text = stringResource(R.string.profile_sign_out),
                onClick = { vm.signOut() },
                leadingIcon = Icons.AutoMirrored.Filled.Logout,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_sign_out_button"),
            )
            Spacer(Modifier.size(8.dp))
            ChatAppDestructiveButton(
                text = stringResource(R.string.profile_sign_out_all_devices),
                onClick = { vm.requestSignOutAll() },
                leadingIcon = Icons.AutoMirrored.Filled.Logout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun ThemeSelector(
    selected: ThemePreference,
    onSelect: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        ThemePreference.SYSTEM to stringResource(R.string.profile_theme_system),
        ThemePreference.LIGHT to stringResource(R.string.profile_theme_light),
        ThemePreference.DARK to stringResource(R.string.profile_theme_dark),
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (pref, label) ->
            val isSelected = pref == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(pref) }
                    .testTag("profile_theme_${pref.name.lowercase()}_option"),
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                tonalElevation = if (isSelected) 0.dp else 0.dp,
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

