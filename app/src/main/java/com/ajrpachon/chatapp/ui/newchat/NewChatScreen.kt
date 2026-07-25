package com.ajrpachon.chatapp.ui.newchat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ajrpachon.chatapp.ui.components.ChatAppAvatar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.ui.common.ChatConstants
import com.ajrpachon.chatapp.domain.model.UserRelationship
import com.ajrpachon.chatapp.ui.components.ChatAppSecondaryButton
import com.ajrpachon.chatapp.ui.components.ChatAppSearchField
import com.ajrpachon.chatapp.ui.components.ChatAppTopBar
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import com.github.skydoves.navgraph.annotations.NavDestination
import com.github.skydoves.navgraph.annotations.NavEdge
import com.ajrpachon.chatapp.ChatRoute
import com.ajrpachon.chatapp.InvitationsRoute
import com.ajrpachon.chatapp.NewChatRoute
import org.koin.androidx.compose.koinViewModel

@NavEdge(to = ChatRoute::class, label = "Open Chat")
@NavEdge(to = InvitationsRoute::class, label = "Invite User")
@NavDestination(route = NewChatRoute::class)
@Composable
fun NewChatScreen(
    onBack: () -> Unit,
    onOpenConversation: (id: String, name: String) -> Unit,
    onOpenInvitations: () -> Unit = {},
) {
    val vm: NewChatViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareInvitationTitle = stringResource(R.string.newchat_share_invitation_title)
    val qrNotRecognizedText = stringResource(R.string.newchat_qr_not_recognized)
    val scanQrPromptText = stringResource(R.string.newchat_scan_qr_prompt)

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is NewChatEffect.NavigateToChat -> onOpenConversation(effect.conversationId, effect.otherUserName)
                is NewChatEffect.NavigateToInvitations -> onOpenInvitations()
                is NewChatEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
                is NewChatEffect.ShareText -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, effect.text)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, shareInvitationTitle))
                }
                is NewChatEffect.InviteContact -> {
                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${effect.phoneNumber}")).apply {
                        putExtra("sms_body", effect.text)
                    }
                    context.startActivity(smsIntent)
                }
            }
        }
    }

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.onIntent(NewChatIntent.LoadContacts)
        } else {
            vm.onIntent(NewChatIntent.ContactsPermissionDenied)
        }
    }

    val qrScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val raw = result.contents ?: return@rememberLauncherForActivityResult
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return@rememberLauncherForActivityResult
        if (uri.scheme == ChatConstants.DEEP_LINK_SCHEME && uri.host == ChatConstants.DEEP_LINK_USER_HOST) {
            val userId = uri.lastPathSegment?.takeIf { it.isNotBlank() }
                ?: return@rememberLauncherForActivityResult
            vm.onIntent(NewChatIntent.UserScannedByQr(userId))
        } else {
            scope.launch { snackbarHostState.showSnackbar(qrNotRecognizedText) }
        }
    }

    LaunchedEffect(Unit) {
        val permission = Manifest.permission.READ_CONTACTS
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            vm.onIntent(NewChatIntent.LoadContacts)
        } else {
            requestPermission.launch(permission)
        }
    }

    Scaffold(
        topBar = {
            ChatAppTopBar(
                title = stringResource(R.string.newchat_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        qrScanLauncher.launch(
                            ScanOptions().apply {
                                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                setPrompt(scanQrPromptText)
                                setBeepEnabled(false)
                                setOrientationLocked(false)
                            }
                        )
                    }) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.newchat_scan_qr_code),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ChatAppSearchField(
                value = state.query,
                onValueChange = { vm.onIntent(NewChatIntent.QueryChanged(it)) },
                placeholder = stringResource(R.string.newchat_search_placeholder),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {

                if (state.currentUsername.isNotBlank()) {
                    item {
                        InviteCodeCard(
                            username = state.currentUsername,
                            onCopy = { vm.onIntent(NewChatIntent.CopyInviteCode(state.currentUsername)) },
                            onShare = { vm.onIntent(NewChatIntent.ShareInviteText(state.currentUsername)) },
                        )
                    }
                }

                // ── Sugeridos section ─────────────────────────────────────
                if (state.isLoadingSuggested) {
                    item {
                        SectionHeader(stringResource(R.string.newchat_suggested_section))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                    }
                } else if (state.suggestedContacts.isNotEmpty()) {
                    item {
                        SectionHeader(stringResource(R.string.newchat_suggested_section))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                        ) {
                            items(state.suggestedContacts, key = { it.id }) { user ->
                                SuggestedContactChip(
                                    user = user,
                                    onClick = { vm.onIntent(NewChatIntent.UserAction(user)) },
                                )
                            }
                        }
                    }
                }

                if (state.isLoadingUsers) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                    }
                }

                if (!state.isLoadingUsers && state.appUsers.isEmpty() && state.query.isNotBlank()) {
                    item {
                        Text(
                            stringResource(R.string.newchat_no_users_found, state.query),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                if (state.appUsers.isNotEmpty()) {
                    item {
                        SectionHeader(
                            if (state.query.isBlank()) {
                                stringResource(R.string.newchat_users_section)
                            } else {
                                stringResource(R.string.newchat_results_section)
                            }
                        )
                    }
                    items(state.appUsers, key = { it.id }) { user ->
                        AppUserItem(
                            user = user,
                            relationship = state.userRelationships[user.id],
                            isPending = user.id in state.pendingUserIds,
                            onAction = { vm.onIntent(NewChatIntent.UserAction(user)) },
                            onBlock = { vm.onIntent(NewChatIntent.BlockUser(user)) },
                            onUnblock = { vm.onIntent(NewChatIntent.UnblockUser(user)) },
                        )
                        HorizontalDivider()
                    }
                }

                if (state.contacts.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.newchat_contacts_section)) }
                    items(state.contacts, key = { it.phoneNumber }) { contact ->
                        ContactItem(
                            contact = contact,
                            onInvite = {
                                vm.onIntent(NewChatIntent.InviteContact(contact.phoneNumber, state.currentUsername))
                            },
                        )
                        HorizontalDivider()
                    }
                }

                if (state.contactsPermissionDenied) {
                    item {
                        Text(
                            stringResource(R.string.newchat_contacts_permission_denied),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                state.error?.let { error ->
                    item {
                        Text(
                            error,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────

@Composable
private fun InviteCodeCard(
    username: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.newchat_invite_friends_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                stringResource(R.string.newchat_invite_friends_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
            )
            Text(
                "@$username",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChatAppSecondaryButton(
                    text = stringResource(R.string.newchat_copy),
                    onClick = onCopy,
                    leadingIcon = Icons.Default.ContentCopy,
                )
                ChatAppSecondaryButton(
                    text = stringResource(R.string.newchat_share),
                    onClick = onShare,
                    leadingIcon = Icons.Default.Share,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun AppUserItem(
    user: UserBO,
    relationship: UserRelationship?,
    isPending: Boolean,
    onAction: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
) {
    val isConnected = relationship == UserRelationship.CONNECTED
    val isBlocked = relationship == UserRelationship.BLOCKED
    ListItem(
        headlineContent = { Text(user.displayName) },
        supportingContent = {
            if (isBlocked) {
                Text(
                    stringResource(R.string.newchat_blocked),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                )
            } else {
                Text("@${user.username}")
            }
        },
        trailingContent = {
            when {
                isPending -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                isBlocked ->
                    IconButton(onClick = onUnblock) {
                        Icon(
                            Icons.Default.LockOpen,
                            contentDescription = stringResource(R.string.newchat_unblock_cd),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                relationship == null || relationship == UserRelationship.NONE ->
                    Row {
                        IconButton(onClick = onAction) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = stringResource(R.string.newchat_invite_cd),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = onBlock) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = stringResource(R.string.newchat_block_cd),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                relationship == UserRelationship.PENDING_SENT ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                stringResource(R.string.newchat_invitation_sent),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                stringResource(R.string.newchat_pending_response),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        IconButton(onClick = onBlock) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = stringResource(R.string.newchat_block_cd),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                relationship == UserRelationship.PENDING_RECEIVED ->
                    IconButton(onClick = onAction) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = stringResource(R.string.newchat_accept_cd),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                isConnected -> null
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isConnected, onClick = onAction),
    )
}

@Composable
private fun ContactItem(contact: PhoneContact, onInvite: () -> Unit) {
    ListItem(
        headlineContent = { Text(contact.name) },
        supportingContent = { Text(contact.phoneNumber) },
        trailingContent = {
            IconButton(onClick = onInvite) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = stringResource(R.string.newchat_invite_contact_cd, contact.name),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SuggestedContactChip(
    user: UserBO,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        ChatAppAvatar(
            name = user.displayName,
            url = user.avatarUrl,
            size = 52.dp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = user.displayName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "@${user.username}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
