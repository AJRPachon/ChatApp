package com.ajrpachon.chatapp.ui.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import coil3.compose.AsyncImage
import com.ajrpachon.chatapp.domain.model.CallBO
import com.ajrpachon.chatapp.domain.model.MediaUrlValidator
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.MessageLimits
import com.ajrpachon.chatapp.domain.model.StickerValidation
import com.ajrpachon.chatapp.utils.ClipboardProtection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.github.skydoves.navgraph.annotations.NavDestination
import com.github.skydoves.navgraph.annotations.NavEdge
import com.ajrpachon.chatapp.CallRoute
import com.ajrpachon.chatapp.ChatRoute
import com.ajrpachon.chatapp.GroupInfoRoute
import com.ajrpachon.chatapp.UserInfoRoute
import com.ajrpachon.chatapp.ui.components.ChatAppTextField
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
) {
    val vm: ChatViewModel = koinViewModel(key = conversationId, parameters = { parametersOf(conversationId, otherUserName) })
    val state by vm.state.collectAsStateWithLifecycle()
    val lazyPagingItems = vm.messages.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(conversationId) {
        com.ajrpachon.chatapp.service.ActiveChatTracker.activeConversationId = conversationId
        onDispose {
            com.ajrpachon.chatapp.service.ActiveChatTracker.activeConversationId = null
        }
    }

    val scope = rememberCoroutineScope()
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
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

    var viewerUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var viewerInitialIndex by remember { mutableStateOf(0) }
    var showViewer by remember { mutableStateOf(false) }

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
            }
        }
    }

    val initialScrollDone = remember { mutableStateOf(false) }

    // Initial scroll: wait for the refresh to fully complete before scrolling to bottom.
    LaunchedEffect(Unit) {
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

    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> if (uris.isNotEmpty()) vm.onIntent(ChatIntent.SendImages(context, uris)) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraUri?.let { vm.onIntent(ChatIntent.SendImages(context, listOf(it))) } }

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
            val file = File.createTempFile("audio_", ".m4a", context.cacheDir)
            vm.onIntent(ChatIntent.StartRecording(context, file.absolutePath))
        }
    }

    if (showViewer && viewerUrls.isNotEmpty()) {
        ImageViewerDialog(
            imageUrls = viewerUrls,
            initialIndex = viewerInitialIndex,
            onDismiss = { showViewer = false },
        )
    }

    if (state.showStickerPicker) {
        StickerGifPicker(
            onStickerSelected = { vm.onIntent(ChatIntent.SendSticker(it)) },
            onGifSelected = { vm.onIntent(ChatIntent.SendGif(it)) },
            onDismiss = { vm.onIntent(ChatIntent.CloseStickerPicker) },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
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
                                contentDescription = null,
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
                                Text(state.conversationTitle.ifBlank { "Chat" })
                                if (!state.isGroup) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Cifrado extremo a extremo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                            if (!state.isGroup) {
                                val presenceText = when {
                                    state.isOtherUserOnline -> "En línea"
                                    state.otherUserLastSeenMs != null -> {
                                        val lastSeenMs = state.otherUserLastSeenMs ?: 0L
                                        formatLastSeen(System.currentTimeMillis() - lastSeenMs)
                                    }
                                    else -> null
                                }
                                if (presenceText != null) {
                                    Text(
                                        text = presenceText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.isOtherUserOnline)
                                            androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                        else
                                            MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (state.isGroup) {
                        IconButton(onClick = { vm.onIntent(ChatIntent.StartCall("audio")) }) {
                            Icon(Icons.Default.Phone, contentDescription = "Llamada grupal de voz")
                        }
                        IconButton(onClick = { vm.onIntent(ChatIntent.StartCall("video")) }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Videollamada grupal")
                        }
                    } else if (state.otherUserId != null) {
                        IconButton(onClick = { vm.onIntent(ChatIntent.StartCall("audio")) }) {
                            Icon(Icons.Default.Phone, contentDescription = "Llamada de voz")
                        }
                        IconButton(onClick = { vm.onIntent(ChatIntent.StartCall("video")) }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Videollamada")
                        }
                    }
                    IconButton(onClick = { vm.onIntent(ChatIntent.OpenSearch) }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar mensajes")
                    }
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(if (state.isMuted) "Activar notificaciones" else "Silenciar")
                                },
                                leadingIcon = {
                                    Icon(
                                        if (state.isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    vm.onIntent(ChatIntent.ToggleMute)
                                },
                            )
                            if (state.isGroup) {
                                DropdownMenuItem(
                                    text = { Text("Info del grupo") },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onGroupInfo()
                                    },
                                )
                                if (state.isCurrentUserMember) {
                                    DropdownMenuItem(
                                        text = { Text("Salir del grupo", color = MaterialTheme.colorScheme.error) },
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
        },
        bottomBar = {
            if (state.isCurrentUserMember) Surface(shadowElevation = 4.dp) {
                Column {
                val replyingTo = state.replyingTo
                if (replyingTo != null) {
                    ReplyPreviewBar(
                        message = replyingTo,
                        onCancel = { vm.onIntent(ChatIntent.CancelReply) },
                    )
                    HorizontalDivider()
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
                        isUploading = audioState.isUploading,
                        onDiscard = { vm.onIntent(ChatIntent.DiscardAudio) },
                        onSend = { vm.onIntent(ChatIntent.SendAudio) },
                    )
                    else -> NormalInputBar(
                        inputText = state.inputText,
                        isSending = state.isSending,
                        isUploadingImage = state.isUploadingImage,
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
                                    val file = File.createTempFile("audio_", ".m4a", context.cacheDir)
                                    vm.onIntent(ChatIntent.StartRecording(context, file.absolutePath))
                                }
                                else -> audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onSticker = { vm.onIntent(ChatIntent.OpenStickerPicker) },
                    )
                }
                } // Column
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            val isInitialLoad = lazyPagingItems.loadState.refresh is LoadState.Loading
                && lazyPagingItems.itemCount == 0
            if (isInitialLoad) {
                com.ajrpachon.chatapp.ui.components.ChatMessagesSkeleton(
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    count = lazyPagingItems.itemCount,
                    key = lazyPagingItems.itemKey { it.id },
                ) { index ->
                    val message = lazyPagingItems[index] ?: return@items

                    // Determine if this item is already covered by a group rendered at a lower index.
                    // With reverseLayout=true and DESC order, index 0 = newest (bottom).
                    // Index i-1 was composed before i, so it's already in the snapshot.
                    val prevMessage = if (index > 0) lazyPagingItems[index - 1] else null
                    val isInsideGroup = prevMessage != null
                        && message.imageUrl != null && message.audioUrl == null
                        && prevMessage.imageUrl != null && prevMessage.audioUrl == null
                        && prevMessage.senderId == message.senderId

                    if (!isInsideGroup) {
                        val isImageGroupStart = message.imageUrl != null && message.audioUrl == null
                        if (isImageGroupStart) {
                            // Collect consecutive images from the same sender starting at this index.
                            // Accessing lazyPagingItems[j] triggers loading of the next page if j is
                            // near the page boundary — ensuring the group is always complete.
                            val group = mutableListOf(message)
                            var j = index + 1
                            while (j < lazyPagingItems.itemCount) {
                                val next = lazyPagingItems[j] ?: break
                                if (next.imageUrl != null && next.audioUrl == null && next.senderId == message.senderId) {
                                    group.add(next)
                                    j++
                                } else break
                            }
                            if (group.size > 2) {
                                ImageGroupBubble(
                                    messages = group,
                                    onImageClick = { idx ->
                                        viewerUrls = group.mapNotNull { it.imageUrl }
                                        viewerInitialIndex = idx
                                        showViewer = true
                                    },
                                    onReply = { vm.onIntent(ChatIntent.SetReply(group.first())) },
                                )
                            } else {
                                MessageBubble(
                                    message = message,
                                    isGroup = state.isGroup,
                                    onImageClick = { url ->
                                        viewerUrls = listOf(url)
                                        viewerInitialIndex = 0
                                        showViewer = true
                                    },
                                    onReply = { vm.onIntent(ChatIntent.SetReply(message)) },
                                    isHighlighted = message.id == highlightedMessageId,
                                    onReplyClick = onScrollToMessage,
                                )
                            }
                        } else {
                            MessageBubble(
                                message = message,
                                isGroup = state.isGroup,
                                onImageClick = { url ->
                                    viewerUrls = listOf(url)
                                    viewerInitialIndex = 0
                                    showViewer = true
                                },
                                onReply = { vm.onIntent(ChatIntent.SetReply(message)) },
                                isHighlighted = message.id == highlightedMessageId,
                                onReplyClick = onScrollToMessage,
                            )
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
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Ir al final")
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
                )
            }
        }
    }
}

@Composable
private fun MessageSearchOverlay(
    query: String,
    results: List<MessageBO>,
    isSearching: Boolean,
    topPadding: androidx.compose.ui.unit.Dp,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar mensajes...") },
                    singleLine = true,
                    leadingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    },
                    trailingIcon = if (query.isNotEmpty()) {
                        { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, contentDescription = "Limpiar") } }
                    } else null,
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda")
                }
            }
            HorizontalDivider()
            if (query.isNotBlank() && results.isEmpty() && !isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin resultados", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(results, key = { _, m -> m.id }) { _, message ->
                        SearchResultItem(message = message)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(message: MessageBO) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            val timeText = remember(message.createdAt) {
                val local = message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
                "%02d:%02d".format(local.hour, local.minute)
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = message.content.ifBlank { message.replySnippet() },
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Bottom bar composables ────────────────────────────────────────────────────

@Composable
private fun NormalInputBar(
    inputText: String,
    isSending: Boolean,
    isUploadingImage: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onMic: () -> Unit,
    onSticker: () -> Unit,
) {
    val busy = isUploadingImage || isSending
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconButton(onClick = onGallery, enabled = !busy) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Galería")
        }
        IconButton(onClick = onCamera, enabled = !busy) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Cámara")
        }
        IconButton(onClick = onSticker, enabled = !busy) {
            Icon(Icons.Default.EmojiEmotions, contentDescription = "Stickers y GIFs")
        }
        ChatAppTextField(
            value = inputText,
            onValueChange = { if (it.length <= MessageLimits.MAX_CONTENT_LENGTH) onTextChange(it) },
            modifier = Modifier.weight(1f),
            placeholder = "Mensaje…",
            singleLine = false,
            maxLines = 4,
            isError = inputText.length >= MessageLimits.MAX_CONTENT_LENGTH,
            supportingText = if (inputText.length >= MessageLimits.MAX_CONTENT_LENGTH - 100)
                "${inputText.length}/${MessageLimits.MAX_CONTENT_LENGTH}" else null,
        )
        if (isUploadingImage) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp).padding(8.dp))
        } else if (inputText.isNotBlank()) {
            IconButton(onClick = onSend, enabled = !isSending) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
            }
        } else {
            IconButton(onClick = onMic, enabled = !busy) {
                Icon(Icons.Default.Mic, contentDescription = "Grabar audio")
            }
        }
    }
}

