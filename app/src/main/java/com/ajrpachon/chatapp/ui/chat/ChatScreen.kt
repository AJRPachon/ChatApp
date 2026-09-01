package com.ajrpachon.chatapp.ui.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.service.ActiveChatTracker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    LaunchedEffect(state.search.highlightedMessageId) {
        val id = state.search.highlightedMessageId ?: return@LaunchedEffect
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
            ChatTopBar(
                state = state,
                vm = vm,
                latestPinned = latestPinned,
                pinnedBannerVisible = pinnedBannerVisible,
                showDeleteSelectionConfirm = showDeleteSelectionConfirm,
                onBack = onBack,
                onGroupInfo = onGroupInfo,
                onUserInfo = onUserInfo,
                onOpenMediaGallery = onOpenMediaGallery,
                onScrollToMessage = onScrollToMessage,
            )
        },
        bottomBar = {
            ChatBottomBar(
                state = state,
                vm = vm,
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
            )
        },
    ) { innerPadding ->
        ChatMessageList(
            state = state,
            vm = vm,
            lazyPagingItems = lazyPagingItems,
            listState = listState,
            scope = scope,
            reactions = reactions,
            chatThemeColors = chatThemeColors,
            highlightedMessageId = highlightedMessageId,
            showScrollToBottom = showScrollToBottom,
            innerPadding = innerPadding,
            onScrollToMessage = onScrollToMessage,
            onOpenPdf = onOpenPdf,
            viewerUrls = viewerUrls,
            viewerInitialIndex = viewerInitialIndex,
            showViewer = showViewer,
            reactionDetailMessageId = reactionDetailMessageId,
        )
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
