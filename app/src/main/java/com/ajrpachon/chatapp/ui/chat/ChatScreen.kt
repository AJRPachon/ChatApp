package com.ajrpachon.chatapp.ui.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import androidx.compose.runtime.collectAsState
import com.ajrpachon.chatapp.R
import com.ajrpachon.chatapp.domain.repository.PollRepository
import com.ajrpachon.chatapp.service.ActiveChatTracker
import com.ajrpachon.chatapp.ui.components.ChatMessagesSkeleton
import com.ajrpachon.chatapp.ui.components.OfflineBanner
import com.ajrpachon.chatapp.ui.components.EmojiPickerBottomSheet
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.ajrpachon.chatapp.domain.model.ChatTheme
import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.MediaUrlValidator
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.StickerValidation
import com.ajrpachon.chatapp.utils.LinkPreviewData
import com.ajrpachon.chatapp.utils.LinkPreviewFetcher
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
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
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
    var reactionDetailMessageId by remember { mutableStateOf<String?>(null) }
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

    var viewerUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var viewerInitialIndex by rememberSaveable { mutableStateOf(0) }
    var showViewer by rememberSaveable { mutableStateOf(false) }

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

    var showDeleteSelectionConfirm by remember { mutableStateOf(false) }

    if (showDeleteSelectionConfirm) {
        val count = state.selectedMessageIds.size
        AlertDialog(
            onDismissRequest = { showDeleteSelectionConfirm = false },
            title = { Text(stringResource(R.string.chat_delete_messages_title)) },
            text = { Text(pluralStringResource(R.plurals.chat_delete_messages_confirm, count, count)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteSelectionConfirm = false
                    vm.onIntent(ChatIntent.DeleteSelectedMessages)
                }) { Text(stringResource(R.string.chat_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectionConfirm = false }) { Text(stringResource(R.string.chat_cancel)) }
            },
        )
    }

    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var videoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> if (uris.isNotEmpty()) vm.onIntent(ChatIntent.SendImages(context, uris)) }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.onIntent(ChatIntent.SendFile(context, uri)) }

    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success -> if (success) videoUri?.let { vm.onIntent(ChatIntent.SendVideo(context, it)) } }

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

    if (showViewer && viewerUrls.isNotEmpty()) {
        ImageViewerDialog(
            imageUrls = viewerUrls,
            initialIndex = viewerInitialIndex,
            onDismiss = { showViewer = false },
        )
    }

    state.expiryDialogMessageId?.let { msgId ->
        ExpiryDurationDialog(
            onDismiss = { vm.onIntent(ChatIntent.DismissExpiryDialog) },
            onSelect = { vm.onIntent(ChatIntent.SetExpiry(msgId, it)) },
        )
    }

    if (state.showForwardDialog) {
        ForwardConversationDialog(
            conversations = state.forwardableConversations,
            onDismiss = { vm.onIntent(ChatIntent.DismissForwardDialog) },
            onSelect = { targetConversationId ->
                val forwardingMsg = state.forwardingMessage
                if (forwardingMsg != null) {
                    vm.onIntent(ChatIntent.ForwardMessage(forwardingMsg.id, targetConversationId))
                } else {
                    vm.onIntent(ChatIntent.ForwardSelectedMessages(targetConversationId))
                }
            },
        )
    }

    if (state.showIncognitoInfoDialog) {
        AlertDialog(
            onDismissRequest = { vm.onIntent(ChatIntent.DismissIncognitoDialog) },
            title = { Text(stringResource(R.string.chat_incognito_mode)) },
            text = {
                Text(stringResource(R.string.chat_incognito_mode_description))
            },
            confirmButton = {
                TextButton(onClick = { vm.onIntent(ChatIntent.ConfirmIncognito) }) {
                    Text(stringResource(R.string.chat_incognito_confirm_activate))
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.onIntent(ChatIntent.DismissIncognitoDialog) }) {
                    Text(stringResource(R.string.chat_cancel))
                }
            },
        )
    }

    if (state.showForwardSelectionDialog) {
        ForwardConversationDialog(
            conversations = state.forwardableConversations,
            onDismiss = { vm.onIntent(ChatIntent.DismissForwardSelectionDialog) },
            onSelect = { targetConversationId ->
                vm.onIntent(ChatIntent.ForwardSelectedMessages(targetConversationId))
            },
        )
    }

    if (state.showForwardSelectionDialog) {
        ForwardConversationDialog(
            conversations = state.forwardableConversations,
            onDismiss = { vm.onIntent(ChatIntent.DismissForwardSelectionDialog) },
            onSelect = { targetConversationId ->
                vm.onIntent(ChatIntent.ForwardSelectedMessages(targetConversationId))
            },
        )
    }

    if (state.showMuteDialog) {
        MuteDurationDialog(
            onDismiss = { vm.onIntent(ChatIntent.DismissMuteDialog) },
            onSelect = { vm.onIntent(ChatIntent.MuteFor(it)) },
        )
    }

    var showStickerStore by remember { mutableStateOf(false) }

    if (state.showStickerPicker) {
        StickerGifPicker(
            onStickerSelected = { vm.onIntent(ChatIntent.SendSticker(it)) },
            onGifSelected = { vm.onIntent(ChatIntent.SendGif(it)) },
            onOpenStore = {
                vm.onIntent(ChatIntent.CloseStickerPicker)
                showStickerStore = true
            },
            onDismiss = { vm.onIntent(ChatIntent.CloseStickerPicker) },
        )
    }

    if (showStickerStore) {
        StickerStoreSheet(onDismiss = { showStickerStore = false })
    }

    val chatTheme = state.chatTheme
    val chatThemeColors = chatTheme.toColors()

    if (state.showThemePicker) {
        ChatThemePickerSheet(
            currentTheme = chatTheme,
            onSelect = { vm.onIntent(ChatIntent.SetChatTheme(it)) },
            onDismiss = { vm.onIntent(ChatIntent.DismissThemePicker) },
        )
    }

    if (state.showDisappearingModeSheet) {
        DisappearingModeSheet(
            currentSeconds = state.disappearingModeSeconds,
            onDismiss = { vm.onIntent(ChatIntent.DismissDisappearingModeSheet) },
            onSelect = { seconds -> vm.onIntent(ChatIntent.SetDisappearingMode(conversationId, seconds)) },
        )
    }

    if (state.showScheduleDialog) {
        ScheduleMessageDialog(
            onDismiss = { vm.onIntent(ChatIntent.DismissScheduleDialog) },
            onConfirm = { scheduledAtMs -> vm.onIntent(ChatIntent.ScheduleMessage(scheduledAtMs)) },
        )
    }

    if (state.showScheduledSheet) {
        val scheduledSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { vm.onIntent(ChatIntent.DismissScheduledSheet) },
            sheetState = scheduledSheetState,
        ) {
            Text(
                text = stringResource(R.string.chat_scheduled_messages),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (state.scheduledMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.chat_no_scheduled_messages),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(state.scheduledMessages, key = { it.id }) { msg ->
                        val formatter = remember { java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.getDefault()) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = formatter.format(java.util.Date(msg.scheduledAtMs)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { vm.onIntent(ChatIntent.CancelScheduledMessage(msg.id)) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.chat_cancel_scheduled_message))
                            }
                        }
                    }
                }
            }
            Spacer(
                Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            )
        }
    }

    if (state.showAiSheet) {
        AiAssistantSheet(
            aiSuggestion = state.aiSuggestion,
            isAiLoading = state.isAiLoading,
            onDismiss = { vm.onIntent(ChatIntent.DismissAiSheet) },
            onSuggestReply = { vm.onIntent(ChatIntent.AiSuggestReply) },
            onFreeform = { prompt -> vm.onIntent(ChatIntent.AiFreeform(prompt)) },
            onInsert = { vm.onIntent(ChatIntent.InsertAiSuggestion) },
        )
    }

    if (state.showCreatePollSheet) {
        ModalBottomSheet(
            onDismissRequest = { vm.onIntent(ChatIntent.DismissCreatePollSheet) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            CreatePollSheetContent(
                onDismiss = { vm.onIntent(ChatIntent.DismissCreatePollSheet) },
                onCreate = { question, options, allowMultiple ->
                    vm.onIntent(ChatIntent.CreatePoll(question, options, allowMultiple))
                },
            )
        }
    }

    reactionDetailMessageId?.let { msgId ->
        val msgReactions = reactions[msgId] ?: emptyList()
        if (msgReactions.isNotEmpty()) {
            ModalBottomSheet(
                onDismissRequest = { reactionDetailMessageId = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                ReactionDetailsSheet(
                    reactions = msgReactions,
                    onDismiss = { reactionDetailMessageId = null },
                )
            }
        }
    }

    if (state.showWallpaperPicker) {
        WallpaperPickerSheet(
            currentColor = state.wallpaperColor,
            onSelect = { colorValue: Long? ->
                vm.onIntent(ChatIntent.SetWallpaperColor(colorValue))
                vm.onIntent(ChatIntent.DismissWallpaperPicker)
            },
            onDismiss = { vm.onIntent(ChatIntent.DismissWallpaperPicker) },
        )
    }

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
                        .background(androidx.compose.ui.graphics.Color(0xFF4A148C))
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
                        IconButton(onClick = { showDeleteSelectionConfirm = true }) {
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
                                        androidx.compose.ui.graphics.Color(0xFF4CAF50)
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
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.chat_more_options))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            if (state.isGroup || state.otherUserId != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (state.isGroup) stringResource(R.string.chat_group_voice_call)
                                            else stringResource(R.string.chat_voice_call)
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
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
                                        menuExpanded = false
                                        vm.onIntent(ChatIntent.StartCall("video"))
                                    },
                                )
                            }
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
                                        tint = if (state.isIncognito) androidx.compose.ui.graphics.Color(0xFF7B1FA2) else MaterialTheme.colorScheme.onSurface,
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
                                onShowReactionDetails = { reactionDetailMessageId = message.id },
                                onRetryMessage = { vm.onIntent(ChatIntent.RetryMessage(it)) },
                                onCopy = { vm.onIntent(ChatIntent.CopyMessageContent(it)) },
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