@Composable
private fun RecordingBar(
    durationMs: Long,
    amplitudeHistory: List<Float>,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        val barColor = MaterialTheme.colorScheme.error
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(36.dp),
        ) {
            val barW = 4.dp.toPx()
            val gap = 3.dp.toPx()
            val step = barW + gap
            val minH = 4.dp.toPx()
            val maxH = size.height
            amplitudeHistory.forEachIndexed { i, amp ->
                val h = (minH + amp * (maxH - minH)).coerceIn(minH, maxH)
                val top = (maxH - h) / 2f
                drawRoundRect(
                    color = barColor,
                    topLeft = androidx.compose.ui.geometry.Offset(i * step, top),
                    size = androidx.compose.ui.geometry.Size(barW, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = formatAudioDuration(durationMs.toInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        IconButton(onClick = onStop) {
            Icon(
                Icons.Default.Stop,
                contentDescription = "Detener grabación",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun AudioPreviewBar(
    filePath: String,
    isUploading: Boolean,
    onDiscard: () -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LocalAudioPlayer(
            filePath = filePath,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDiscard, enabled = !isUploading) {
            Icon(Icons.Default.Delete, contentDescription = "Descartar audio")
        }
        if (isUploading) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp).padding(8.dp))
        } else {
            IconButton(onClick = onSend) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar audio")
            }
        }
    }
}

// ── Audio player composables ─────────────────────────────────────────────────

@Composable
private fun LocalAudioPlayer(filePath: String, modifier: Modifier = Modifier) {
    var isPrepared by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentMs by remember { mutableStateOf(0) }
    var durationMs by remember { mutableStateOf(0) }
    val playerRef = remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(filePath) {
        val mp = MediaPlayer()
        playerRef.value = mp
        runCatching {
            mp.setDataSource(filePath)
            mp.prepare()
            isPrepared = true
            durationMs = mp.duration
            mp.setOnCompletionListener { p -> p.seekTo(0); isPlaying = false; currentMs = 0 }
        }
        onDispose { mp.release(); playerRef.value = null }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentMs = playerRef.value?.currentPosition ?: 0
            delay(100)
        }
    }

    AudioPlayerRow(
        modifier = modifier,
        isPrepared = isPrepared,
        isPlaying = isPlaying,
        currentMs = currentMs,
        durationMs = durationMs,
        onToggle = {
            val mp = playerRef.value ?: return@AudioPlayerRow
            if (!isPrepared) return@AudioPlayerRow
            if (mp.isPlaying) { mp.pause(); isPlaying = false }
            else { mp.start(); isPlaying = true }
        },
    )
}

@Composable
private fun RemoteAudioPlayer(url: String, modifier: Modifier = Modifier) {
    var isPrepared by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentMs by remember { mutableStateOf(0) }
    var durationMs by remember { mutableStateOf(0) }
    val playerRef = remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(url) {
        val mp = MediaPlayer()
        playerRef.value = mp
        runCatching {
            mp.setDataSource(url)
            mp.setOnPreparedListener { p -> isPrepared = true; durationMs = p.duration }
            mp.setOnCompletionListener { p -> p.seekTo(0); isPlaying = false; currentMs = 0 }
            mp.prepareAsync()
        }
        onDispose { mp.release(); playerRef.value = null }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentMs = playerRef.value?.currentPosition ?: 0
            delay(100)
        }
    }

    AudioPlayerRow(
        modifier = modifier,
        isPrepared = isPrepared,
        isPlaying = isPlaying,
        currentMs = currentMs,
        durationMs = durationMs,
        onToggle = {
            val mp = playerRef.value ?: return@AudioPlayerRow
            if (!isPrepared) return@AudioPlayerRow
            if (mp.isPlaying) { mp.pause(); isPlaying = false }
            else { mp.start(); isPlaying = true }
        },
    )
}

