package com.ajrpachon.chatapp.ui.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.foundation.lazy.grid.items
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.service.ActiveChatTracker
import com.ajrpachon.chatapp.ui.components.ChatMessagesSkeleton
import com.ajrpachon.chatapp.ui.components.OfflineBanner
import com.ajrpachon.chatapp.ui.theme.IncognitoAccent
import com.ajrpachon.chatapp.ui.theme.IncognitoBannerBackground
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import coil3.compose.AsyncImage
import com.ajrpachon.chatapp.domain.model.CallBO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.github.skydoves.navgraph.annotations.NavDestination
import com.github.skydoves.navgraph.annotations.NavEdge
import com.ajrpachon.chatapp.CallRoute
import com.ajrpachon.chatapp.ChatRoute
import com.ajrpachon.chatapp.GroupInfoRoute
import com.ajrpachon.chatapp.UserInfoRoute
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File


// ── Screen ───────────────────────────────────────────────────────────────────

@NavEdge(to = CallRoute::class, label = "Start Call")
@NavEdge(to = GroupInfoRoute::class, label = "Group Info")
@NavEdge(to = UserInfoRoute::class, label = "User Info")
@NavDestination(route = ChatRoute::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    otherUserName: String,
    onBack: () -> Unit,
    onStartCall: (CallBO) -> Unit = {},
    onGroupInfo: () -> Unit = {},
    onUserInfo: (userId: String) -> Unit = {},
    onOpenPdf: (url: String, filename: String) -> Unit = { _, _ -> },
    onOpenMediaGallery: () -> Unit = {},
    onNavigateToConversation: (conversationId: String, otherUserName: String) -> Unit = { _, _ -> },
) {
    val vm: ChatViewModel = koinViewModel(key = conversationId, parameters = { parametersOf(ChatArgs(conversationId, otherUserName)) })
    val state by vm.state.collectAsStateWithLifecycle()
    val lazyPagingItems = vm.messages.collectAsLazyPagingItems()
    val reactions by vm.reactions.collectAsStateWithLifecycle(initialValue = emptyMap())
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val exportConversationLabel = stringResource(R.string.chat_export_conversation)

    DisposableEffect(conversationId) {
        ActiveChatTracker.activeConversationId = conversationId
        onDispose {
            ActiveChatTracker.activeConversationId = null
        }
    }

    val scope = rememberCoroutineScope()
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    // MutableState (not `by remember`) because ChatDialogHost also reads/writes this one —
    // see its own doc for why.
    val reactionDetailMessageId = remember { mutableStateOf<String?>(null) }
    val showScrollToBottom by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

    val onScrollToMessage: (String) -> Unit = { messageId ->
        val snapshot = lazyPagingItems.itemSnapshotList
        val index = snapshot.indexOfFirst { it?.id == messageId }
        if (index >= 0) {
            scope.launch {
                listState.animateScrollToItem(index)
                highlightedMessageId = messageId
                delay(1500)
                highlightedMessageId = null
            }
        }
    }

    // Jump-to-message: when ViewModel sets a highlighted message ID, scroll to it
    LaunchedEffect(state.highlightedMessageId) {
        val id = state.highlightedMessageId ?: return@LaunchedEffect
        onScrollToMessage(id)
    }

    // MutableState (not `by remember`/`by rememberSaveable`) because ChatDialogHost also
    // reads/writes these — see its own doc for why.
    val viewerUrls = remember { mutableStateOf<List<String>>(emptyList()) }
    val viewerInitialIndex = rememberSaveable { mutableStateOf(0) }
    val showViewer = rememberSaveable { mutableStateOf(false) }

    // Tracks whether a send-triggered scroll is pending (waits for Paging to deliver the new item).
    val pendingSendScroll = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                is ChatEffect.NavigateToCall -> onStartCall(effect.call)
                // Mark a scroll pending; the itemCount LaunchedEffect will execute it once the
                // new message arrives in the list (avoids scrolling before Paging delivers it).
                ChatEffect.ScrollToBottom -> pendingSendScroll.value = true
                ChatEffect.NavigateBack -> onBack()
                is ChatEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is ChatEffect.ShowShareSheet -> {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_STREAM, effect.uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, exportConversationLabel))
                }
                is ChatEffect.NavigateToConversation -> onNavigateToConversation(effect.conversationId, effect.otherUserName)
                is ChatEffect.InviteContact -> {
                    val smsIntent = android.content.Intent(
                        android.content.Intent.ACTION_SENDTO,
                        android.net.Uri.parse("smsto:${effect.phoneNumber}"),
                    ).apply { putExtra("sms_body", effect.text) }
                    context.startActivity(smsIntent)
                }
            }
        }
    }

    val initialScrollDone = remember { mutableStateOf(false) }

    // Initial scroll: wait for the refresh to fully complete before scrolling to bottom.
    LaunchedEffect(conversationId) {
        snapshotFlow { lazyPagingItems.loadState.refresh }
            .first { it is LoadState.NotLoading }
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { it > 0 }
        withFrameNanos { }
        listState.scrollToItem(0)
        initialScrollDone.value = true
    }

    // Subsequent scrolls: new sent messages and incoming messages while near the bottom.
    LaunchedEffect(lazyPagingItems.itemCount) {
        if (!initialScrollDone.value) return@LaunchedEffect
        val count = lazyPagingItems.itemCount
        if (count == 0) return@LaunchedEffect

        if (pendingSendScroll.value) {
            // New message sent — item just arrived in PagingData, scroll to it now.
            snapshotFlow { listState.layoutInfo.totalItemsCount }
                .first { it > 0 }
            withFrameNanos { }
            listState.animateScrollToItem(0)
            pendingSendScroll.value = false
        } else if (listState.firstVisibleItemIndex <= 3) {
            // Someone else sent while we're near the bottom — follow the conversation.
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.onIntent(ChatIntent.DismissError)
        }
    }

    // MutableState (not `by remember`) because ChatDialogHost also reads/writes this — see
    // its own doc for why.
    val showDeleteSelectionConfirm = remember { mutableStateOf(false) }

    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var videoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> if (uris.isNotEmpty()) vm.onIntent(ChatIntent.SendImages(uris)) }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.onIntent(ChatIntent.SendFile(uri)) }

    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success -> if (success) videoUri?.let { vm.onIntent(ChatIntent.SendVideo(it)) } }

    val videoPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            videoUri = createVideoUri(context)
            videoUri?.let { videoLauncher.launch(it) }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraUri?.let { vm.onIntent(ChatIntent.SendImages(listOf(it))) } }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraUri = createCameraUri(context)
            cameraUri?.let { cameraLauncher.launch(it) }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.onIntent(ChatIntent.StartRecording)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.onIntent(ChatIntent.FetchAndSendLocation)
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            vm.onIntent(ChatIntent.ContactSelected(uri))
        }
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactPickerLauncher.launch(null)
        }
    }

    ChatDialogHost(
        state = state,
        vm = vm,
        conversationId = conversationId,
        reactions = reactions,
        showDeleteSelectionConfirm = showDeleteSelectionConfirm,
        showViewer = showViewer,
        viewerUrls = viewerUrls,
        viewerInitialIndex = viewerInitialIndex,
        reactionDetailMessageId = reactionDetailMessageId,
    )

    val chatThemeColors = state.chatTheme.toColors()

    val latestPinned = state.latestPinnedMessage
    var pinnedBannerVisible by rememberSaveable(latestPinned?.id) { mutableStateOf(true) }

    val scaffoldContainerColor = if (chatThemeColors.backgroundTint == androidx.compose.ui.graphics.Color.Transparent) {
        MaterialTheme.colorScheme.background
    } else {
        chatThemeColors.backgroundTint
    }

    Scaffold(
        containerColor = scaffoldContainerColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
            if (state.isIncognito) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IncognitoBannerBackground)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.chat_incognito_banner),
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                }
            }
            if (state.isMultiSelectActive) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { vm.onIntent(ChatIntent.ClearSelection) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.chat_cancel_selection))
                        }
                    },
                    title = { Text(stringResource(R.string.chat_selected_count, state.selectedMessageIds.size)) },
                    actions = {
                        IconButton(onClick = { vm.onIntent(ChatIntent.ShowForwardSelectionDialog) }) {
                            Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = stringResource(R.string.chat_forward_selected))
                        }
                        IconButton(onClick = { showDeleteSelectionConfirm.value = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.chat_delete_selected))
                        }
                    },
                )
            } else TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = dropUnlessResumed {
                                if (state.isGroup) onGroupInfo()
                                else state.otherUserId?.let { onUserInfo(it) }
                            })
                            .padding(vertical = 4.dp),
                    ) {
                        val avatarUrl = if (state.isGroup) state.groupAvatarUrl else state.otherUserAvatarUrl
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = stringResource(R.string.chat_photo_of, state.conversationTitle),
                                modifier = Modifier.size(36.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (state.isGroup) {
                                    Icon(
                                        Icons.Default.Group,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp),
                                    )
                                } else {
                                    Text(
                                        text = state.conversationTitle.firstOrNull()?.uppercase() ?: "?",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = state.conversationTitle.ifBlank { stringResource(R.string.chat_title_default) },
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .basicMarquee(iterations = 1),
                                )
                                if (state.disappearingModeSeconds > 0L) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = stringResource(R.string.chat_disappearing_mode_active),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(11.dp),
                                    )
                                }
                            }
                            val secondaryLine = state.presenceText ?: state.subtitleText
                            secondaryLine?.let { line ->
                                val isActive = state.isOtherUserOnline || state.onlineMemberCount > 0
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    color = if (isActive)
                                        MaterialTheme.colorScheme.tertiary
                                    else
                                        MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.chat_back))
                    }
                },
                actions = {
                    if (state.scheduledMessageCount > 0) {
                        Box {
                            IconButton(onClick = { vm.onIntent(ChatIntent.ShowScheduledSheet) }) {
                                Icon(Icons.Default.Schedule, contentDescription = stringResource(R.string.chat_scheduled_messages))
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(16.dp)
                                    .background(MaterialTheme.colorScheme.error, androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = state.scheduledMessageCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onError,
                                )
                            }
                        }
                    }
                    if (state.isGroup || state.otherUserId != null) {
                        var callMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { callMenuExpanded = true }) {
                                Icon(Icons.Default.PhoneInTalk, contentDescription = stringResource(R.string.chat_call))
                            }
                            DropdownMenu(
                                expanded = callMenuExpanded,
                                onDismissRequest = { callMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (state.isGroup) stringResource(R.string.chat_group_voice_call)
                                            else stringResource(R.string.chat_voice_call)
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                    onClick = {
                                        callMenuExpanded = false
                                        vm.onIntent(ChatIntent.StartCall("audio"))
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (state.isGroup) stringResource(R.string.chat_group_video_call)
                                            else stringResource(R.string.chat_video_call)
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                                    onClick = {
                                        callMenuExpanded = false
                                        vm.onIntent(ChatIntent.StartCall("video"))
                                    },
                                )
                            }
                        }
                    }
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.chat_more_options))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_search_messages)) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    vm.onIntent(ChatIntent.OpenSearch)
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(if (state.isMuted) stringResource(R.string.chat_enable_notifications) else stringResource(R.string.chat_mute))
                                },
                                leadingIcon = {
                                    Icon(
                                        if (state.isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    if (state.isMuted) {
                                        vm.onIntent(ChatIntent.MuteFor(0L))
                                    } else {
                                        vm.onIntent(ChatIntent.ShowMuteDialog)
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_chat_theme)) },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    vm.onIntent(ChatIntent.OpenThemePicker)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_wallpaper)) },
                                leadingIcon = { Icon(Icons.Default.Wallpaper, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    vm.onIntent(ChatIntent.OpenWallpaperPicker)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_export_conversation)) },
                                leadingIcon = {
                                    if (state.isExporting) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Share, contentDescription = null)
                                    }
                                },
                                enabled = !state.isExporting,
                                onClick = {
                                    menuExpanded = false
                                    vm.onIntent(ChatIntent.ExportConversation)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_disappearing_mode)) },
                                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    vm.onIntent(ChatIntent.ShowDisappearingModeSheet)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (state.isIncognito) stringResource(R.string.chat_disable_incognito) else stringResource(R.string.chat_incognito_mode)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (state.isIncognito) IncognitoAccent else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    vm.onIntent(ChatIntent.ToggleIncognito)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_shared_media)) },
                                leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenMediaGallery()
                                },
                            )
                            if (state.isGroup) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.chat_group_info)) },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onGroupInfo()
                                    },
                                )
                                if (state.isCurrentUserMember) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.chat_leave_group), color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ExitToApp,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            vm.onIntent(ChatIntent.LeaveGroup)
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
            if (latestPinned != null && pinnedBannerVisible) {
                PinnedMessageBanner(
                    message = latestPinned,
                    pinnedCount = state.pinnedMessages.size,
                    onTap = {
                        onScrollToMessage(latestPinned.id)
                    },
                    onDismiss = { vm.onIntent(ChatIntent.UnpinMessage(latestPinned.id)) },
                )
            }
            } // Column wrapper for topBar (incognito banner + app bar)
        },
        bottomBar = {
            Column {
                androidx.compose.animation.AnimatedVisibility(visible = !state.isOnline) {
                    com.ajrpachon.chatapp.ui.components.OfflineBanner()
                }
            if (state.isCurrentUserMember) Surface(shadowElevation = 4.dp) {
                Column(modifier = Modifier.windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))) {
                if (!state.isOnline) {
                    OfflineBanner()
                }
                val editingMessage = state.editingMessage
                if (editingMessage != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(stringResource(R.string.chat_editing_message), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(editingMessage.content.take(60), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        IconButton(onClick = { vm.onIntent(ChatIntent.CancelEdit) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.chat_cancel_edit))
                        }
                    }
                    HorizontalDivider()
                }
                val replyingTo = state.replyingTo
                if (replyingTo != null) {
                    ReplyPreviewBar(
                        message = replyingTo,
                        onCancel = { vm.onIntent(ChatIntent.CancelReply) },
                    )
                    HorizontalDivider()
                }
                // Typing indicator
                val typingUserNames = state.typingUserNames
                AnimatedVisibility(
                    visible = typingUserNames.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    val typingText = when (typingUserNames.size) {
                        1 -> stringResource(R.string.chat_typing_single, typingUserNames[0])
                        else -> stringResource(R.string.chat_typing_multiple, typingUserNames.take(2).joinToString(" y "))
                    }
                    Text(
                        text = typingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    )
                }
                val audioState = state.audioState
                when {
                    audioState.isRecording -> RecordingBar(
                        durationMs = audioState.recordingDurationMs,
                        amplitudeHistory = audioState.amplitudeHistory,
                        onStop = { vm.onIntent(ChatIntent.StopRecording) },
                    )
                    audioState.pendingFilePath != null -> AudioPreviewBar(
                        filePath = audioState.pendingFilePath,
                        amplitudeHistory = audioState.amplitudeHistory,
                        isUploading = audioState.isUploading,
                        onDiscard = { vm.onIntent(ChatIntent.DiscardAudio) },
                        onSend = { vm.onIntent(ChatIntent.SendAudio) },
                    )
                    else -> NormalInputBar(
                        inputText = state.inputText,
                        isSending = state.isSending,
                        isUploadingImage = state.isUploadingFile,
                        mediaUploadProgress = state.mediaUploadProgress,
                        onTextChange = { vm.onIntent(ChatIntent.InputChanged(it)) },
                        onSend = { vm.onIntent(ChatIntent.Send) },
                        onGallery = {
                            galleryLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onCamera = {
                            when {
                                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                        == PackageManager.PERMISSION_GRANTED -> {
                                    cameraUri = createCameraUri(context)
                                    cameraUri?.let { cameraLauncher.launch(it) }
                                }
                                else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onMic = {
                            when {
                                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                        == PackageManager.PERMISSION_GRANTED -> {
                                    vm.onIntent(ChatIntent.StartRecording)
                                }
                                else -> audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onSticker = { vm.onIntent(ChatIntent.OpenStickerPicker) },
                        onAttachFile = { fileLauncher.launch(arrayOf("*/*")) },
                        onAttachVideo = {
                            when {
                                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                        == PackageManager.PERMISSION_GRANTED -> {
                                    videoUri = createVideoUri(context)
                                    videoUri?.let { videoLauncher.launch(it) }
                                }
                                else -> videoPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onLocation = {
                            when {
                                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                        == PackageManager.PERMISSION_GRANTED -> {
                                    vm.onIntent(ChatIntent.FetchAndSendLocation)
                                }
                                else -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        onContact = {
                            when {
                                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                                        == PackageManager.PERMISSION_GRANTED -> contactPickerLauncher.launch(null)
                                else -> contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        onSchedule = { vm.onIntent(ChatIntent.OpenScheduleDialog) },
                        onAi = { vm.onIntent(ChatIntent.OpenAiSheet) },
                        onCreatePoll = { vm.onIntent(ChatIntent.OpenCreatePollSheet) },
                    )
                }
                } // Column
            }
            } // end outer Column (bottomBar)
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    state.wallpaperColor?.let { androidx.compose.ui.graphics.Color(it) }
                        ?: MaterialTheme.colorScheme.background
                )
        ) {
            val isInitialLoad = lazyPagingItems.loadState.refresh is LoadState.Loading
                && lazyPagingItems.itemCount == 0
            if (isInitialLoad) {
                ChatMessagesSkeleton(
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = innerPadding.calculateBottomPadding() + 8.dp,
                        start = 16.dp,
                        end = 16.dp,
                    ),
                )
            }
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 8.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
                // No verticalArrangement spacing here on purpose: hidden items (suppressed
                // in-flight batch messages, isInsideGroup images) render nothing but still
                // occupy a slot for key stability. Arrangement.spacedBy adds its gap between
                // every pair of items regardless of their rendered height, so N hidden items
                // would stack N gaps of empty space. Spacing is applied per-item below instead,
                // only on items that actually render content.
            ) {
                // reverseLayout=true → the first item in this content lambda sits at the visual
                // bottom (newest). Rendering the placeholder here keeps it "below" real messages
                // without needing negative indices into the paging data.
                if (state.pendingImageUris.isNotEmpty()) {
                    item(key = "pending-image-batch") {
                        Box(Modifier.padding(top = 8.dp)) {
                            PendingImageBatchBubble(
                                uris = state.pendingImageUris,
                                progress = state.mediaUploadProgress,
                            )
                        }
                    }
                }
                items(
                    count = lazyPagingItems.itemCount,
                    key = lazyPagingItems.itemKey { it.id },
                ) { index ->
                    val message = lazyPagingItems[index] ?: return@items
                    // Messages from the batch currently rendered by the placeholder above are
                    // hidden here to avoid a duplicate/jumping bubble while uploads are in flight.
                    if (message.id in state.suppressedImageMessageIds) return@items

                    // Determine if this item is already covered by a group rendered at a lower index.
                    // With reverseLayout=true and DESC order, index 0 = newest (bottom).
                    // Index i-1 was composed before i, so it's already in the snapshot.
                    // Skip back over any suppressed (in-flight batch) messages so an older, already
                    // finished group isn't mistakenly treated as continuing into the new batch.
                    var prevIndex = index - 1
                    while (prevIndex >= 0 && lazyPagingItems[prevIndex]?.id in state.suppressedImageMessageIds) prevIndex--
                    val prevMessage = if (prevIndex >= 0) lazyPagingItems[prevIndex] else null
                    val isInsideGroup = prevMessage != null
                        && message.imageUrl != null && message.audioUrl == null
                        && prevMessage.imageUrl != null && prevMessage.audioUrl == null
                        && prevMessage.senderId == message.senderId

                    if (!isInsideGroup) {
                    Box(Modifier.padding(top = 8.dp)) {
                        val isImageGroupStart = message.imageUrl != null && message.audioUrl == null
                        if (isImageGroupStart) {
                            // Collect consecutive images from the same sender starting at this index.
                            // Accessing lazyPagingItems[j] triggers loading of the next page if j is
                            // near the page boundary — ensuring the group is always complete.
                            val group = mutableListOf(message)
                            var j = index + 1
                            while (j < lazyPagingItems.itemCount) {
                                val next = lazyPagingItems[j] ?: break
                                if (next.id in state.suppressedImageMessageIds) break
                                if (next.imageUrl != null && next.audioUrl == null && next.senderId == message.senderId) {
                                    group.add(next)
                                    j++
                                } else break
                            }
                            if (group.size > 2) {
                                ImageGroupBubble(
                                    messages = group,
                                    onImageClick = { idx ->
                                        viewerUrls.value = group.mapNotNull { it.imageUrl }
                                        viewerInitialIndex.value = idx
                                        showViewer.value = true
                                    },
                                    onReply = { vm.onIntent(ChatIntent.SetReply(group.first())) },
                                )
                            } else {
                                MessageBubble(
                                    message = message,
                                    isGroup = state.isGroup,
                                    onImageClick = { url ->
                                        viewerUrls.value = listOf(url)
                                        viewerInitialIndex.value = 0
                                        showViewer.value = true
                                    },
                                    onReply = { vm.onIntent(ChatIntent.SetReply(message)) },
                                    isHighlighted = message.id == highlightedMessageId,
                                    onReplyClick = onScrollToMessage,
                                    onDelete = if (message.isFromMe) {{ vm.onIntent(ChatIntent.DeleteMessage(message.id)) }} else null,
                                    onEdit = if (message.isFromMe && message.content.isNotBlank()) {{ vm.onIntent(ChatIntent.StartEdit(message)) }} else null,
                                    onSelfDestruct = if (message.isFromMe) {{ vm.onIntent(ChatIntent.ShowExpiryDialog(message.id)) }} else null,
                                    isSelected = message.id in state.selectedMessageIds,
                                    isMultiSelectActive = state.isMultiSelectActive,
                                    onToggleSelect = { vm.onIntent(ChatIntent.ToggleMessageSelection(message.id)) },
                                    onForward = { vm.onIntent(ChatIntent.ShowForwardDialog(message)) },
                                    outgoingBubbleColor = chatThemeColors.bubbleColor,
                                    onOpenPdf = onOpenPdf,
                                    onVote = { optionId -> vm.onIntent(ChatIntent.VotePoll(message.content.removePrefix("poll:"), optionId)) },
                                    onRetryMessage = { vm.onIntent(ChatIntent.RetryMessage(it)) },
                                    onCopy = { vm.onIntent(ChatIntent.CopyMessageContent(it)) },
                                    contactPhoneLookups = contactPhoneOf(message.content)?.let { phone ->
                                        state.contactPhoneLookups[phone]?.let { mapOf(phone to it) }
                                    } ?: emptyMap(),
                                    onCheckContactRelationship = { vm.onIntent(ChatIntent.CheckContactRelationship(it)) },
                                    onContactCardPrimaryAction = { vm.onIntent(ChatIntent.ContactCardPrimaryAction(it)) },
                                    pollUiStates = pollIdOf(message.content)?.let { id ->
                                        state.pollUiStates[id]?.let { mapOf(id to it) }
                                    } ?: emptyMap(),
                                    onObservePoll = { pollId -> vm.onIntent(ChatIntent.ObservePoll(pollId)) },
                                    linkPreviews = state.linkPreviews,
                                    onDetectedUrl = { url -> vm.onIntent(ChatIntent.DetectedUrlChanged(url)) },
                                )
                            }
                        } else {
                            MessageBubble(
                                message = message,
                                isGroup = state.isGroup,
                                onImageClick = { url ->
                                    viewerUrls.value = listOf(url)
                                    viewerInitialIndex.value = 0
                                    showViewer.value = true
                                },
                                onReply = { vm.onIntent(ChatIntent.SetReply(message)) },
                                isHighlighted = message.id == highlightedMessageId,
                                onReplyClick = onScrollToMessage,
                                onDelete = if (message.isFromMe) {{ vm.onIntent(ChatIntent.DeleteMessage(message.id)) }} else null,
                                onEdit = if (message.isFromMe && message.content.isNotBlank()) {{ vm.onIntent(ChatIntent.StartEdit(message)) }} else null,
                                onSelfDestruct = if (message.isFromMe) {{ vm.onIntent(ChatIntent.ShowExpiryDialog(message.id)) }} else null,
                                onForward = { vm.onIntent(ChatIntent.ShowForwardDialog(message)) },
                                messageReactions = reactions[message.id] ?: emptyList(),
                                currentUserId = state.currentUserId,
                                onToggleReaction = { emoji -> vm.onIntent(ChatIntent.ToggleReaction(message.id, emoji)) },
                                isSelected = message.id in state.selectedMessageIds,
                                isMultiSelectActive = state.isMultiSelectActive,
                                onToggleSelect = { vm.onIntent(ChatIntent.ToggleMessageSelection(message.id)) },
                                outgoingBubbleColor = chatThemeColors.bubbleColor,
                                onOpenPdf = onOpenPdf,
                                onVote = { optionId -> vm.onIntent(ChatIntent.VotePoll(message.content.removePrefix("poll:"), optionId)) },
                                onShowReactionDetails = { reactionDetailMessageId.value = message.id },
                                onRetryMessage = { vm.onIntent(ChatIntent.RetryMessage(it)) },
                                onCopy = { vm.onIntent(ChatIntent.CopyMessageContent(it)) },
                                contactPhoneLookups = contactPhoneOf(message.content)?.let { phone ->
                                    state.contactPhoneLookups[phone]?.let { mapOf(phone to it) }
                                } ?: emptyMap(),
                                onCheckContactRelationship = { vm.onIntent(ChatIntent.CheckContactRelationship(it)) },
                                onContactCardPrimaryAction = { vm.onIntent(ChatIntent.ContactCardPrimaryAction(it)) },
                                pollUiStates = pollIdOf(message.content)?.let { id ->
                                    state.pollUiStates[id]?.let { mapOf(id to it) }
                                } ?: emptyMap(),
                                onObservePoll = { pollId -> vm.onIntent(ChatIntent.ObservePoll(pollId)) },
                                linkPreviews = state.linkPreviews,
                                onDetectedUrl = { url -> vm.onIntent(ChatIntent.DetectedUrlChanged(url)) },
                            )
                        }
                    }
                    }
                    // isInsideGroup → render nothing; slot still exists for key stability + paging trigger
                }
                // With reverseLayout=true, this item appears at the visual TOP — shown while
                // loading older pages as the user scrolls up through history.
                item(key = "paging-load-more") {
                    if (lazyPagingItems.loadState.append is LoadState.Loading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = showScrollToBottom,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp),
            ) {
                SmallFloatingActionButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.chat_scroll_to_bottom))
                }
            }
            if (state.isSearchActive) {
                MessageSearchOverlay(
                    query = state.searchQuery,
                    results = state.searchResults,
                    isSearching = state.isSearching,
                    topPadding = innerPadding.calculateTopPadding(),
                    onQueryChange = { vm.onIntent(ChatIntent.SearchQueryChanged(it)) },
                    onClose = { vm.onIntent(ChatIntent.CloseSearch) },
                    onJump = { vm.onIntent(ChatIntent.JumpToMessage(it)) },
                )
            }
        }
    }
}

private fun createCameraUri(context: Context): Uri {
    val file = File.createTempFile("img_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun createVideoUri(context: Context): Uri {
    val file = File.createTempFile("vid_", ".mp4", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

// The message-bubble rendering tree (MessageBubble + everything it dispatches to) was
// extracted per docs/chat-viewmodel-decomposition.md Phase 2:
// - MessageBubble.kt: pollIdOf/contactPhoneOf, ReplySelectContainer, ChatBubbleSlot, MessageBubble
// - ChatFileBubbles.kt: CallMessageBubble, FileBubble, PdfFileCard, GenericFileBubble,
//   formatFileSize, VideoBubble, StickerBubble, DeletedMessageBubble
// - ChatBubbleContent.kt: MessageFooterContent, LocationMessageCard, ReadReceiptIcon,
//   SendStatusIcon, MediaMetaOverlay, ReplyQuote, LinkPreviewCard
// - ChatImageGroupBubbles.kt: PendingImageBatchBubble, ImageGroupBubble
// - ContactBubble.kt: ContactHeader, ContactBubble
// - PollBubble.kt: PollBubble, PollOptionRow

// ReactionDetailsSheet, WallpaperPickerSheet extracted to ChatBottomSheets.kt