// MessageSearchOverlay, SearchResultItem → see ChatSearchOverlay.kt

// ── Bottom bar composables ────────────────────────────────────────────────────

// NormalInputBar, AttachmentBottomSheet, ReplyPreviewBar → see ChatInputBar.kt

// Audio components extracted to ChatAudioComponents.kt

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
        status == "ended" -> stringResource(R.string.chat_call_ended_duration, formatCallDuration(message.callDuration ?: 0))
        status == "missed" && !message.isFromMe -> stringResource(R.string.chat_call_missed)
        status == "rejected" && !message.isFromMe -> stringResource(R.string.chat_call_rejected)
        else -> stringResource(R.string.chat_call_no_answer)
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
                        text = if (isVideo) stringResource(R.string.chat_video_call) else stringResource(R.string.chat_voice_call),
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

// ── FileBubble ────────────────────────────────────────────────────────────────

@Composable
private fun FileBubble(
    message: MessageBO,
    onReply: () -> Unit,
    onOpenPdf: (url: String, filename: String) -> Unit = { _, _ -> },
) {
    val isPdf = message.fileUrl?.endsWith(".pdf", ignoreCase = true) == true
    if (isPdf) {
        PdfFileCard(message = message, onReply = onReply, onOpenPdf = onOpenPdf)
    } else {
        GenericFileBubble(message = message, onReply = onReply)
    }
}