@Composable
private fun AudioPlayerRow(
    isPrepared: Boolean,
    isPlaying: Boolean,
    currentMs: Int,
    durationMs: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.widthIn(min = 160.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggle, enabled = isPrepared) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
            )
        }
        Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
            LinearProgressIndicator(
                progress = { if (durationMs > 0) currentMs.toFloat() / durationMs else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatAudioDuration(if (currentMs > 0) currentMs else durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

// ── MessageBubble ─────────────────────────────────────────────────────────────

// ── CallMessageBubble ─────────────────────────────────────────────────────────

@Composable
private fun CallMessageBubble(message: MessageBO) {
    val timeText = remember(message.createdAt) {
        val local = message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        "%02d:%02d".format(local.hour, local.minute)
    }
    val isVideo = message.callType == "video"
    val status = message.callStatus ?: "ended"
    val callGreen = Color(0xFF2E7D32)

    val iconTint = when {
        status == "missed" && !message.isFromMe -> MaterialTheme.colorScheme.error
        status == "ended" -> callGreen
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
    val statusText = when {
        status == "ended" -> "Finalizada · ${formatCallDuration(message.callDuration ?: 0)}"
        status == "missed" && !message.isFromMe -> "Perdida"
        status == "rejected" && !message.isFromMe -> "Rechazada"
        else -> "Sin respuesta"
    }
    val statusColor = when {
        status == "missed" && !message.isFromMe -> MaterialTheme.colorScheme.error
        status == "ended" -> callGreen
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (message.isFromMe) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Phone,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.widthIn(min = 100.dp)) {
                    Text(
                        text = if (isVideo) "Videollamada" else "Llamada de voz",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                    )
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Bottom),
                )
            }
        }
    }
}

