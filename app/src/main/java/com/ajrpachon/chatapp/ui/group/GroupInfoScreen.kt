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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.GroupRole
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.ui.components.ChatAppAvatar
import com.ajrpachon.chatapp.ui.components.ChatAppSearchField
import com.ajrpachon.chatapp.ui.components.ChatAppTopBar
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
                is GroupInfoEffect.CopyToClipboard -> { /* handled inline via LocalClipboardManager */ }
                is GroupInfoEffect.ShareInviteLink -> {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, effect.url)
                    }
                    context.startActivity(
                        android.content.Intent.createChooser(
                            shareIntent,
                            context.getString(R.string.group_share_intent_title),
                        ),
                    )
                }
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

    if (state.showInviteLinkSheet && state.inviteLink != null) {
        val clipboardManager = LocalClipboardManager.current
        ModalBottomSheet(
            onDismissRequest = { vm.onIntent(GroupInfoIntent.DismissInviteLinkSheet) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .navigationBarsPadding(),
            ) {
                Text(
                    stringResource(R.string.group_invite_link_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        state.inviteLink.orEmpty(),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val link = state.inviteLink ?: return@OutlinedButton
                            clipboardManager.setText(AnnotatedString(link))
                            vm.onIntent(GroupInfoIntent.DismissInviteLinkSheet)
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.group_copy)) }
                    Button(
                        onClick = { vm.onIntent(GroupInfoIntent.ShareInviteLink) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.group_share)) }
                }
            }
        }
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
            ChatAppTopBar(
                title = stringResource(R.string.group_info_title),
                onBack = onBack,
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.group_more_options))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            if (state.isCurrentUserAdmin) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.group_edit_group)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        vm.onIntent(GroupInfoIntent.OpenEditDialog)
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.group_leave_group), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp,
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
                        stringResource(R.string.group_participants_count, state.members.size),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.isCurrentUserAdmin) {
                        IconButton(onClick = { vm.onIntent(GroupInfoIntent.OpenAddMember) }) {
                            Icon(Icons.Default.AddCircle, contentDescription = stringResource(R.string.group_add_member_content_description))
                        }
                    }
                }
            }

            items(state.members, key = { it.userId }) { member ->
                MemberItem(
                    member = member,
                    isCurrentUser = member.userId == state.currentUserId,
                    isCurrentUserAdmin = state.isCurrentUserAdmin,
                    isLastAdmin = state.isLastAdmin(member),
                    onRemove = { vm.onIntent(GroupInfoIntent.RemoveMember(member.userId)) },
                    onPromote = { vm.onIntent(GroupInfoIntent.PromoteMember(member.userId)) },
                    onDemote = { vm.onIntent(GroupInfoIntent.DemoteMember(member.userId)) },
                )
            }

            if (state.isCurrentUserAdmin) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.group_invite_link_headline)) },
                        supportingContent = { Text(stringResource(R.string.group_invite_link_supporting)) },
                        leadingContent = { Icon(Icons.Default.Link, contentDescription = null) },
                        modifier = Modifier.clickable { vm.onIntent(GroupInfoIntent.GenerateInviteLink) },
                    )
                }
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
                        contentDescription = stringResource(R.string.group_change_photo_content_description),
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
                    text = if (isCurrentUser) {
                        stringResource(R.string.group_you_suffix, member.displayName)
                    } else {
                        member.displayName
                    },
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
                            stringResource(R.string.group_admin_badge),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            if (member.username.isNotBlank()) {
                Text(
                    stringResource(R.string.group_username, member.username),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (isCurrentUserAdmin && !isCurrentUser) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.group_options_content_description))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    if (member.role == GroupRole.MEMBER) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.group_make_admin)) },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                            onClick = { showMenu = false; onPromote() },
                        )
                    } else if (!isLastAdmin) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.group_remove_admin)) },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                            onClick = { showMenu = false; onDemote() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.group_remove_from_group), color = MaterialTheme.colorScheme.error) },
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
        title = { Text(stringResource(R.string.group_history_dialog_title)) },
        text = {
            Text(stringResource(R.string.group_history_dialog_message, userName))
        },
        confirmButton = {
            TextButton(onClick = onSeeHistory) { Text(stringResource(R.string.group_history_see)) }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onBlankHistory) {
                    Text(stringResource(R.string.group_history_blank), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.group_history_cancel)) }
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
        title = { Text(stringResource(R.string.group_edit_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChatAppTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.group_name_label),
                )
                ChatAppTextField(
                    value = description,
                    onValueChange = onDescChange,
                    label = stringResource(R.string.group_description_label),
                    singleLine = false,
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.group_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.group_cancel)) }
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
            stringResource(R.string.group_add_participant_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        ChatAppSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = stringResource(R.string.group_search_placeholder),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        if (results.isEmpty() && query.isNotBlank()) {
            Text(
                stringResource(R.string.group_no_results),
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
                        Text(stringResource(R.string.group_username, user.username), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