@Composable
private fun PdfFileCard(
    message: MessageBO,
    onReply: () -> Unit,
    onOpenPdf: (url: String, filename: String) -> Unit,
) {
    val timeText = remember(message.createdAt) {
        val local = message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        "%02d:%02d".format(local.hour, local.minute)
    }
    val bubbleColor = if (message.isFromMe)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant
    val alignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val documentPdfDefault = stringResource(R.string.chat_document_pdf_default)

    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = bubbleColor,
            modifier = Modifier.align(alignment).widthIn(max = 280.dp)
                .combinedClickable(onClick = {}, onLongClick = onReply),
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = message.fileName ?: documentPdfDefault,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        if (message.fileSize != null) {
                            Text(
                                text = formatFileSize(message.fileSize),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Bottom),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val url = message.fileUrl ?: return@clickable
                            val filename = message.fileName ?: documentPdfDefault
                            onOpenPdf(url, filename)
                        },
                ) {
                    Text(
                        text = stringResource(R.string.chat_view_pdf),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GenericFileBubble(message: MessageBO, onReply: () -> Unit) {
    val timeText = remember(message.createdAt) {
        val local = message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        "%02d:%02d".format(local.hour, local.minute)
    }
    val bubbleColor = if (message.isFromMe)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant
    val alignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val context = LocalContext.current

    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = bubbleColor,
            modifier = Modifier.align(alignment).widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = {
                        message.fileUrl?.let { url ->
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse(url)
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    },
                    onLongClick = onReply,
                ),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.fileName ?: stringResource(R.string.chat_file_default),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    if (message.fileSize != null) {
                        Text(
                            text = formatFileSize(message.fileSize),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Bottom),
                )
            }
        }
    }
}

internal fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}

// ── VideoBubble ───────────────────────────────────────────────────────────────