// ── StickerBubble ──────────────────────────────────────────────────────────────

@Composable
private fun StickerBubble(message: MessageBO, onReply: () -> Unit) {
    val timeText = remember(message.createdAt) {
        val local = message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        "%02d:%02d".format(local.hour, local.minute)
    }
    val swipeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThreshold = remember(density) { with(density) { 72.dp.toPx() } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeOffset.value >= swipeThreshold) onReply()
                        scope.launch { swipeOffset.animateTo(0f, spring(stiffness = 400f)) }
                    },
                    onDragCancel = {
                        scope.launch { swipeOffset.animateTo(0f, spring(stiffness = 400f)) }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            swipeOffset.snapTo((swipeOffset.value + dragAmount).coerceIn(0f, swipeThreshold * 1.3f))
                        }
                    },
                )
            },
    ) {
        Icon(
            imageVector = Icons.Default.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .alpha((swipeOffset.value / swipeThreshold).coerceIn(0f, 1f)),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = swipeOffset.value }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start,
        ) {
            val sticker = StickerValidation.sanitize(message.stickerUrl)
            if (sticker != null) {
                Text(text = sticker, fontSize = 64.sp)
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

// ── MessageBubble ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: MessageBO,
    isGroup: Boolean = false,
    onImageClick: (String) -> Unit,
    onReply: () -> Unit,
    isHighlighted: Boolean = false,
    onReplyClick: (String) -> Unit = {},
) {
    if (message.isCallMessage) {
        CallMessageBubble(message)
        return
    }
    if (message.stickerUrl != null) {
        StickerBubble(message, onReply)
        return
    }
    val timeText = remember(message.createdAt) {
        val local = message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        "%02d:%02d".format(local.hour, local.minute)
    }
    val highlightAlpha by animateFloatAsState(
        targetValue = if (isHighlighted) 0.25f else 0f,
        animationSpec = tween(300),
        label = "highlight",
    )
    val swipeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    val swipeThreshold = remember(density) { with(density) { 72.dp.toPx() } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeOffset.value >= swipeThreshold) onReply()
                        scope.launch { swipeOffset.animateTo(0f, spring(stiffness = 400f)) }
                    },
                    onDragCancel = {
                        scope.launch { swipeOffset.animateTo(0f, spring(stiffness = 400f)) }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            val next = swipeOffset.value + dragAmount
                            swipeOffset.snapTo(next.coerceIn(0f, swipeThreshold * 1.3f))
                        }
                    },
                )
            },
    ) {
        val iconAlpha = (swipeOffset.value / swipeThreshold).coerceIn(0f, 1f)
        Icon(
            imageVector = Icons.Default.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .alpha(iconAlpha),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = swipeOffset.value },
            horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (message.isFromMe) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (isGroup && !message.isFromMe && message.senderName.isNotBlank()) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (message.replyToId != null) {
                        ReplyQuote(
                            senderName = message.replyToSenderName ?: "",
                            content = message.replyToContent ?: "",
                            isFromMe = message.isFromMe,
                            onClick = { onReplyClick(message.replyToId) },
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (message.imageUrl != null) {
                        AsyncImage(
                            model = message.imageUrl.takeIf { MediaUrlValidator.isValid(it) },
                            contentDescription = "Imagen",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onImageClick(message.imageUrl) },
                        )
                    }
                    if (message.gifUrl != null) {
                        AsyncImage(
                            model = message.gifUrl.takeIf { MediaUrlValidator.isValid(it) },
                            contentDescription = "GIF",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    if (message.audioUrl != null && MediaUrlValidator.isValid(message.audioUrl)) {
                        RemoteAudioPlayer(url = message.audioUrl)
                    }
                    if (message.content.isNotBlank()) {
                        Box(
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    ClipboardProtection.copyWithTimeout(
                                        context = context,
                                        label = "message",
                                        text = message.content,
                                        scope = scope,
                                    )
                                },
                            ),
                        ) {
                            Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                        if (message.isFromMe) {
                            ReadReceiptIcon(isRead = message.isRead)
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = highlightAlpha)),
        )
    }
}

