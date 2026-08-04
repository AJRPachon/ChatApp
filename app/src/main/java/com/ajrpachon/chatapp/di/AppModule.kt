package com.ajrpachon.chatapp.di

import com.ajrpachon.chatapp.BuildConfig
import com.ajrpachon.chatapp.data.local.ThemeRepository
import com.ajrpachon.chatapp.data.local.buildChatDatabase
import com.ajrpachon.chatapp.data.session.AndroidSessionManager
import com.ajrpachon.chatapp.ui.applock.AppLockViewModel
import com.ajrpachon.chatapp.ui.auth.AuthViewModel
import com.ajrpachon.chatapp.ui.call.CallArgs
import com.ajrpachon.chatapp.ui.call.CallViewModel
import com.ajrpachon.chatapp.ui.call.IncomingCallViewModel
import com.ajrpachon.chatapp.ui.chat.gallery.ChatMediaGalleryViewModel
import com.ajrpachon.chatapp.ui.chat.ChatViewModel
import com.ajrpachon.chatapp.ui.chat.StickerPackViewModel
import com.ajrpachon.chatapp.ui.conversations.ConversationListViewModel
import com.ajrpachon.chatapp.ui.group.CreateGroupViewModel
import com.ajrpachon.chatapp.ui.group.GroupInfoViewModel
import com.ajrpachon.chatapp.ui.invitations.InvitationsViewModel
import com.ajrpachon.chatapp.ui.newchat.NewChatViewModel
import com.ajrpachon.chatapp.ui.profile.ProfileViewModel
import com.ajrpachon.chatapp.ui.userinfo.UserInfoViewModel
import com.ajrpachon.chatapp.ui.broadcast.BroadcastListViewModel
import com.ajrpachon.chatapp.ui.usagestats.UsageStatsViewModel
import com.ajrpachon.chatapp.ui.profile.SessionAuditViewModel
import com.ajrpachon.chatapp.ui.backup.BackupViewModel
import com.ajrpachon.chatapp.ui.pdf.PdfViewerViewModel
import com.ajrpachon.chatapp.ui.search.GlobalSearchViewModel
import com.ajrpachon.chatapp.ui.status.StatusViewModel
import com.ajrpachon.chatapp.service.PresenceManager
import com.ajrpachon.chatapp.utils.ClipboardProtection
import com.ajrpachon.chatapp.utils.LinkPreviewFetcher
import com.ajrpachon.chatapp.utils.OkHttpProvider
import com.ajrpachon.chatapp.utils.SessionGuard
import com.ajrpachon.chatapp.utils.TranslationManager
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import android.app.NotificationManager
import android.content.Context
import android.os.Environment
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val databaseModule = module {
    single { buildChatDatabase(androidContext()) }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().userDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().conversationDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().messageDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().invitationDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().groupMemberDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().reactionDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().pollDao() }
    single { com.ajrpachon.chatapp.data.local.PollRepository(get()) }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().stickerPackDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().messageReadReceiptDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().folderDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().broadcastListDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().chatEventDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().sessionDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().scheduledMessageDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().statusDao() }
}

val workManagerModule = module {
    single { androidx.work.WorkManager.getInstance(androidContext()) }
}

val networkModule = module {
    single {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            httpEngine = OkHttp.create { preconfigured = OkHttpProvider.client }
            install(Auth) {
                sessionManager = AndroidSessionManager(androidContext())
                scheme = "com.ajrpachon.chatapp"
                host = "auth-callback"
            }
            install(Postgrest)
            install(Realtime)
            install(Storage)
            install(Functions)
        }
    }
    single<OkHttpClient> { OkHttpProvider.client }
}

