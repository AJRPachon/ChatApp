package com.ajrpachon.chatapp.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.UserRelationship

// ── Contact-card bubble ──────────────────────────────────────────────────────
// Extracted per docs/chat-viewmodel-decomposition.md Phase 2 — part of the message-bubble
// rendering tree, rendered from MessageBubble.kt via ChatBubbleSlot (defined there).

@Composable
private fun ContactHeader(name: String, phone: String) {
    Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.padding(8.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (phone.isNotBlank()) {
                Text(
                    phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun ContactBubble(
    name: String,
    phone: String,
    isFromMe: Boolean,
    lookup: ContactPhoneLookup? = null,
    onCheckRelationship: (String) -> Unit = {},
    onPrimaryAction: (String) -> Unit = {},
) {
    val context = LocalContext.current

    // Relationship is resolved from the shared contact's own phone-derived identity, not
    // ChatState.otherUserId, since this bubble renders identically in group chats where
    // otherUserId is null/irrelevant to who this specific contact card belongs to.
    LaunchedEffect(phone) {
        if (phone.isNotBlank()) onCheckRelationship(phone)
    }

    ChatBubbleSlot(isFromMe = isFromMe, modifier = Modifier.padding(vertical = 2.dp)) { maxBubbleWidth ->
    Card(
        modifier = Modifier.widthIn(max = minOf(280.dp, maxBubbleWidth)),
        colors = CardDefaults.cardColors(
            containerColor = if (isFromMe) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        ContactHeader(name, phone)
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth()) {
            val relationship = lookup?.relationship
            val primaryLabel = when (relationship) {
                UserRelationship.CONNECTED -> stringResource(R.string.chat_contact_send_message)
                UserRelationship.PENDING_SENT -> stringResource(R.string.chat_contact_invitation_sent)
                else -> stringResource(R.string.chat_contact_send_invitation)
            }
            TextButton(
                onClick = { onPrimaryAction(phone) },
                enabled = relationship != UserRelationship.PENDING_SENT,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    primaryLabel,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TextButton(
                onClick = {
                    val insertIntent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                        type = android.provider.ContactsContract.Contacts.CONTENT_TYPE
                        putExtra(android.provider.ContactsContract.Intents.Insert.NAME, name)
                        putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, phone)
                    }
                    context.startActivity(insertIntent)
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    stringResource(R.string.chat_contact_add_to_contacts),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    }
}