@Composable
private fun VideoBubble(message: MessageBO, onReply: () -> Unit) {
    val timeText = remember(message.createdAt) {
        val local = message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        "%02d:%02d".format(local.hour, local.minute)
    }
    val alignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val context = LocalContext.current

    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
        Column(
            modifier = Modifier.align(alignment),
            horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (message.isFromMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 240.dp).combinedClickable(
                    onClick = {
                        val uri = android.net.Uri.parse(message.videoUrl)
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "video/*")
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { context.startActivity(intent) }
                    },
                    onLongClick = onReply,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .height(160.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = message.videoUrl,
                        contentDescription = stringResource(R.string.chat_video_content_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.chat_play),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
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
            imageVector = Icons.AutoMirrored.Filled.Reply,
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

// ── DeletedMessageBubble ──────────────────────────────────────────────────────

@Composable
private fun DeletedMessageBubble(message: MessageBO) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
                Text(
                    stringResource(R.string.chat_message_deleted),
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

// ── MessageBubble ─────────────────────────────────────────────────────────────

@Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList", "ReturnCount")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MessageBubble(
    message: MessageBO,
    isGroup: Boolean = false,
    onImageClick: (String) -> Unit,
    onReply: () -> Unit,
    isHighlighted: Boolean = false,
    onReplyClick: (String) -> Unit = {},
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onSelfDestruct: (() -> Unit)? = null,
    onForward: (() -> Unit)? = null,
    messageReactions: List<com.ajrpachon.chatapp.domain.model.ReactionBO> = emptyList(),
    currentUserId: String? = null,
    onToggleReaction: (String) -> Unit = {},
    isSelected: Boolean = false,
    isMultiSelectActive: Boolean = false,
    onToggleSelect: () -> Unit = {},
    outgoingBubbleColor: Color = Color.Unspecified,
    onOpenPdf: (url: String, filename: String) -> Unit = { _, _ -> },
    onVote: ((optionId: String) -> Unit)? = null,
    onShowReactionDetails: () -> Unit = {},
    onRetryMessage: (String) -> Unit = {},
    onCopy: (String) -> Unit = {},
) {
    if (message.isDeleted) {
        DeletedMessageBubble(message)
        return
    }
    if (message.isCallMessage) {
        CallMessageBubble(message)
        return
    }
    if (message.stickerUrl != null) {
        StickerBubble(message, onReply)
        return
    }
    if (message.content.startsWith("contact:{")) {
        val json = message.content.removePrefix("contact:")
        val contactName = runCatching { org.json.JSONObject(json).getString("name") }.getOrNull()
        val contactPhone = runCatching { org.json.JSONObject(json).optString("phone", "") }.getOrNull()
        if (contactName != null) {
            ContactBubble(
                name = contactName,
                phone = contactPhone ?: "",
                isFromMe = message.isFromMe,
            )
        }
        return
    }
    if (message.fileUrl != null) {
        FileBubble(message, onReply, onOpenPdf)
        return
    }
    if (message.videoUrl != null) {
        VideoBubble(message, onReply)
        return
    }
    if (message.content.startsWith("poll:")) {
        val pollId = remember(message.content) { message.content.removePrefix("poll:") }
        val pollRepository: PollRepository = koinInject()
        PollBubble(
            pollId = pollId,
            isFromMe = message.isFromMe,
            pollRepository = pollRepository,
            currentUserId = currentUserId,
            onVote = { optionId -> onVote?.invoke(optionId) },
        )
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
            .combinedClickable(
                onClick = { if (isMultiSelectActive) onToggleSelect() },
                onLongClick = { onToggleSelect() },
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (!isMultiSelectActive && swipeOffset.value >= swipeThreshold) onReply()
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
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .alpha(iconAlpha),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = swipeOffset.value },
            horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start,
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start,
        ) {
            Box {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (message.isFromMe) {
                    if (outgoingBubbleColor != Color.Unspecified) outgoingBubbleColor
                    else MaterialTheme.colorScheme.primaryContainer
                } else MaterialTheme.colorScheme.surfaceVariant,
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
                            contentDescription = stringResource(R.string.chat_image_content_description),
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
                            contentDescription = stringResource(R.string.chat_gif_content_description),
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
                        var showMsgMenu by remember { mutableStateOf(false) }
                        var showEmojiPicker by remember { mutableStateOf(false) }
                        val emojiSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        val locationUrl = remember(message.content) {
                            if (message.content.startsWith("📍 Mi ubicación: https://maps.google.com/?q=")) {
                                message.content.substringAfter("📍 Mi ubicación: ")
                            } else null
                        }
                        if (locationUrl != null) {
                            LocationMessageCard(mapsUrl = locationUrl)
                        }
                        Box(
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = { showMsgMenu = true },
                            ),
                        ) {
                            Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
                            DropdownMenu(expanded = showMsgMenu, onDismissRequest = { showMsgMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.chat_react)) },
                                    leadingIcon = { Text("😊") },
                                    onClick = { showMsgMenu = false; showEmojiPicker = true },
                                )
                                if (onEdit != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.chat_edit)) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = { showMsgMenu = false; onEdit() },
                                    )
                                }
                                if (onSelfDestruct != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (message.expiresAt != null) stringResource(R.string.chat_remove_self_destruct) else stringResource(R.string.chat_ephemeral_message))
                                        },
                                        leadingIcon = { Text(if (message.expiresAt != null) "♾️" else "⏱️") },
                                        onClick = { showMsgMenu = false; onSelfDestruct() },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.chat_copy)) },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                    onClick = { showMsgMenu = false; onCopy(message.content) },
                                )
                                if (onDelete != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.chat_delete)) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                        onClick = { showMsgMenu = false; onDelete() },
                                    )
                                }
                                if (onForward != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.chat_forward)) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null) },
                                        onClick = { showMsgMenu = false; onForward() },
                                    )
                                }
                            }
                        }
                        if (showEmojiPicker) {
                            EmojiPickerBottomSheet(
                                sheetState = emojiSheetState,
                                onDismiss = { showEmojiPicker = false },
                                onEmojiSelected = { emoji -> onToggleReaction(emoji) },
                            )
                        }
                    }
                    // Link preview — shown only for plain text messages (no image/audio/gif)
                    val hasAttachment = message.imageUrl != null || message.audioUrl != null ||
                        message.gifUrl != null
                    if (!hasAttachment && message.content.isNotBlank()) {
                        val detectedUrl = remember(message.content) {
                            val matcher = Patterns.WEB_URL.matcher(message.content)
                            if (matcher.find()) matcher.group() else null
                        }
                        if (detectedUrl != null) {
                            val fetcher: LinkPreviewFetcher = koinInject()
                            var previewData by remember(detectedUrl) { mutableStateOf<LinkPreviewData?>(null) }
                            LaunchedEffect(detectedUrl) {
                                previewData = fetcher.fetchLinkPreview(detectedUrl)
                            }
                            if (previewData != null) {
                                Spacer(Modifier.height(6.dp))
                                LinkPreviewCard(data = previewData ?: return@Column)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        message.expiresAt?.let { exp ->
                            val secsLeft = ((exp - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                            Text(
                                "⏱️ ${if (secsLeft < 60) "${secsLeft}s" else "${secsLeft / 60}m"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            )
                        }
                        if (message.isEdited) {
                            Text(
                                stringResource(R.string.chat_edited_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                        }
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                        if (message.isFromMe) {
                            when (message.sendStatus) {
                                com.ajrpachon.chatapp.domain.model.SendStatus.PENDING ->
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Schedule,
                                        contentDescription = stringResource(R.string.chat_pending_send),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(12.dp),
                                    )
                                com.ajrpachon.chatapp.domain.model.SendStatus.FAILED ->
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = stringResource(R.string.chat_send_error),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(12.dp),
                                    )
                                com.ajrpachon.chatapp.domain.model.SendStatus.SENT ->
                                    ReadReceiptIcon(isRead = message.isRead)
                            }
                        }
                    }
                }
            }
            } // Box
        }
        if (messageReactions.isNotEmpty()) {
            val grouped = messageReactions.groupBy { it.emoji }
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                grouped.forEach { (emoji, reactors) ->
                    val isMine = reactors.any { it.userId == currentUserId }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isMine) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.combinedClickable(
                            onClick = { onToggleReaction(emoji) },
                            onLongClick = { onShowReactionDetails() },
                        ),
                    ) {
                        Text(
                            text = "$emoji ${reactors.size}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
        } // close Column
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = highlightAlpha)),
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            )
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.chat_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(if (message.isFromMe) Alignment.TopEnd else Alignment.TopStart)
                    .padding(4.dp)
                    .size(20.dp),
            )
        }
    }
}

