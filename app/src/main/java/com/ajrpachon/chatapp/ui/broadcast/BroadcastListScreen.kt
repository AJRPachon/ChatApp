package com.ajrpachon.chatapp.ui.broadcast

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BroadcastListScreen(
    onBack: () -> Unit,
    vm: BroadcastListViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                BroadcastListEffect.GoBack -> onBack()
                is BroadcastListEffect.ShowToast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Create dialog ────────────────────────────────────────────────────────
    if (state.showCreateDialog) {
        AlertDialog(
            onDismissRequest = { vm.onIntent(BroadcastListIntent.DismissCreateDialog) },
            title = { Text("Nueva lista de difusión") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.newListName,
                        onValueChange = { vm.onIntent(BroadcastListIntent.NameChanged(it)) },
                        label = { Text("Nombre de la lista") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { vm.onIntent(BroadcastListIntent.SearchQueryChanged(it)) },
                        label = { Text("Buscar contactos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.selectedMembers.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            state.selectedMembers.forEach { user ->
                                AssistChip(
                                    onClick = { vm.onIntent(BroadcastListIntent.ToggleMember(user)) },
                                    label = { Text(user.displayName) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    },
                                )
                            }
                        }
                    }
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(state.searchResults, key = { it.id }) { user ->
                            val isSelected = state.selectedMembers.any { it.id == user.id }
                            ListItem(
                                headlineContent = { Text(user.displayName) },
                                supportingContent = { Text("@${user.username}") },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                trailingContent = {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Seleccionado",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                                modifier = Modifier.clickable { vm.onIntent(BroadcastListIntent.ToggleMember(user)) },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.onIntent(BroadcastListIntent.CreateList) },
                    enabled = !state.isCreating,
                ) {
                    if (state.isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    else Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.onIntent(BroadcastListIntent.DismissCreateDialog) }) {
                    Text("Cancelar")
                }
            },
        )
    }

    // ── Send dialog ──────────────────────────────────────────────────────────
    state.sendingListId?.let { listId ->
        val listItem = state.lists.find { it.id == listId }
        AlertDialog(
            onDismissRequest = { vm.onIntent(BroadcastListIntent.DismissSendDialog) },
            title = { Text("Difundir mensaje") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listItem?.let {
                        Text(
                            text = "Se enviará a ${it.members.size} contacto(s) de \"${it.name}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = state.broadcastMessage,
                        onValueChange = { vm.onIntent(BroadcastListIntent.BroadcastMessageChanged(it)) },
                        label = { Text("Mensaje") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.onIntent(BroadcastListIntent.SendBroadcast) },
                    enabled = !state.isSending && state.broadcastMessage.isNotBlank(),
                ) {
                    if (state.isSending) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Enviar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.onIntent(BroadcastListIntent.DismissSendDialog) }) {
                    Text("Cancelar")
                }
            },
        )
    }

    // ── Error dialog ─────────────────────────────────────────────────────────
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { vm.onIntent(BroadcastListIntent.DismissError) },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { vm.onIntent(BroadcastListIntent.DismissError) }) { Text("OK") }
            },
        )
    }

    // ── Main scaffold ────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listas de difusión", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.onIntent(BroadcastListIntent.OpenCreateDialog) }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva lista")
            }
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else if (state.lists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Campaign,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Sin listas de difusión",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Toca + para crear una nueva",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
            ) {
                items(state.lists, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = {
                            Text(item.name, fontWeight = FontWeight.SemiBold)
                        },
                        supportingContent = {
                            Text("${item.members.size} contacto(s)")
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.Campaign,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { vm.onIntent(BroadcastListIntent.OpenSendDialog(item.id)) }) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Difundir",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(onClick = { vm.onIntent(BroadcastListIntent.DeleteList(item.id)) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
