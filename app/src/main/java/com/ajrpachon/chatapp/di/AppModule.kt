package com.ajrpachon.chatapp.di

import com.ajrpachon.chatapp.BuildConfig
import com.ajrpachon.chatapp.data.emoji.EmojiRepositoryImpl
import com.ajrpachon.chatapp.data.local.AppLockRepositoryImpl
import com.ajrpachon.chatapp.data.local.ChatDatabase
import com.ajrpachon.chatapp.data.local.ChatThemeRepositoryImpl
import com.ajrpachon.chatapp.data.local.DraftRepository as DraftRepositoryLocal
import com.ajrpachon.chatapp.data.local.IncognitoRepository as IncognitoRepositoryLocal
import com.ajrpachon.chatapp.data.local.NotificationSoundRepositoryImpl
import com.ajrpachon.chatapp.data.local.PollRepository as PollLocalDataSource
import com.ajrpachon.chatapp.data.local.ThemeRepositoryImpl
import com.ajrpachon.chatapp.data.local.WallpaperRepository as WallpaperRepositoryLocal
import com.ajrpachon.chatapp.data.local.buildChatDatabase
import com.ajrpachon.chatapp.data.repository.AiAssistantRepository as AiAssistantRepositoryImpl
import com.ajrpachon.chatapp.data.repository.FirebaseAnalyticsTracker
import com.ajrpachon.chatapp.data.repository.FirebaseCrashReporter
import com.ajrpachon.chatapp.data.session.AndroidSessionManager
import com.ajrpachon.chatapp.domain.repository.AiAssistantRepository
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.AppLockRepository
import com.ajrpachon.chatapp.domain.repository.ChatThemeRepository
import com.ajrpachon.chatapp.domain.repository.CrashReporter
import com.ajrpachon.chatapp.domain.repository.DraftRepository
import com.ajrpachon.chatapp.domain.repository.EmojiRepository
import com.ajrpachon.chatapp.domain.repository.IncognitoRepository
import com.ajrpachon.chatapp.domain.repository.NotificationSoundRepository
import com.ajrpachon.chatapp.domain.repository.ThemeRepository
import com.ajrpachon.chatapp.domain.repository.WallpaperRepository
import com.ajrpachon.chatapp.ui.applock.AppLockViewModel
import com.ajrpachon.chatapp.ui.auth.AuthViewModel
import com.ajrpachon.chatapp.ui.call.CallArgs
import com.ajrpachon.chatapp.ui.call.CallViewModel
import com.ajrpachon.chatapp.ui.call.IncomingCallViewModel
import com.ajrpachon.chatapp.ui.chat.gallery.ChatMediaGalleryViewModel
import com.ajrpachon.chatapp.ui.chat.ChatViewModel
import com.ajrpachon.chatapp.ui.chat.StickerPackViewModel
import com.ajrpachon.chatapp.ui.chat.GifPickerViewModel
import com.ajrpachon.chatapp.ui.components.EmojiPickerViewModel
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
import com.ajrpachon.chatapp.utils.AudioTranscriber
import com.ajrpachon.chatapp.utils.ClipboardProtection
import com.ajrpachon.chatapp.utils.ContactSyncManager
import com.ajrpachon.chatapp.utils.LinkPreviewFetcher
import com.ajrpachon.chatapp.utils.NetworkMonitor
import com.ajrpachon.chatapp.utils.OkHttpProvider
import com.ajrpachon.chatapp.utils.SessionGuard
import com.ajrpachon.chatapp.utils.TranslationManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import android.app.NotificationManager
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.work.WorkManager
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val databaseModule = module {
    single { buildChatDatabase(androidContext()) }
    single { get<ChatDatabase>().userDao() }
    single { get<ChatDatabase>().conversationDao() }
    single { get<ChatDatabase>().messageDao() }
    single { get<ChatDatabase>().invitationDao() }
    single { get<ChatDatabase>().groupMemberDao() }
    single { get<ChatDatabase>().reactionDao() }
    single { get<ChatDatabase>().pollDao() }
    single { PollLocalDataSource(get()) }
    single { get<ChatDatabase>().stickerPackDao() }
    single { get<ChatDatabase>().messageReadReceiptDao() }
    single { get<ChatDatabase>().folderDao() }
    single { get<ChatDatabase>().broadcastListDao() }
    single { get<ChatDatabase>().chatEventDao() }
    single { get<ChatDatabase>().sessionDao() }
    single { get<ChatDatabase>().scheduledMessageDao() }
    single { get<ChatDatabase>().statusDao() }
}

val workManagerModule = module {
    single { WorkManager.getInstance(androidContext()) }
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
    viewModelOf(::EmojiPickerViewModel)
    viewModelOf(::GifPickerViewModel)
    // ChatViewModel: Repositories only, no DAOs
    viewModel { params ->
        ChatViewModel(
            args = params[0],
            application = androidApplication(),
            clipboardProtection = get(),
            sendMessageUseCase = get(),
            messageRepository = get(),
            pendingMessageRepository = get(),
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
            networkMonitor = get<NetworkMonitor>(),
            sendInvitationUseCase = get(),
            exportConversationUseCase = get(),
            linkPreviewFetcher = get(),
            getUriMetadataUseCase = get(),
            readUriAsBytesUseCase = get(),
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
            analyticsTracker = get(),
            livekitUrl = BuildConfig.LIVEKIT_URL,
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
    single<AppLockRepository> { AppLockRepositoryImpl(androidContext(), get()) }
    single<IncognitoRepository> { IncognitoRepositoryLocal(androidContext(), get()) }
    single<ThemeRepository> { ThemeRepositoryImpl(androidContext(), get()) }
    single<DraftRepository> { DraftRepositoryLocal(androidContext()) }
    single { TranslationManager(get()) }
    single<NotificationSoundRepository> { NotificationSoundRepositoryImpl(androidContext(), get()) }
    single { AudioTranscriber(androidContext()) }
    single { CredentialManager.create(androidContext()) }
    single<ChatThemeRepository> { ChatThemeRepositoryImpl(androidContext(), get()) }
    single { NetworkMonitor(androidContext()) }
    single { ContactSyncManager(androidContext().contentResolver) }
    single<WallpaperRepository> { WallpaperRepositoryLocal(androidContext(), get()) }
    single { androidContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    single<EmojiRepository> { EmojiRepositoryImpl(androidContext()) }
}

val aiModule = module {
    single<AiAssistantRepository> { AiAssistantRepositoryImpl(get(), get()) }
}

val analyticsModule = module {
    single { FirebaseAnalytics.getInstance(androidContext()) }
    single { FirebaseCrashlytics.getInstance() }
    single<AnalyticsTracker> { FirebaseAnalyticsTracker(get()) }
    single<CrashReporter> { FirebaseCrashReporter(get()) }
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
    analyticsModule,
)
