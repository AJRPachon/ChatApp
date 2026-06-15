package com.ajrpachon.chatapp.ui.newchat

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.model.UserRelationship
import com.ajrpachon.chatapp.ui.components.ChatAppSecondaryButton
import com.ajrpachon.chatapp.ui.components.ChatAppSearchField
import com.ajrpachon.chatapp.ui.components.ChatAppTextField
import com.ajrpachon.chatapp.ui.components.ChatAppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is NewChatEffect.NavigateToChat -> onOpenConversation(effect.conversationId, effect.otherUserName)
                is NewChatEffect.NavigateToInvitations -> onOpenInvitations()
                is NewChatEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
            }
        }
    }

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                val contacts = withContext(Dispatchers.IO) { loadContacts(context.contentResolver) }
                vm.onIntent(NewChatIntent.ContactsLoaded(contacts))
            }
        } else {
            vm.onIntent(NewChatIntent.ContactsPermissionDenied)
        }
    }

    LaunchedEffect(Unit) {
        val permission = Manifest.permission.READ_CONTACTS
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            val contacts = withContext(Dispatchers.IO) { loadContacts(context.contentResolver) }
            vm.onIntent(NewChatIntent.ContactsLoaded(contacts))
        } else {
            requestPermission.launch(permission)
        }
    }

    Scaffold(
        topBar = {
            ChatAppTopBar(title = "Nuevo chat", onBack = onBack)
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
                placeholder = "Buscar por username",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {

                if (state.currentUsername.isNotBlank()) {
                    item {
                        InviteCodeCard(
                            username = state.currentUsername,
                            onCopy = {
                                copyToClipboard(context, state.currentUsername)
                                scope.launch { snackbarHostState.showSnackbar("Código copiado") }
                            },
                            onShare = { shareInviteText(context, state.currentUsername) },
                        )
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
                            "No se encontró ningún usuario con \"${state.query}\"",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                if (state.appUsers.isNotEmpty()) {
                    item {
                        SectionHeader(if (state.query.isBlank()) "Usuarios" else "Resultados")
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
                    item { SectionHeader("Contactos") }
                    items(state.contacts, key = { it.phoneNumber }) { contact ->
                        ContactItem(
                            contact = contact,
                            onInvite = {
                                inviteContact(context, contact.phoneNumber, state.currentUsername)
                            },
                        )
                        HorizontalDivider()
                    }
                }

                if (state.contactsPermissionDenied) {
                    item {
                        Text(
                            "Permiso de contactos denegado",
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
                "Invita a tus amigos",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                "Comparte tu código para que puedan encontrarte",
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
                    text = "Copiar",
                    onClick = onCopy,
                    leadingIcon = Icons.Default.ContentCopy,
                )
                ChatAppSecondaryButton(
                    text = "Compartir",
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
            if (isBlocked) Text("Bloqueado", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            else Text("@${user.username}")
        },
        trailingContent = {
            when {
                isPending -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                isBlocked ->
                    IconButton(onClick = onUnblock) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Desbloquear", tint = MaterialTheme.colorScheme.outline)
                    }
                relationship == null || relationship == UserRelationship.NONE ->
                    Row {
                        IconButton(onClick = onAction) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Invitar", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onBlock) {
                            Icon(Icons.Default.Block, contentDescription = "Bloquear", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                relationship == UserRelationship.PENDING_SENT ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Invitación enviada", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Pendiente de respuesta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(onClick = onBlock) {
                            Icon(Icons.Default.Block, contentDescription = "Bloquear", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                relationship == UserRelationship.PENDING_RECEIVED ->
                    IconButton(onClick = onAction) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Aceptar", tint = MaterialTheme.colorScheme.tertiary)
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
                    contentDescription = "Invitar a ${contact.name}",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

// ── Helpers ────────────────────────────────────────────────────────────────

private fun copyToClipboard(context: Context, username: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("invite_code", "@$username"))
}

private fun shareInviteText(context: Context, username: String) {
    val text = "¡Únete a ChatApp! Búscame como @$username y hablamos 💬"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir invitación"))
}

private fun inviteContact(context: Context, phoneNumber: String, username: String) {
    val message = "¡Únete a ChatApp! Búscame como @$username y hablamos 💬"
    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber")).apply {
        putExtra("sms_body", message)
    }
    if (smsIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(smsIntent)
    } else {
        shareInviteText(context, username)
    }
}

private fun loadContacts(resolver: ContentResolver): List<PhoneContact> {
    val contacts = mutableListOf<PhoneContact>()
    val cursor = resolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        ),
        null, null,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
    ) ?: return contacts
    cursor.use {
        val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (it.moveToNext()) {
            val name = it.getString(nameIdx) ?: continue
            val number = it.getString(numIdx) ?: continue
            contacts.add(PhoneContact(name, number.trim()))
        }
    }
    return contacts.distinctBy { it.phoneNumber }
}
