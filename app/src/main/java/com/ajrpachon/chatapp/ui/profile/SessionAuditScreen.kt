package com.ajrpachon.chatapp.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionAuditScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val vm: SessionAuditViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                SessionAuditEffect.SessionRevoked -> {
                    snackbarHostState.showSnackbar("Sesión cerrada correctamente")
                }
                is SessionAuditEffect.Error -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    val otherSessions = state.sessions.filter { !it.isCurrent }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Sesiones activas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (otherSessions.isNotEmpty()) {
                        TextButton(
                            onClick = { vm.onIntent(SessionAuditIntent.RevokeAllOtherSessions) },
                        ) {
                            Text(
                                "Cerrar todas las demás",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.sessions.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No hay sesiones activas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            "Tus dispositivos conectados",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    items(state.sessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            onRevoke = { vm.onIntent(SessionAuditIntent.RevokeSession(session.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: SessionInfo,
    onRevoke: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (session.isCurrent)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.Smartphone,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (session.isCurrent)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        session.deviceInfo,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    if (session.isCurrent) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                "Este dispositivo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
                Text(
                    "Último acceso: ${formatDate(session.lastActiveAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            IconButton(onClick = onRevoke) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cerrar sesión",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun formatDate(epochMs: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(epochMs))
}