@Composable
private fun ReadReceiptIcon(isRead: Boolean) {
    Icon(
        imageVector = if (isRead) Icons.Default.DoneAll else Icons.Default.Done,
        contentDescription = if (isRead) "Leído" else "Enviado",
        modifier = Modifier.size(14.dp),
        tint = if (isRead)
            androidx.compose.ui.graphics.Color(0xFF4FC3F7)
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
    )
}

private fun formatLastSeen(diffMs: Long): String = when {
    diffMs < 60_000L -> "última vez hace un momento"
    diffMs < 3_600_000L -> "última vez hace ${diffMs / 60_000} min"
    diffMs < 86_400_000L -> "última vez hace ${diffMs / 3_600_000} h"
    else -> "última vez hace ${diffMs / 86_400_000} d"
}

// ── ImageGroupBubble ──────────────────────────────────────────────────────────

@Composable
private fun ImageGroupBubble(
    messages: List<MessageBO>,
    onImageClick: (index: Int) -> Unit,
    onReply: () -> Unit,
) {
    val isFromMe = messages.first().isFromMe
    val urls = messages.mapNotNull { it.imageUrl }
    val timeText = remember(messages.last().createdAt) {
        val local = messages.last().createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        "%02d:%02d".format(local.hour, local.minute)
    }
    val swipeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThreshold = remember(density) { with(density) { 72.dp.toPx() } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeOffset.value >= swipeThreshold) onReply()
                        scope.launch { swipeOffset.animateTo(0f, spring(stiffness = 400f)) }
                    },
                    onDragCancel = {
                        scope.launch { swipeOffset.animateTo(0f, spring(stiffness = 400f)) }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            swipeOffset.snapTo((swipeOffset.value + dragAmount).coerceIn(0f, swipeThreshold * 1.3f))
                        }
                    },
                )
            },
    ) {
        Icon(
            imageVector = Icons.Default.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .alpha((swipeOffset.value / swipeThreshold).coerceIn(0f, 1f)),
        )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = swipeOffset.value },
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start) {
            Row(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(12.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AsyncImage(
                    model = urls[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp)
                        .clickable { onImageClick(0) },
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp)
                        .clickable { onImageClick(1) },
                ) {
                    AsyncImage(
                        model = urls[1],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(
                                text = "${urls.size} fotos",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
    } // Box
}

// ── ReplyQuote ────────────────────────────────────────────────────────────────

@Composable
private fun ReplyQuote(
    senderName: String,
    content: String,
    isFromMe: Boolean,
    onClick: () -> Unit = {},
) {
    val accent = MaterialTheme.colorScheme.primary
    val bg = if (isFromMe)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Row(
        modifier = Modifier
            .widthIn(min = 120.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .height(IntrinsicSize.Min)
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxSize()
                .background(accent),
        )
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = senderName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── ReplyPreviewBar ───────────────────────────────────────────────────────────

@Composable
private fun ReplyPreviewBar(message: MessageBO, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
            Text(
                text = message.replySnippet(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Cancelar respuesta",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── ImageViewerDialog ─────────────────────────────────────────────────────────

@Composable
private fun ImageViewerDialog(
    imageUrls: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
            var fullscreenUrl by remember { mutableStateOf<String?>(null) }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 56.dp),
            ) {
                itemsIndexed(imageUrls) { _, url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fullscreenUrl = url },
                    )
                }
            }

            fullscreenUrl?.let { url ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable { fullscreenUrl = null },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                    IconButton(
                        onClick = { fullscreenUrl = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 40.dp, end = 8.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50)),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }
            }

            if (fullscreenUrl == null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 40.dp, end = 8.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50)),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun createCameraUri(context: Context): Uri {
    val file = File.createTempFile("img_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun formatAudioDuration(ms: Int): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

private fun formatCallDuration(seconds: Int): String =
    "%d:%02d".format(seconds / 60, seconds % 60)