private fun openLocationInMaps(context: android.content.Context, mapsUrl: String, latLng: Pair<Double, Double>?) {
    val gmmIntent = latLng?.let { (lat, lng) ->
        android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng"),
        ).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    val opened = gmmIntent != null && runCatching { context.startActivity(gmmIntent) }.isSuccess
    if (!opened) {
        val fallback = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(mapsUrl)).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(fallback) }
    }
}

@Composable
private fun LocationMessageCard(mapsUrl: String) {
    val context = LocalContext.current
    val latLng = remember(mapsUrl) {
        runCatching {
            val query = android.net.Uri.parse(mapsUrl).getQueryParameter("q") ?: return@runCatching null
            val (lat, lng) = query.split(",").map { it.trim().toDouble() }
            lat to lng
        }.getOrNull()
    }
    val staticMapUrl = remember(latLng) {
        latLng?.let { (lat, lng) ->
            "https://staticmap.openstreetmap.de/staticmap.php?center=$lat,$lng&zoom=15&size=320x160&markers=$lat,$lng,red-pushpin"
        }
    }
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
            .widthIn(min = 160.dp, max = 240.dp)
            .clickable { openLocationInMaps(context, mapsUrl, latLng) },
    ) {
        Column {
            if (staticMapUrl != null) {
                AsyncImage(
                    model = staticMapUrl,
                    contentDescription = stringResource(R.string.chat_shared_location),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.chat_shared_location),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.chat_view_on_maps),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ReadReceiptIcon(isRead: Boolean) {
    Icon(
        imageVector = if (isRead) Icons.Default.DoneAll else Icons.Default.Done,
        contentDescription = if (isRead) stringResource(R.string.chat_read) else stringResource(R.string.chat_sent),
        modifier = Modifier.size(14.dp),
        tint = if (isRead)
            androidx.compose.ui.graphics.Color(0xFF4FC3F7)
        else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
    )
}


// ── ImageGroupBubble ──────────────────────────────────────────────────────────

// Placeholder shown while a multi-image batch (>2 photos) uploads, mirroring ImageGroupBubble's
// layout exactly (same 2-cell grid + overlay) so there is no shape/size change when the real,
// server-backed group replaces it once the batch finishes — that's what removes the visible jump.
@Composable
private fun PendingImageBatchBubble(
    uris: List<Uri>,
    progress: MediaUploadProgress?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(12.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AsyncImage(
                model = uris[0],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp),
            ) {
                AsyncImage(
                    model = uris[1],
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
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = "${progress?.completedCount ?: 0}/${progress?.totalCount ?: uris.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}

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
            imageVector = Icons.AutoMirrored.Filled.Reply,
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
                                text = stringResource(R.string.chat_photos_count, urls.size),
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

// ── LinkPreviewCard ───────────────────────────────────────────────────────────

@Composable
private fun LinkPreviewCard(data: LinkPreviewData) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(data.url)).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(intent) }
            },
    ) {
        Column {
            if (data.imageUrl != null) {
                AsyncImage(
                    model = data.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                )
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (data.description != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = data.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = runCatching { java.net.URL(data.url).host }.getOrDefault(data.url),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ReplyPreviewBar → see ChatInputBar.kt

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
                itemsIndexed(imageUrls, key = { _, url -> url }) { _, url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 800.dp)
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
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.chat_close), tint = Color.White)
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
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.chat_close), tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ContactBubble(name: String, phone: String, isFromMe: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.widthIn(max = 280.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFromMe) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
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
        HorizontalDivider()
        TextButton(
            onClick = {
                val dialIntent = android.content.Intent(
                    android.content.Intent.ACTION_DIAL,
                    android.net.Uri.parse("tel:$phone"),
                )
                context.startActivity(dialIntent)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.chat_call_action), style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun createCameraUri(context: Context): Uri {
    val file = File.createTempFile("img_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun createVideoUri(context: Context): Uri {
    val file = File.createTempFile("vid_", ".mp4", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

// formatAudioDuration extracted to ChatAudioComponents.kt

private fun formatCallDuration(seconds: Int): String =
    "%d:%02d".format(seconds / 60, seconds % 60)

@Composable
private fun ExpiryDurationDialog(onDismiss: () -> Unit, onSelect: (Long?) -> Unit) {
    val options = listOf(
        stringResource(R.string.chat_expiry_1_minute) to (System.currentTimeMillis() + 60_000L),
        stringResource(R.string.chat_1_hour) to (System.currentTimeMillis() + 3_600_000L),
        stringResource(R.string.chat_24_hours) to (System.currentTimeMillis() + 86_400_000L),
        stringResource(R.string.chat_7_days) to (System.currentTimeMillis() + 604_800_000L),
        stringResource(R.string.chat_remove_self_destruct) to null,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_ephemeral_message)) },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(
                    stringResource(R.string.chat_ephemeral_message_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(androidx.compose.ui.Modifier.height(8.dp))
                options.forEach { (label, value) ->
                    TextButton(
                        onClick = { onSelect(value) },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    ) {
                        Text(label, modifier = androidx.compose.ui.Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
        },
    )
}

@Composable
private fun MuteDurationDialog(onDismiss: () -> Unit, onSelect: (Long) -> Unit) {
    val options = listOf(
        stringResource(R.string.chat_1_hour) to (System.currentTimeMillis() + 3_600_000L),
        stringResource(R.string.chat_8_hours) to (System.currentTimeMillis() + 28_800_000L),
        stringResource(R.string.chat_24_hours) to (System.currentTimeMillis() + 86_400_000L),
        stringResource(R.string.chat_mute_always) to -1L,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_mute_notifications_title)) },
        text = {
            androidx.compose.foundation.layout.Column {
                options.forEach { (label, value) ->
                    TextButton(
                        onClick = { onSelect(value) },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    ) {
                        Text(label, modifier = androidx.compose.ui.Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
        },
    )
}

@Composable
private fun ForwardConversationDialog(
    conversations: List<ConversationBO>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_forward_to)) },
        text = {
            if (conversations.isEmpty()) {
                Text(
                    stringResource(R.string.chat_no_other_conversations),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(conversations, key = { _, c -> c.id }) { _, conv ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(conv.id) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val avatarUrl = conv.displayAvatarUrl
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
                                    if (conv.isGroup) {
                                        Icon(
                                            Icons.Default.Group,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    } else {
                                        Text(
                                            text = conv.name.firstOrNull()?.uppercase() ?: "?",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = conv.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
        },
    )
}

// ── ChatThemePickerSheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatThemePickerSheet(
    currentTheme: ChatTheme,
    onSelect: (ChatTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_chat_theme),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(ChatTheme.entries.size, key = { it }) { index ->
                    val theme = ChatTheme.entries[index]
                    val isSelected = theme == currentTheme
                    val label = when (theme) {
                        ChatTheme.DEFAULT -> stringResource(R.string.chat_theme_default)
                        ChatTheme.OCEAN -> stringResource(R.string.chat_theme_ocean)
                        ChatTheme.SUNSET -> stringResource(R.string.chat_theme_sunset)
                        ChatTheme.FOREST -> stringResource(R.string.chat_theme_forest)
                        ChatTheme.LAVENDER -> stringResource(R.string.chat_theme_lavender)
                        ChatTheme.ROSE -> stringResource(R.string.chat_theme_rose)
                        ChatTheme.MIDNIGHT -> stringResource(R.string.chat_theme_midnight)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            onSelect(theme)
                            onDismiss()
                        },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    color = if (theme == ChatTheme.DEFAULT)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        theme.toColors().bubbleColor,
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = stringResource(R.string.chat_selected),
                                    tint = if (theme == ChatTheme.MIDNIGHT)
                                        Color.White
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

// ── DisappearingModeSheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisappearingModeSheet(
    currentSeconds: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val options = listOf(
        stringResource(R.string.chat_disabled) to 0L,
        stringResource(R.string.chat_24_hours) to 86_400L,
        stringResource(R.string.chat_7_days) to 604_800L,
        stringResource(R.string.chat_30_days) to 2_592_000L,
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                stringResource(R.string.chat_disappearing_mode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Text(
                stringResource(R.string.chat_disappearing_mode_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            )
            options.forEach { (label, seconds) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(seconds) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, modifier = Modifier.weight(1f))
                    if (seconds == currentSeconds) {
                        Icon(
                            Icons.Default.Done,
                            contentDescription = stringResource(R.string.chat_selected),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleMessageDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 60_000,
    )
    val timePickerState = rememberTimePickerState(
        initialHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE) + 5,
        is24Hour = true,
    )
    var showTimePicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (!showTimePicker) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { showTimePicker = true }) {
                    Text(stringResource(R.string.chat_next))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.chat_select_time)) },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDateMs = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val cal = java.util.Calendar.getInstance().apply {
                        timeInMillis = selectedDateMs
                        set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(java.util.Calendar.MINUTE, timePickerState.minute)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    onConfirm(cal.timeInMillis)
                }) {
                    Text(stringResource(R.string.chat_schedule_action))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
            },
        )
    }
}

// ── AI Assistant bottom sheet ─────────────────────────────────────────────────

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AiAssistantSheet(
    aiSuggestion: String?,
    isAiLoading: Boolean,
    onDismiss: () -> Unit,
    onSuggestReply: () -> Unit,
    onFreeform: (String) -> Unit,
    onInsert: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var freeformText by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_ai_assistant),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            // Action chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.SuggestionChip(
                    onClick = onSuggestReply,
                    label = { Text(stringResource(R.string.chat_suggest_reply)) },
                    enabled = !isAiLoading,
                )
            }

            // Free-form input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = freeformText,
                    onValueChange = { freeformText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.chat_ask_question_placeholder)) },
                    singleLine = true,
                    enabled = !isAiLoading,
                )
                IconButton(
                    onClick = {
                        if (freeformText.isNotBlank()) {
                            onFreeform(freeformText)
                            freeformText = ""
                        }
                    },
                    enabled = freeformText.isNotBlank() && !isAiLoading,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.chat_send_query))
                }
            }

            // Loading indicator
            if (isAiLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            // Result area
            if (aiSuggestion != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(12.dp),
                ) {
                    Text(
                        text = aiSuggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                androidx.compose.material3.Button(
                    onClick = onInsert,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.chat_insert_in_message))
                }
            }
        }
    }
}

