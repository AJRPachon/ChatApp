package com.ajrpachon.chatapp.ui.group

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.ui.components.ChatAppAvatar
import com.ajrpachon.chatapp.ui.components.ChatAppPrimaryButton
import com.ajrpachon.chatapp.ui.components.ChatAppTextField
import com.github.skydoves.navgraph.annotations.NavDestination
import com.github.skydoves.navgraph.annotations.NavEdge
import com.ajrpachon.chatapp.ChatRoute
import com.ajrpachon.chatapp.CreateGroupRoute
import org.koin.androidx.compose.koinViewModel

@NavEdge(to = ChatRoute::class, label = "Group Created")
@NavDestination(route = CreateGroupRoute::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onGroupCreated: (conversationId: String, name: String) -> Unit,
) {
    val vm: CreateGroupViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is CreateGroupEffect.NavigateToChat -> onGroupCreated(effect.conversationId, effect.name)
                CreateGroupEffect.GoBack -> onBack()
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.onIntent(CreateGroupIntent.DismissError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.step == CreateGroupStep.SELECT_MEMBERS) "Nuevo grupo" else "Nombre del grupo")
                },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { vm.onIntent(CreateGroupIntent.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { innerPadding ->
        when (state.step) {
            CreateGroupStep.SELECT_MEMBERS -> SelectMembersStep(
                state = state,
                onIntent = vm::onIntent,
                modifier = Modifier.padding(innerPadding),
            )
            CreateGroupStep.SET_INFO -> SetGroupInfoStep(
                state = state,
                onIntent = vm::onIntent,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectMembersStep(
    state: CreateGroupState,
    onIntent: (CreateGroupIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.selectedUsers.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.selectedUsers.forEach { user ->
                    InputChip(
                        selected = true,
                        onClick = { onIntent(CreateGroupIntent.ToggleUser(user)) },
                        label = { Text(user.displayName) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Quitar", modifier = Modifier.size(16.dp))
                        },
                    )
                }
            }
        }

        ChatAppTextField(
            value = state.query,
            onValueChange = { onIntent(CreateGroupIntent.QueryChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = "Buscar personas…",
            leadingIcon = Icons.Default.Search,
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.searchResults, key = { it.id }) { user ->
                UserSearchItem(
                    user = user,
                    isSelected = state.selectedUsers.any { it.id == user.id },
                    onClick = { onIntent(CreateGroupIntent.ToggleUser(user)) },
                )
            }
        }

        ChatAppPrimaryButton(
            text = "Siguiente (${state.selectedUsers.size} seleccionados)",
            onClick = { onIntent(CreateGroupIntent.Next) },
            enabled = state.selectedUsers.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(16.dp),
        )
    }
}

@Composable
private fun SetGroupInfoStep(
    state: CreateGroupState,
    onIntent: (CreateGroupIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(),
    ) {
        ChatAppTextField(
            value = state.groupName,
            onValueChange = { onIntent(CreateGroupIntent.NameChanged(it)) },
            label = "Nombre del grupo",
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        ChatAppTextField(
            value = state.groupDescription,
            onValueChange = { onIntent(CreateGroupIntent.DescriptionChanged(it)) },
            label = "Descripción (opcional)",
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3,
        )

        Spacer(Modifier.weight(1f))

        ChatAppPrimaryButton(
            text = "Crear grupo",
            onClick = { onIntent(CreateGroupIntent.Create) },
            enabled = state.groupName.isNotBlank() && !state.isLoading,
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun UserSearchItem(
    user: UserBO,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(40.dp)) {
            ChatAppAvatar(
                name = user.displayName,
                url = user.avatarUrl,
                size = 40.dp,
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(user.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (user.username.isNotBlank()) {
                Text(
                    "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
