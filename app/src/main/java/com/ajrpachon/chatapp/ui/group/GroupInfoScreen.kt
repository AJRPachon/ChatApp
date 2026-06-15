package com.ajrpachon.chatapp.ui.group

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import coil3.compose.AsyncImage
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.GroupRole
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.ui.components.ChatAppAvatar
import com.ajrpachon.chatapp.ui.components.ChatAppTextField
import com.github.skydoves.navgraph.annotations.NavDestination
import com.github.skydoves.navgraph.annotations.NavEdge
import com.ajrpachon.chatapp.GroupInfoRoute
import com.ajrpachon.chatapp.UserInfoRoute
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@NavEdge(to = UserInfoRoute::class, label = "Member Info")
@NavDestination(route = GroupInfoRoute::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    conversationId: String,
    groupName: String,
    groupAvatarUrl: String?,
    groupDescription: String?,
    onBack: () -> Unit,
) {
    val vm: GroupInfoViewModel = koinViewModel(
        key = conversationId,
        parameters = { parametersOf(conversationId) },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(conversationId) {
        vm.setGroupHeader(groupName, groupDescription, groupAvatarUrl)
    }

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                GroupInfoEffect.NavigateBack -> onBack()
                is GroupInfoEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.onIntent(GroupInfoIntent.DismissError)
        }
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            if (bytes != null) vm.onIntent(GroupInfoIntent.PickAvatar(bytes, mimeType))
        }
    }

    val sheetState = rememberModalBottomSheetState()

    if (state.showAddMemberSheet) {
        ModalBottomSheet(
            onDismissRequest = { vm.onIntent(GroupInfoIntent.CloseAddMember) },
            sheetState = sheetState,
        ) {
            AddMemberSheet(
                query = state.addMemberQuery,
                results = state.addMemberResults,
                onQueryChange = { vm.onIntent(GroupInfoIntent.AddMemberQueryChanged(it)) },
                onAddUser = { vm.onIntent(GroupInfoIntent.AddMember(it)) },
                modifier = Modifier.padding(bottom = 32.dp),
            )
        }
    }

    val pendingUser = state.pendingAddUser
    if (state.showHistoryDialog && pendingUser != null) {
        HistoryChoiceDialog(
            userName = pendingUser.displayName,
            onSeeHistory = { vm.onIntent(GroupInfoIntent.ConfirmAddMember(canSeeHistory = true)) },
            onBlankHistory = { vm.onIntent(GroupInfoIntent.ConfirmAddMember(canSeeHistory = false)) },
            onDismiss = { vm.onIntent(GroupInfoIntent.DismissHistoryDialog) },
        )
    }

    if (state.showEditDialog) {
        EditGroupDialog(
            name = state.groupName,
            description = state.groupDescription,
            onNameChange = { vm.onIntent(GroupInfoIntent.NameChanged(it)) },
            onDescChange = { vm.onIntent(GroupInfoIntent.DescriptionChanged(it)) },
            onSave = { vm.onIntent(GroupInfoIntent.SaveGroupInfo) },
            onDismiss = { vm.onIntent(GroupInfoIntent.CloseEditDialog) },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Info del grupo") },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            if (state.isCurrentUserAdmin) {
                                DropdownMenuItem(
                                    text = { Text("Editar grupo") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        vm.onIntent(GroupInfoIntent.OpenEditDialog)
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Salir del grupo", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.ExitToApp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    vm.onIntent(GroupInfoIntent.LeaveGroup)
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                GroupHeader(
                    name = state.groupName,
                    description = state.groupDescription,
                    avatarUrl = state.groupAvatarUrl,
                    isAdmin = state.isCurrentUserAdmin,
                    isSaving = state.isSaving,
                    onPickAvatar = { avatarPickerLauncher.launch("image/*") },
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${state.members.size} participantes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.isCurrentUserAdmin) {
                        IconButton(onClick = { vm.onIntent(GroupInfoIntent.OpenAddMember) }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Añadir miembro")
                        }
                    }
                }
            }

            val adminCount = state.members.count { it.role == GroupRole.ADMIN }
            items(state.members, key = { it.userId }) { member ->
                MemberItem(
                    member = member,
                    isCurrentUser = member.userId == state.currentUserId,
                    isCurrentUserAdmin = state.isCurrentUserAdmin,
                    isLastAdmin = member.role == GroupRole.ADMIN && adminCount == 1,
                    onRemove = { vm.onIntent(GroupInfoIntent.RemoveMember(member.userId)) },
                    onPromote = { vm.onIntent(GroupInfoIntent.PromoteMember(member.userId)) },
                    onDemote = { vm.onIntent(GroupInfoIntent.DemoteMember(member.userId)) },
                )
            }

        }
    }
}

@Composable
private fun GroupHeader(
    name: String,
    description: String,
    avatarUrl: String?,
    isAdmin: Boolean,
    isSaving: Boolean,
    onPickAvatar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable(enabled = isAdmin, onClick = onPickAvatar),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                if (isSaving) {
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
            if (isAdmin) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Cambiar foto",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        if (description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MemberItem(
    member: GroupMemberBO,
    isCurrentUser: Boolean,
    isCurrentUserAdmin: Boolean,
    isLastAdmin: Boolean,
    onRemove: () -> Unit,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatAppAvatar(
            name = member.displayName,
            url = member.avatarUrl,
            size = 44.dp,
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isCurrentUser) "${member.displayName} (Tú)" else member.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (member.role == GroupRole.ADMIN) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            "Admin",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            if (member.username.isNotBlank()) {
                Text(
                    "@${member.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (isCurrentUserAdmin && !isCurrentUser) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    if (member.role == GroupRole.MEMBER) {
                        DropdownMenuItem(
                            text = { Text("Hacer admin") },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                            onClick = { showMenu = false; onPromote() },
                        )
                    } else if (!isLastAdmin) {
                        DropdownMenuItem(
                            text = { Text("Quitar admin") },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                            onClick = { showMenu = false; onDemote() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Eliminar del grupo", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Default.PersonRemove, contentDescription = null,
                                tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = { showMenu = false; onRemove() },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryChoiceDialog(
    userName: String,
    onSeeHistory: () -> Unit,
    onBlankHistory: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historial de mensajes") },
        text = {
            Text("¿Puede $userName ver los mensajes anteriores a su entrada al grupo?")
        },
        confirmButton = {
            TextButton(onClick = onSeeHistory) { Text("Sí, ver historial") }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onBlankHistory) {
                    Text("No, historial vacío", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}

@Composable
private fun EditGroupDialog(
    name: String,
    description: String,
    onNameChange: (String) -> Unit,
    onDescChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar grupo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChatAppTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = "Nombre del grupo",
                    modifier = Modifier.fillMaxWidth(),
                )
                ChatAppTextField(
                    value = description,
                    onValueChange = onDescChange,
                    label = "Descripción",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = name.isNotBlank()) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun AddMemberSheet(
    query: String,
    results: List<UserBO>,
    onQueryChange: (String) -> Unit,
    onAddUser: (UserBO) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.imePadding()) {
        Text(
            "Añadir participante",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        ChatAppTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = "Buscar por nombre o usuario…",
            leadingIcon = Icons.Default.Search,
        )
        Spacer(Modifier.height(8.dp))
        if (results.isEmpty() && query.isNotBlank()) {
            Text(
                "Sin resultados",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        results.forEach { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddUser(user) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChatAppAvatar(
                    name = user.displayName,
                    url = user.avatarUrl,
                    size = 40.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(user.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    if (user.username.isNotBlank()) {
                        Text("@${user.username}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