// ── CreatePollSheetContent ────────────────────────────────────────────────────

@Composable
private fun CreatePollSheetContent(
    onDismiss: () -> Unit,
    onCreate: (question: String, options: List<String>, allowMultiple: Boolean) -> Unit,
) {
    var question by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf("", "")) }
    var allowMultiple by remember { mutableStateOf(false) }

    val isValid = question.isNotBlank() && options.count { it.isNotBlank() } >= 2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.chat_create_poll),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text(stringResource(R.string.chat_question)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        options.forEachIndexed { index, option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = option,
                    onValueChange = { newValue ->
                        options = options.toMutableList().also { it[index] = newValue }
                    },
                    label = { Text(stringResource(R.string.chat_option_number, index + 1)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                if (options.size > 2) {
                    IconButton(
                        onClick = {
                            options = options.toMutableList().also { it.removeAt(index) }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.chat_remove_option),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        if (options.size < 10) {
            TextButton(onClick = { options = options + "" }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.chat_add_option))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.chat_poll_allow_multiple),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = allowMultiple, onCheckedChange = { allowMultiple = it })
        }

        androidx.compose.material3.Button(
            onClick = { onCreate(question, options.filter { it.isNotBlank() }, allowMultiple); onDismiss() },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.chat_create_poll)) }
    }
}

