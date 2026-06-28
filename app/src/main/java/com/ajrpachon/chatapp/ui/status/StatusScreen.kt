package com.ajrpachon.chatapp.ui.status

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ajrpachon.chatapp.domain.model.StatusBO
import com.ajrpachon.chatapp.ui.components.ChatAppAvatar
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

private const val STORY_DURATION_MS = 5_000L

// ── Status bar (embedded in ConversationListScreen) ────────────────────────

@Composable
fun StatusBar(
    onViewStatus: (StatusBO) -> Unit,
    modifier: Modifier = Modifier,
    vm: StatusViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { vm.onIntent(StatusIntent.PostImageStatus(context, it)) }
    }

    if (state.statuses.isEmpty() && !state.isLoading) return

    Column(modifier = modifier) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // "Add status" button
            item {
                AddStatusButton(
                    onAddText = { vm.onIntent(StatusIntent.OpenCompose) },
                    onAddImage = {
                        imageLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                )
            }
            items(state.statuses, key = { it.id }) { status ->
                StatusAvatar(status = status, onClick = { onViewStatus(status) })
            }
        }
    }

    if (state.showComposeDialog) {
        ComposeStatusDialog(
            text = state.composeText,
            onTextChange = { vm.onIntent(StatusIntent.TextChanged(it)) },
            onPost = { vm.onIntent(StatusIntent.PostTextStatus) },
            onDismiss = { vm.onIntent(StatusIntent.CloseCompose) },
        )
    }
}

@Composable
private fun AddStatusButton(
    onAddText: () -> Unit,
    onAddImage: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onAddText),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add text status",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(onClick = onAddImage),
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "Add image status",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Mi estado", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StatusAvatar(status: StatusBO, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        ) {
            ChatAppAvatar(
                name = status.userName,
                url = status.userAvatarUrl,
                size = 52.dp,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = status.userName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ComposeStatusDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onPost: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo estado") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("¿Qué está pasando?") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onPost, enabled = text.isNotBlank()) { Text("Publicar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

// ── Full-screen story viewer ───────────────────────────────────────────────

@Composable
fun StatusViewerScreen(
    statuses: List<StatusBO>,
    initialIndex: Int = 0,
    onClose: () -> Unit,
    onDelete: (String) -> Unit = {},
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    val current = statuses.getOrNull(currentIndex) ?: run { onClose(); return }
    val progress = remember(currentIndex) { Animatable(0f) }

    LaunchedEffect(currentIndex) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(STORY_DURATION_MS.toInt(), easing = LinearEasing),
        )
        if (currentIndex < statuses.lastIndex) currentIndex++ else onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(current.backgroundColor)),
    ) {
        // Background image if present
        if (current.imageUrl != null) {
            AsyncImage(
                model = current.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Progress bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            statuses.forEachIndexed { idx, _ ->
                val segmentProgress = when {
                    idx < currentIndex -> 1f
                    idx == currentIndex -> progress.value
                    else -> 0f
                }
                LinearProgressIndicator(
                    progress = { segmentProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.4f),
                )
            }
        }

        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 8.dp, end = 8.dp)
                .align(Alignment.TopCenter),
        ) {
            ChatAppAvatar(
                name = current.userName,
                url = current.userAvatarUrl,
                size = 36.dp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = current.userName,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (current.isFromMe) {
                IconButton(onClick = { onDelete(current.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar estado", tint = Color.White)
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }

        // Text overlay
        if (!current.text.isNullOrBlank()) {
            Text(
                text = current.text,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(16.dp),
            )
        }

        // Tap zones to navigate
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxSize().clickable {
                if (currentIndex > 0) currentIndex-- else onClose()
            })
            Box(modifier = Modifier.weight(1f).fillMaxSize().clickable {
                if (currentIndex < statuses.lastIndex) currentIndex++ else onClose()
            })
        }
    }
}

// Convenience: launch viewer for a single user's statuses starting at that user's first story
@Composable
fun StatusViewerScreen(
    initialStatus: StatusBO,
    allStatuses: List<StatusBO>,
    onClose: () -> Unit,
    vm: StatusViewModel = koinViewModel(),
) {
    val userStatuses = allStatuses.filter { it.userId == initialStatus.userId }
    StatusViewerScreen(
        statuses = userStatuses,
        initialIndex = 0,
        onClose = onClose,
        onDelete = { id -> vm.onIntent(StatusIntent.DeleteStatus(id)) },
    )
}