val viewModelModule = module {
    // BuildConfig values not injectable — kept as lambda
    viewModel { AuthViewModel(get(), get(), get(), get(), BuildConfig.GOOGLE_WEB_CLIENT_ID, get(), get()) }

    viewModelOf(::AppLockViewModel)
    viewModelOf(::ConversationListViewModel)
    viewModelOf(::InvitationsViewModel)
    viewModelOf(::NewChatViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::IncomingCallViewModel)
    viewModelOf(::CreateGroupViewModel)
    viewModelOf(::StickerPackViewModel)
    viewModelOf(::BroadcastListViewModel)
    viewModelOf(::UsageStatsViewModel)
    viewModelOf(::SessionAuditViewModel)
    viewModelOf(::BackupViewModel)
    viewModelOf(::GlobalSearchViewModel)
    viewModelOf(::StatusViewModel)
    // ChatViewModel: Repositories only, no DAOs
    viewModel { params ->
        ChatViewModel(
            args = params[0],
            application = androidApplication(),
            clipboardProtection = get(),
            sendMessageUseCase = get(),
            messageRepository = get(),
            callRepository = get(),
            userRepository = get(),
            getGroupMembersUseCase = get(),
            leaveGroupUseCase = get(),
            groupRepository = get(),
            reactionRepository = get(),
            conversationRepository = get(),
            scheduledMessageRepository = get(),
            typingRepository = get(),
            draftRepository = get(),
            translationManager = get(),
            audioTranscriber = get(),
            pollRepository = get(),
            contactRepository = get(),
            chatThemeRepository = get(),
            workManager = get(),
            incognitoRepository = get(),
            aiAssistantRepository = get(),
            wallpaperRepository = get(),
            networkMonitor = get<com.ajrpachon.chatapp.utils.NetworkMonitor>(),
            sendInvitationUseCase = get(),
        )
    }
    viewModelOf(::GroupInfoViewModel)
    viewModelOf(::UserInfoViewModel)
    viewModelOf(::ChatMediaGalleryViewModel)
    viewModelOf(::PdfViewerViewModel)

    // CallViewModel: BuildConfig.LIVEKIT_URL + runtime CallArgs — kept as lambda
    viewModel { params ->
        CallViewModel(
            args = params.get<CallArgs>(),
            application = androidApplication(),
            callRepository = get(),
            getCurrentUserUseCase = get(),
            sendMessageUseCase = get(),
            livekitUrl = BuildConfig.LIVEKIT_URL,
            recordingsDir = androidContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: androidContext().filesDir,
        )
    }
}

val utilsModule = module {
    single { ClipboardProtection(androidApplication()) }
    single { SessionGuard(androidContext()) }
    // PresenceManager.close() is called from ConversationListViewModel.onCleared(), its sole
    // start()-caller; see ConversationListViewModel for rationale.
    single { PresenceManager(get()) }
    single { LinkPreviewFetcher() }
    single { com.ajrpachon.chatapp.data.local.AppLockRepository(androidContext()) }
    single<com.ajrpachon.chatapp.domain.repository.IncognitoRepository> {
        com.ajrpachon.chatapp.data.local.IncognitoRepository(androidContext())
    }
    single { ThemeRepository(androidContext()) }
    single<com.ajrpachon.chatapp.domain.repository.DraftRepository> {
        com.ajrpachon.chatapp.data.local.DraftRepository(androidContext())
    }
    single { TranslationManager() }
    single { com.ajrpachon.chatapp.data.local.NotificationSoundRepository(androidContext()) }
    single { com.ajrpachon.chatapp.utils.AudioTranscriber(androidContext()) }
    single { androidx.credentials.CredentialManager.create(androidContext()) }
    single { com.ajrpachon.chatapp.data.local.ChatThemeRepository(androidContext()) }
    single { com.ajrpachon.chatapp.utils.NetworkMonitor(androidContext()) }
    single { com.ajrpachon.chatapp.utils.ContactSyncManager(androidContext().contentResolver) }
    single { com.ajrpachon.chatapp.utils.BackupManager(androidContext(), get()) }
    single<com.ajrpachon.chatapp.domain.repository.WallpaperRepository> {
        com.ajrpachon.chatapp.data.local.WallpaperRepository(androidContext())
    }
    single { androidContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
}

val aiModule = module {
    single<com.ajrpachon.chatapp.domain.repository.AiAssistantRepository> {
        com.ajrpachon.chatapp.data.repository.AiAssistantRepository(get())
    }
}

val appModules = listOf(
    databaseModule,
    networkModule,
    remoteModule,
    repositoryModule,
    useCaseModule,
    viewModelModule,
    utilsModule,
    workManagerModule,
    aiModule,
)