@Composable
private fun PinnedMessageBanner(
    message: MessageBO,
    pinnedCount: Int,
    onTap: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.PushPin,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (pinnedCount > 1) stringResource(R.string.chat_pinned_message_count, pinnedCount) else stringResource(R.string.chat_pinned_message),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = message.content.ifBlank { stringResource(R.string.chat_attachment_placeholder) },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.chat_unpin), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── PollBubble ────────────────────────────────────────────────────────────────

@Composable
private fun PollBubble(
    pollId: String,
    isFromMe: Boolean,
    pollRepository: PollRepository,
    currentUserId: String?,
    onVote: (optionId: String) -> Unit,
) {
    val poll by pollRepository.observePollById(pollId).collectAsState(initial = null)
    val options by pollRepository.observeOptionsByPollId(pollId).collectAsState(initial = emptyList())
    val userVotes by pollRepository.observeVotes(pollId, currentUserId ?: "").collectAsState(initial = emptyList())

    val alignment = if (isFromMe) Alignment.End else Alignment.Start

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(min = 220.dp, max = 300.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.HowToVote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.chat_poll),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (poll == null) {
                    Text(
                        text = stringResource(R.string.chat_loading_poll),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                } else {
                    // Question
                    val safePoll = poll ?: return@Column
                    Text(
                        text = safePoll.question,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    val totalVotes = options.sumOf { it.voteCount }.coerceAtLeast(1)

                    // Options
                    val allowMultiple = safePoll.allowMultiple
                    options.forEach { option ->
                        val isSelected = userVotes.any { it.optionId == option.id }
                        val fraction = option.voteCount.toFloat() / totalVotes.toFloat()
                        // Animate width instead of snapping so re-votes read as a smooth transition.
                        val animatedFraction by animateFloatAsState(
                            targetValue = fraction,
                            animationSpec = tween(durationMillis = 300),
                            label = "pollOptionFraction",
                        )
                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (allowMultiple) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { onVote(option.id) },
                                        modifier = Modifier.size(20.dp),
                                    )
                                } else {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onVote(option.id) },
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = option.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${option.voteCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .padding(start = 26.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedFraction)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── ReactionDetailsSheet ──────────────────────────────────────────────────────

@Composable
private fun ReactionDetailsSheet(
    reactions: List<com.ajrpachon.chatapp.domain.model.ReactionBO>,
    onDismiss: () -> Unit,
) {
    val grouped = reactions.groupBy { it.emoji }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
    ) {
        Text(
            text = stringResource(R.string.chat_reactions_count, reactions.size),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        grouped.forEach { (emoji, reactors) ->
            reactors.forEach { reaction ->
                ListItem(
                    headlineContent = { Text(reaction.userId) },
                    trailingContent = { Text(emoji, style = MaterialTheme.typography.titleLarge) },
                )
                HorizontalDivider()
            }
        }
    }
}

// ── WallpaperPickerSheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WallpaperPickerSheet(
    currentColor: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = listOf(
        null to stringResource(R.string.chat_wallpaper_default),
        0xFFE3F2FDL to stringResource(R.string.chat_wallpaper_light_blue),
        0xFFF3E5F5L to stringResource(R.string.chat_wallpaper_purple),
        0xFFE8F5E9L to stringResource(R.string.chat_wallpaper_green),
        0xFFFFF8E1L to stringResource(R.string.chat_wallpaper_yellow),
        0xFFFCE4ECL to stringResource(R.string.chat_wallpaper_pink),
        0xFF212121L to stringResource(R.string.chat_wallpaper_dark),
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            stringResource(R.string.chat_wallpaper),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(colors, key = { it.first?.toString() ?: "default" }) { (colorValue, label) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelect(colorValue) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                colorValue?.let { androidx.compose.ui.graphics.Color(it) }
                                    ?: MaterialTheme.colorScheme.background
                            )
                            .border(
                                width = if (currentColor == colorValue) 3.dp else 1.dp,
                                color = if (currentColor == colorValue) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
